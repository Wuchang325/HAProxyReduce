package top.zient.haproxyreduce.common

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.concurrent.atomic.AtomicReference
import top.zient.haproxyreduce.common.LogMessages

/**
 * 代理白名单管理器（基于 YAML 配置）
 */
class ProxyWhitelist private constructor(
    private val entries: List<CIDR>,
    private val mode: Config.WhitelistMode
) {
    /**
     * 检查地址是否应该解析 HAProxy 协议
     * @return true = 解析协议, false = 跳过解析（使用原始IP）
     */
    fun shouldParseHAProxy(address: SocketAddress): Boolean {
        return when (mode) {
            Config.WhitelistMode.DISABLED -> true
            Config.WhitelistMode.EMPTY_ALLOW_ALL -> true
            Config.WhitelistMode.EMPTY_DENY_ALL -> {
                // 空列表时拒绝所有
                if (entries.isEmpty()) return false
                checkAddress(address)
            }
        }
    }

    /**
     * 检查地址是否在白名单中
     */
    fun checkAddress(address: SocketAddress): Boolean {
        return when (mode) {
            Config.WhitelistMode.DISABLED -> true
            Config.WhitelistMode.EMPTY_ALLOW_ALL -> {
                if (entries.isEmpty()) return true
                matches(address)
            }
            Config.WhitelistMode.EMPTY_DENY_ALL -> {
                if (entries.isEmpty()) return false
                matches(address)
            }
        }
    }

    private fun matches(address: SocketAddress): Boolean {
        return (address as? InetSocketAddress)?.address?.let { addr ->
            entries.any { it.contains(addr) }
        } ?: false
    }

    companion object {
        @Volatile
        var whitelist: ProxyWhitelist? = null
            private set

        private val lastWarningRef = AtomicReference<InetAddress>()

        /**
         * 从 Config 加载白名单
         */
        fun load(config: Config): ProxyWhitelist {
            val entries = config.whitelist.ips.flatMap { cidrStr ->
                try {
                    CIDR.parse(cidrStr)
                } catch (e: Exception) {
                    emptyList()
                }
            }

            return ProxyWhitelist(entries, config.whitelist.mode).also {
                whitelist = it
            }
        }

        /**
         * 检查是否应该解析 HAProxy 协议（静态便捷方法）
         */
        fun shouldParse(address: SocketAddress?): Boolean {
            address ?: return false
            return whitelist?.shouldParseHAProxy(address) ?: false
        }

        /**
         * 检查地址是否在白名单中（静态便捷方法）
         */
        fun check(address: SocketAddress?): Boolean {
            address ?: return false
            return whitelist?.checkAddress(address) ?: false
        }

        /**
         * 获取警告信息（用于日志记录，遵循 warnOnce 设置）
         */
        fun getWarningFor(address: SocketAddress?, config: Config): String? {
            address ?: return null
            if (!config.logging.warnOnce) {
                val inetAddr = (address as? InetSocketAddress)?.address
                return inetAddr?.let { LogMessages.PROXY_CONNECTION_NOT_WHITELISTED.format(it.hostAddress) }
            }

            val inetAddr = (address as? InetSocketAddress)?.address ?: return null
            val previous = lastWarningRef.getAndSet(inetAddr)

            return if (inetAddr != previous) {
                LogMessages.PROXY_CONNECTION_NOT_WHITELISTED.format(inetAddr.hostAddress)
            } else null
        }

        /**
         * 获取当前模式描述
         */
        fun getModeDescription(): String {
            return whitelist?.mode?.name ?: "未加载"
        }
    }
}
