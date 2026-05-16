package top.zient.haproxyreduce.paper

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.reflect.FuzzyReflection
import io.netty.channel.*
import org.bstats.bukkit.Metrics
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerLoadEvent
import org.bukkit.plugin.java.JavaPlugin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import top.zient.haproxyreduce.api.HAProxyReduceAPIImpl
import top.zient.haproxyreduce.common.Config
import top.zient.haproxyreduce.common.ConfigHotReloader
import top.zient.haproxyreduce.common.LogMessages
import top.zient.haproxyreduce.common.MetricsId
import top.zient.haproxyreduce.common.ProxyAccessControl
import java.lang.reflect.Field
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.nio.file.Path

class PaperMain : JavaPlugin(), Listener {
    private val logger: Logger = LoggerFactory.getLogger("HAProxyReduce")
    private var dataDir: Path? = null
    private var serverType = "Unknown"
    private var handlerField: Field? = null
    private var injectorInitializer: ChannelInboundHandler? = null
    private var originalHandler: ChannelInboundHandler? = null
    private lateinit var config: Config
    private lateinit var hotReloader: ConfigHotReloader

    override fun onLoad() {
        dataDir = dataFolder.toPath()
        serverType = detectServerType()
    }

    override fun onEnable() {
        // 仅支持 Paper 和 Folia
        if (serverType != "Paper" && serverType != "Folia") {
        logger.error(LogMessages.UNSUPPORTED_SERVER_TYPE, serverType)
            server.pluginManager.disablePlugin(this)
            return
        }

        logger.info(LogMessages.SERVER_ENVIRONMENT_DETECTED, serverType)

        if (!isProxyProtocolEnabled()) {
            logger.error(LogMessages.PROXY_PROTOCOL_NOT_ENABLED)
            server.pluginManager.disablePlugin(this)
            return
        }

        server.pluginManager.registerEvents(this, this)

        // 检查 ProtocolLib
        if (server.pluginManager.getPlugin("ProtocolLib") == null) {
            logger.error(LogMessages.PROTOCOL_LIB_NOT_FOUND)
            server.pluginManager.disablePlugin(this)
            return
        }

        // 加载配置
        try {
            config = Config.load(dataDir!!.resolve("config.yml"))
            logger.info(LogMessages.CONFIG_LOADED, config.whitelist.mode)
        } catch (e: Exception) {
            logger.error(LogMessages.CONFIG_LOAD_FAILED, e)
            server.pluginManager.disablePlugin(this)
            return
        }

        // 初始化API
        HAProxyReduceAPIImpl.create(dataDir!!.resolve("config.yml"), config)

        // 初始化访问控制
        ProxyAccessControl.load(config)

        // 显示配置信息
        when (config.whitelist.mode) {
            Config.WhitelistMode.EMPTY_DENY_ALL -> {
                if (config.whitelist.ips.isEmpty()) {
                    logger.warn(LogMessages.WHITELIST_MODE_EMPTY_DENY_ALL_WARNING)
                } else {
                    logger.info(LogMessages.WHITELIST_RULES_LOADED, config.whitelist.ips.size)
                }
            }
            Config.WhitelistMode.EMPTY_ALLOW_ALL -> {
                logger.info(LogMessages.WHITELIST_MODE_EMPTY_ALLOW_ALL)
                if (config.whitelist.ips.isNotEmpty()) {
                    logger.info(LogMessages.WHITELIST_RULES_LOADED, config.whitelist.ips.size)
                }
            }
            Config.WhitelistMode.DISABLED -> {
                logger.info(LogMessages.WHITELIST_MODE_DISABLED)
            }
        }

        if (config.blacklist.enabled) {
            logger.info(LogMessages.BLACKLIST_ENABLED, config.blacklist.ips.size)
        } else {
            logger.info(LogMessages.BLACKLIST_DISABLED)
        }

        initMetrics()

        // 启动热重载
        hotReloader = ConfigHotReloader(dataDir!!.resolve("config.yml"), logger) { newConfig ->
            config = newConfig
            ProxyAccessControl.load(config)
            logger.info(LogMessages.CONFIG_RELOAD_COMPLETE + ": ${ProxyAccessControl.getModeDescription()}")
        }
        hotReloader.start(config)

        // 注册命令
        val cmdExecutor = PaperCommands(this)
        this.getCommand("haproxyreload")?.setExecutor(cmdExecutor)
        this.getCommand("haproxystatus")?.setExecutor(cmdExecutor)

        if (!inject()) {
            logger.error(LogMessages.CHANNEL_INJECTION_FAILED)
            server.pluginManager.disablePlugin(this)
            return
        }

        logger.info(LogMessages.PLUGIN_ENABLED + " ($serverType 环境)")
    }

    private fun isProxyProtocolEnabled(): Boolean {
        return try {
            // Paper 使用 paper-global.yml 中的配置
            // 通过反射访问 PaperConfiguration 或类似配置类
            val paperConfigClass = Class.forName("io.papermc.paper.configuration.GlobalConfiguration")
            val getMethod = paperConfigClass.getMethod("get")
            val globalConfig = getMethod.invoke(null)

            // 访问 proxies.proxy-protocol
            val proxyClass = Class.forName("io.papermc.paper.configuration.GlobalConfiguration\$Proxies")
            val proxiesField = paperConfigClass.getDeclaredField("proxies")
            proxiesField.isAccessible = true
            val proxies = proxiesField.get(globalConfig)

            val proxyProtocolField = proxyClass.getDeclaredField("proxyProtocol")
            proxyProtocolField.isAccessible = true
            proxyProtocolField.getBoolean(proxies)
        } catch (e: Exception) {
            logger.warn(LogMessages.PROXY_PROTOCOL_DETECTION_FAILED, e)
            // 备用方法：检查系统属性或配置文件
            checkProxyProtocolInConfig()
        }
    }
    private fun checkProxyProtocolInConfig(): Boolean {
        return try {
            val configFile = dataDir!!.parent.parent.resolve("config/paper-global.yml").toFile()
            if (!configFile.exists()) {
                // 尝试 Folia 配置
                val foliaConfig = dataDir!!.parent.parent.resolve("config/folia-global.yml").toFile()
                if (foliaConfig.exists()) {
                    foliaConfig.readText().contains("proxy-protocol: true")
                } else {
                    false
                }
            } else {
                configFile.readText().contains("proxy-protocol: true")
            }
        } catch (e: Exception) {
            logger.warn(LogMessages.PROXY_PROTOCOL_READ_FAILED, e)
            false
        }
    }


    @EventHandler
    fun onServerLoaded(event: ServerLoadEvent?) {
        logger.info(LogMessages.SERVER_STARTUP_COMPLETE)
    }

    private fun initMetrics() {
        Metrics(this, 31334).addCustomChart(MetricsId.createWhitelistCountChart())
    }

    private fun detectServerType(): String {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer")
            return "Folia"
        } catch (e: ClassNotFoundException) {
            try {
                Class.forName("com.destroystokyo.paper.PaperConfig")
                return "Paper"
            } catch (ex: ClassNotFoundException) {
                return "Unknown"
            }
        }
    }

    /**
     * 基于ProtocolLib内部机制注入通道处理器
     */
    private fun inject(): Boolean {
        try {
            uninject()

            val networkManagerInjectorClass =
                Class.forName("com.comphenix.protocol.injector.netty.manager.NetworkManagerInjector")
            val injectionChannelInitializerClass =
                Class.forName("com.comphenix.protocol.injector.netty.manager.InjectionChannelInitializer")

            val pm = ProtocolLibrary.getProtocolManager()
            val injectorField = FuzzyReflection.fromObject(pm, true)
                .getFieldByType("networkManagerInjector", networkManagerInjectorClass)
            injectorField.setAccessible(true)
            val networkManagerInjector = injectorField.get(pm)

            val injectorInitializerField = FuzzyReflection.fromClass(networkManagerInjectorClass, true)
                .getFieldByType("pipelineInjectorHandler", injectionChannelInitializerClass)
            injectorInitializerField.setAccessible(true)
            this.injectorInitializer = injectorInitializerField.get(networkManagerInjector) as ChannelInboundHandler?

            this.handlerField = FuzzyReflection.fromClass(injectionChannelInitializerClass, true)
                .getFieldByType("handler", ChannelInboundHandler::class.java)
            handlerField!!.setAccessible(true)
            this.originalHandler = handlerField!!.get(injectorInitializer) as ChannelInboundHandler?

            // 创建代理处理器拦截channelActive事件
            val proxyHandler = Proxy.newProxyInstance(
                javaClass.getClassLoader(),
                arrayOf<Class<*>>(ChannelInboundHandler::class.java),
                object : InvocationHandler {
                    @Throws(Throwable::class)
                    override fun invoke(proxy: Any?, method: Method, args: Array<Any?>): Any? {
                        if ("channelActive" == method.name && args.isNotEmpty() && args[0] is ChannelHandlerContext) {
                            val ctx = args[0] as ChannelHandlerContext
                            // 恢复原始处理器并注入我们的检测器
                            ctx.pipeline().remove(proxy as ChannelHandler?)
                                .addFirst("protocol_lib_inbound_inject", originalHandler)

                            val result = method.invoke(originalHandler, *args)
                            doInject(ctx.channel())
                            return result
                        }
                        return method.invoke(originalHandler, *args)
                    }
                }
            ) as ChannelInboundHandler

            val field = handlerField ?: run {
                logger.error(LogMessages.HANDLER_FIELD_NULL)
                return false
            }
            field.set(injectorInitializer, proxyHandler)
            logger.debug(LogMessages.CHANNEL_INJECTION_SUCCESS)
            return true
        } catch (e: Exception) {
            logger.error("注入处理器失败", e)
            return false
        }
    }

    /**
     * 实际执行通道注入逻辑
     */
    private fun doInject(channel: Channel) {
        if (channel.eventLoop().inEventLoop()) {
            try {
                val pipeline = channel.pipeline()
                if (!channel.isOpen() || pipeline.get("haproxy-detector") != null) {
                    return
                }

                // 移除已存在的HAProxy解码器（如果有）
                if (pipeline.get("haproxy-decoder") != null) {
                    pipeline.remove("haproxy-decoder")
                }

                // 添加我们的检测器
                val detector = PaperDetectorHandler(logger, config)
                try {
                    pipeline.addAfter("timeout", "haproxy-detector", detector)
                } catch (e: NoSuchElementException) {
                    // 兼容不同的通道流水线结构
                    pipeline.addFirst("haproxy-detector", detector)
                }
                logger.debug("成功向通道添加HAProxy检测器")
            } catch (t: Throwable) {
                logger.warn("注入检测器时发生异常", t)
            }
        } else {
            // 确保在正确的事件循环中执行
            channel.eventLoop().execute(Runnable { doInject(channel) })
        }
    }

    /**
     * 解除注入，恢复原始状态
     */
    private fun uninject() {
        if (handlerField != null && injectorInitializer != null && originalHandler != null) {
            try {
                handlerField!!.set(injectorInitializer, originalHandler)
            } catch (e: Exception) {
                logger.warn("解除注入失败", e)
            }
            injectorInitializer = null
            originalHandler = null
        }
    }

    override fun onDisable() {
        // 停止热重载
        if (::hotReloader.isInitialized) {
            hotReloader.stop()
        }

        // 清理API实例
        HAProxyReduceAPIImpl.destroy()

        uninject()
        logger.info(LogMessages.PLUGIN_DISABLED)
    }

    companion object {
        /**
         * 从通道流水线获取NetworkManager（兼容不同服务端）
         */
        fun getNetworkManager(pipeline: ChannelPipeline): ChannelHandler? {
            try {
                // Paper/Folia的NetworkManager通常在pipeline中
                return pipeline.get("packet_handler")
            } catch (e: NoSuchElementException) {
                // 尝试其他可能的名称
                for (name in arrayOf<String>("network_manager", "packet_handler")) {
                    try {
                        return pipeline.get(name)
                    } catch (ignored: NoSuchElementException) {
                    }
                }
                throw e
            }
        }
    }
}
