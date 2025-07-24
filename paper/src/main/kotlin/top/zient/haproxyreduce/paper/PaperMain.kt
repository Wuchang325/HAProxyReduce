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
import top.zient.haproxyreduce.common.MetricsId.createWhitelistCountChart
import top.zient.haproxyreduce.common.ProxyWhitelist
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

    override fun onLoad() {
        dataDir = dataFolder.toPath()
        serverType = detectServerType()
    }

    override fun onEnable() {
        // 仅支持 Paper 和 Folia
        if (serverType !== "Paper" && serverType !== "Folia") {
            logger.error("此插件仅支持 Paper 或 Folia 服务端，当前服务端类型: " + serverType)
            getServer().getPluginManager().disablePlugin(this)
            return
        }

        logger.info("检测到服务端环境: " + serverType)
        getServer().getPluginManager().registerEvents(this, this)

        // 检查 ProtocolLib
        if (getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
            logger.error("未找到 ProtocolLib 插件，请安装 ProtocolLib 5.1.0 或更高版本")
            getServer().getPluginManager().disablePlugin(this)
            return
        }

        // 加载白名单
        val whitelist: ProxyWhitelist
        try {
            whitelist = ProxyWhitelist.loadOrDefault(dataDir!!.resolve("whitelist.conf"))
        } catch (e: Exception) {
            logger.error("加载白名单配置失败，插件将禁用", e)
            getServer().getPluginManager().disablePlugin(this)
            return
        }
        ProxyWhitelist.whitelist = whitelist

        if (whitelist.size === 0) {
            logger.warn("代理白名单为空，将禁止所有代理连接！")
        }

        initMetrics()

        if (!inject()) {
            logger.error("通道处理器注入失败，插件无法正常工作")
            getServer().getPluginManager().disablePlugin(this)
            return
        }

        logger.info("HAProxyReduce 已启用 (" + serverType + " 环境)")
    }

    @EventHandler
    fun onServerLoaded(event: ServerLoadEvent?) {
        logger.info("服务器启动完成，HAProxyReduce 运行中")
    }

    private fun initMetrics() {
        Metrics(this, 14444).addCustomChart(createWhitelistCountChart())
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
            uninject() // 先尝试解除之前的注入

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
                        if ("channelActive" == method.getName() && args.size > 0 && args[0] is ChannelHandlerContext) {
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

            handlerField!!.set(injectorInitializer, proxyHandler)
            logger.debug("通道处理器注入成功")
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
                val detector = PaperDetectorHandler(logger)
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
        uninject()
        logger.info("HAProxyReduce 已禁用")
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