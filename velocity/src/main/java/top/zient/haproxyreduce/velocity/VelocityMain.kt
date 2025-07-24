package top.zient.haproxyreduce.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.ProxyServer
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import org.bstats.velocity.Metrics.Factory
import org.slf4j.Logger
import top.zient.haproxyreduce.common.MetricsId
import top.zient.haproxyreduce.common.ProxyWhitelist
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field
import java.nio.file.Path

@Plugin(
    id = "haproxy-reduce",
    name = "HAProxyReduce",
    version = "3.2.0",
    description = "同时支持代理和直连连接",
    authors = ["Wuchang325"]
)
class VelocityMain @Inject constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path,
    private val metricsFactory: Factory
) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        try {
            if (!isProxyEnabled()) {
                logger.error("!!! ==============================")
                logger.error("!!! 代理协议未启用，插件可能无法正常工作！")
                logger.error("!!! ==============================")
            }

            val whitelist = ProxyWhitelist.loadOrDefault(dataDirectory.resolve("whitelist.conf"))
            if (whitelist.size == 0) {
                logger.warn("代理白名单为空，将禁止所有代理连接！")
            }
            ProxyWhitelist.whitelist = whitelist

            inject()

            val metrics = metricsFactory.make(this, 14442)
            metrics.addCustomChart(MetricsId.createWhitelistCountChart())
        } catch (e: Exception) {
            logger.error("初始化失败", e)
        }
    }

    private fun isProxyEnabled(): Boolean {
        return try {
            val config = server.configuration
            val method = config.javaClass.getMethod("isProxyProtocol")
            method.invoke(config) as Boolean
        } catch (e: Exception) {
            logger.warn("检测代理协议状态失败", e)
            false
        }
    }

    private fun inject() {
        try {
            // 获取连接管理器
            val cmType = Class.forName("com.velocitypowered.proxy.network.ConnectionManager")
            val cmField = findFirstFieldByType(server.javaClass, cmType)
            cmField.isAccessible = true
            val connectionManager = cmField.get(server)

            // 获取通道初始化器持有者
            val holder = cmType.getMethod("getServerChannelInitializer").invoke(connectionManager)
            val holderType = holder.javaClass

            // 获取原始通道初始化器
            val originalInitializer = holderType.getMethod("get").invoke(holder) as ChannelInitializer<Channel>

            // 创建新的通道初始化器
            val newInitializer = DetectorInitializer(logger, originalInitializer)

            // 替换通道初始化器
            val setMethod = holderType.getMethod("set", ChannelInitializer::class.java)
            val setHandle = MethodHandles.lookup().unreflect(setMethod)

            logger.info("正在替换通道初始化器，以下警告可安全忽略")
            setHandle.invoke(holder, newInitializer)
        } catch (e: Throwable) {
            logger.error("注入失败", e)
        }
    }

    private fun findFirstFieldByType(clazz: Class<*>, type: Class<*>): Field {
        return clazz.declaredFields.firstOrNull { it.type == type }
            ?: throw NoSuchElementException("在 ${clazz.name} 中找不到类型为 ${type.name} 的字段")
    }
}