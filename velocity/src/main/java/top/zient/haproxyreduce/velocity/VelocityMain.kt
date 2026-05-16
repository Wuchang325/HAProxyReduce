package top.zient.haproxyreduce.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import org.bstats.velocity.Metrics.Factory
import org.slf4j.Logger
import top.zient.haproxyreduce.api.HAProxyReduceAPIImpl
import top.zient.haproxyreduce.common.Config
import top.zient.haproxyreduce.common.ConfigHotReloader
import top.zient.haproxyreduce.common.LogMessages
import top.zient.haproxyreduce.common.MetricsId
import top.zient.haproxyreduce.common.ProxyAccessControl
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Plugin(
    id = "haproxyreduce",
    name = "HAProxyReduce",
    version = "\${project.version}",
    description = "同时支持代理和直连连接",
    authors = ["Wuchang325"]
)
class VelocityMain @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path,
    private val metricsFactory: Factory,
    private val pluginContainer: PluginContainer
) {
    private lateinit var config: Config
    private lateinit var hotReloader: ConfigHotReloader

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        try {
            // 1. 加载 YAML 配置
            config = Config.load(dataDirectory.resolve("config.yml"))
            logger.info(LogMessages.CONFIG_LOADED, config.whitelist.mode)

            if (!isProxyEnabled()) {
                logger.error(LogMessages.PROXY_PROTOCOL_NOT_ENABLED_ERROR)
                throw IllegalStateException("HAProxy 支持未启用")
            }

            // 2. 初始化API
            HAProxyReduceAPIImpl.create(dataDirectory.resolve("config.yml"), config)

            // 3. 初始化访问控制
            ProxyAccessControl.load(config)

            // 4. 显示配置信息
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

            // 5. 注入 Netty 处理器
            inject()

            // 6. 注册事件监听器
            server.eventManager.register(this, LoginListener(logger))
            logger.info(LogMessages.LOGIN_LISTENER_REGISTERED)

            // 7. 启动热重载
            hotReloader = ConfigHotReloader(dataDirectory.resolve("config.yml"), logger) { newConfig ->
                config = newConfig
                ProxyAccessControl.load(config)
                logger.info(LogMessages.CONFIG_RELOAD_COMPLETE + ": ${ProxyAccessControl.getModeDescription()}")
            }
            hotReloader.start(config)

            // 8. 启动定时清理任务（使用配置的超时时间）
            server.scheduler.buildTask(this, Runnable {
                ConnectionTracker.cleanupOldConnections(config.connectionTracker.timeout * 1000L)
            }).repeat(config.connectionTracker.cleanupInterval.toLong(), TimeUnit.MINUTES).schedule()

            // 9. 启动 bStats
            val metrics = metricsFactory.make(this, 31347)
            metrics.addCustomChart(MetricsId.createWhitelistCountChart())

            val version = pluginContainer.description.version.orElse("未知")
            logger.info(LogMessages.PLUGIN_ENABLED + " v$version")

            // 注册命令
            try {
                val metaReload = server.commandManager.metaBuilder("haproxyreload").build()
                server.commandManager.register(metaReload, ReloadCommand())

                val metaStatus = server.commandManager.metaBuilder("haproxystatus").build()
                server.commandManager.register(metaStatus, StatusCommand())
            } catch (ignored: Exception) {
                logger.warn("命令注册失败，可能与当前 Velocity 版本或权限系统不兼容")
            }

        } catch (e: Exception) {
            logger.error(LogMessages.INITIALIZATION_FAILED, e)
        }
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        // 停止热重载
        if (::hotReloader.isInitialized) {
            hotReloader.stop()
        }

        // 清理API实例
        HAProxyReduceAPIImpl.destroy()

        logger.info(LogMessages.PLUGIN_DISABLED)
    }

    private fun isProxyEnabled(): Boolean {
        return try {
            val config = server.configuration
            val method = config.javaClass.getMethod("isProxyProtocol")
            method.invoke(config) as Boolean
        } catch (e: Exception) {
            logger.warn(LogMessages.PROXY_PROTOCOL_DETECTION_FAILED, e)
            false
        }
    }
    @Suppress("UNCHECKED_CAST")
    private fun inject() {
        try {
            val cmType = Class.forName("com.velocitypowered.proxy.network.ConnectionManager")
            val cmField = findFirstFieldByType(server.javaClass, cmType)
            cmField.isAccessible = true
            val connectionManager = cmField.get(server)

            val holder = cmType.getMethod("getServerChannelInitializer").invoke(connectionManager)
            val holderType = holder.javaClass

            val rawInitializer = holderType.getMethod("get").invoke(holder)
            val originalInitializer = rawInitializer as ChannelInitializer<io.netty.channel.Channel>

            // 传递 config
            val newInitializer = DetectorInitializer(logger, config, originalInitializer)

            val setMethod = holderType.getMethod("set", ChannelInitializer::class.java)
            val setHandle = MethodHandles.lookup().unreflect(setMethod)

            setHandle.invoke(holder, newInitializer)
            logger.info(LogMessages.CHANNEL_INITIALIZER_INJECTED_SUCCESS)
        } catch (e: Throwable) {
            logger.error(LogMessages.INJECTION_FAILED, e)
            throw e
        }
    }

    private fun findFirstFieldByType(clazz: Class<*>, type: Class<*>): Field {
        return clazz.declaredFields.firstOrNull { it.type == type }
            ?: throw NoSuchElementException("在 ${clazz.name} 中找不到类型为 ${type.name} 的字段")
    }
}
