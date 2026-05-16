package top.zient.haproxyreduce.common

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.concurrent.atomic.AtomicReference

/**
 * 代理访问控制管理器（支持黑白名单）
 */
class ProxyAccessControl private constructor(
    private val whitelistEntries: List<CIDR>,
    private val blacklistEntries: List<CIDR>,
    private val whitelistMode: Config.WhitelistMode,
    private val blacklistEnabled: Boolean
) {
    /**
     * 检查地址是否应该解析 HAProxy 协议
     * @return true = 解析协议, false = 跳过解析（使用原始IP）
     */
    fun shouldParseHAProxy(address: SocketAddress): Boolean {
        // 首先检查黑名单（优先级最高）
        if (blacklistEnabled && isBlacklisted(address)) {
            return false
        }

        // 然后检查白名单
        return when (whitelistMode) {
            Config.WhitelistMode.DISABLED -> true
            Config.WhitelistMode.EMPTY_ALLOW_ALL -> true
            Config.WhitelistMode.EMPTY_DENY_ALL -> {
                // 空列表时拒绝所有
                if (whitelistEntries.isEmpty()) return false
                matchesWhitelist(address)
            }
        }
    }

    /**
     * 检查地址是否被允许（通过白名单且不在黑名单）
     */
    fun isAllowed(address: SocketAddress): Boolean {
        // 首先检查黑名单
        if (blacklistEnabled && isBlacklisted(address)) {
            return false
        }

        // 然后检查白名单
        return when (whitelistMode) {
            Config.WhitelistMode.DISABLED -> true
            Config.WhitelistMode.EMPTY_ALLOW_ALL -> {
                if (whitelistEntries.isEmpty()) return true
                matchesWhitelist(address)
            }
            Config.WhitelistMode.EMPTY_DENY_ALL -> {
                if (whitelistEntries.isEmpty()) return false
                matchesWhitelist(address)
            }
        }
    }

    /**
     * 检查地址是否在黑名单中
     */
    fun isBlacklisted(address: SocketAddress): Boolean {
        if (!blacklistEnabled) return false
        return matches(address, blacklistEntries)
    }

    /**
     * 检查地址是否在白名单中
     */
    fun isWhitelisted(address: SocketAddress): Boolean {
        return when (whitelistMode) {
            Config.WhitelistMode.DISABLED -> true
            Config.WhitelistMode.EMPTY_ALLOW_ALL -> {
                if (whitelistEntries.isEmpty()) return true
                matchesWhitelist(address)
            }
            Config.WhitelistMode.EMPTY_DENY_ALL -> {
                if (whitelistEntries.isEmpty()) return false
                matchesWhitelist(address)
            }
        }
    }

    private fun matchesWhitelist(address: SocketAddress): Boolean = matches(address, whitelistEntries)

    private fun matches(address: SocketAddress, entries: List<CIDR>): Boolean {
        return (address as? InetSocketAddress)?.address?.let { addr ->
            entries.any { it.contains(addr) }
        } ?: false
    }

    companion object {
        @Volatile
        var accessControl: ProxyAccessControl? = null
            private set

        private val lastWarningRef = AtomicReference<InetAddress>()

        /**
         * 从 Config 加载访问控制规则
         */
        fun load(config: Config): ProxyAccessControl {
            val whitelistEntries = config.whitelist.ips.flatMap { cidrStr ->
                try {
                    CIDR.parse(cidrStr)
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val blacklistEntries = if (config.blacklist.enabled) {
                config.blacklist.ips.flatMap { cidrStr ->
                    try {
                        CIDR.parse(cidrStr)
                    } catch (e: Exception) {
                        emptyList()
                    }
                }
            } else {
                emptyList()
            }

            return ProxyAccessControl(
                whitelistEntries = whitelistEntries,
                blacklistEntries = blacklistEntries,
                whitelistMode = config.whitelist.mode,
                blacklistEnabled = config.blacklist.enabled
            ).also {
                accessControl = it
            }
        }

        /**
         * 检查是否应该解析 HAProxy 协议（静态便捷方法）
         */
        fun shouldParse(address: SocketAddress?): Boolean {
            address ?: return false
            return accessControl?.shouldParseHAProxy(address) ?: false
        }

        /**
         * 检查地址是否被允许（静态便捷方法）
         */
        fun isAllowed(address: SocketAddress?): Boolean {
            address ?: return false
            return accessControl?.isAllowed(address) ?: false
        }

        /**
         * 检查地址是否在黑名单中（静态便捷方法）
         */
        fun isBlacklisted(address: SocketAddress?): Boolean {
            address ?: return false
            return accessControl?.isBlacklisted(address) ?: false
        }

        /**
         * 检查地址是否在白名单中（静态便捷方法）
         */
        fun isWhitelisted(address: SocketAddress?): Boolean {
            address ?: return false
            return accessControl?.isWhitelisted(address) ?: false
        }

        /**
         * 获取警告信息（用于日志记录，遵循 warnOnce 设置）
         */
        fun getWarningFor(address: SocketAddress?, config: Config): String? {
            address ?: return null
            if (!config.logging.warnOnce) {
                val inetAddr = (address as? InetSocketAddress)?.address
                return when {
                    isBlacklisted(address) -> "检测到来自 ${inetAddr?.hostAddress} 的代理连接，但该地址在黑名单中（使用原始IP）"
                    !isWhitelisted(address) -> "检测到来自 ${inetAddr?.hostAddress} 的代理连接，但该地址不在白名单中（使用原始IP）"
                    else -> null
                }
            }

            val inetAddr = (address as? InetSocketAddress)?.address ?: return null
            val previous = lastWarningRef.getAndSet(inetAddr)

            return if (inetAddr != previous) {
                when {
                    isBlacklisted(address) -> "检测到来自 ${inetAddr.hostAddress} 的代理连接，但该地址在黑名单中（使用原始IP）"
                    !isWhitelisted(address) -> "检测到来自 ${inetAddr.hostAddress} 的代理连接，但该地址不在白名单中（使用原始IP）"
                    else -> null
                }
            } else null
        }

        /**
         * 获取当前模式描述
         */
        fun getModeDescription(): String {
            return accessControl?.let { control ->
                val whitelistDesc = when (control.whitelistMode) {
                    Config.WhitelistMode.DISABLED -> "白名单已禁用"
                    Config.WhitelistMode.EMPTY_ALLOW_ALL -> "白名单模式: EMPTY_ALLOW_ALL"
                    Config.WhitelistMode.EMPTY_DENY_ALL -> "白名单模式: EMPTY_DENY_ALL"
                }
                val blacklistDesc = if (control.blacklistEnabled) {
                    " (黑名单已启用)"
                } else {
                    " (黑名单已禁用)"
                }
                whitelistDesc + blacklistDesc
            } ?: "未加载"
        }

        /**
         * 获取统计信息
         */
        fun getStats(): AccessControlStats {
            return accessControl?.let { control ->
                AccessControlStats(
                    whitelistCount = control.whitelistEntries.size,
                    blacklistCount = control.blacklistEntries.size,
                    whitelistMode = control.whitelistMode,
                    blacklistEnabled = control.blacklistEnabled
                )
            } ?: AccessControlStats()
        }
    }

    data class AccessControlStats(
        val whitelistCount: Int = 0,
        val blacklistCount: Int = 0,
        val whitelistMode: Config.WhitelistMode = Config.WhitelistMode.DISABLED,
        val blacklistEnabled: Boolean = false
    )
}
