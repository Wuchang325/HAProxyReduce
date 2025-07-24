package top.zient.haproxyreduce.common

import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class ProxyWhitelist private constructor(private val entries: List<CIDR>) {
    fun matches(address: InetAddress): Boolean {
        return entries.any { it.contains(address) }
    }

    val size: Int get() = entries.size

    companion object {
        @Volatile // 增加volatile保证线程可见性
        var whitelist: ProxyWhitelist? = null
        private var lastWarning: InetAddress? = null

        /**
         * 检查地址是否在白名单中
         * 现在只用于记录，不再拒绝连接
         */
        fun check(address: SocketAddress): Boolean {
            return whitelist?.let {
                (address as? InetSocketAddress)?.address?.let { addr ->
                    it.matches(addr)
                } ?: false // 非InetSocketAddress类型返回false
            } ?: false // 白名单未初始化时返回false
        }

        fun getWarningFor(address: SocketAddress): String? {
            val inetAddr = (address as? InetSocketAddress)?.address ?: return null
            // 避免重复打印相同警告
            if (inetAddr != lastWarning) {
                lastWarning = inetAddr
                return "检测到来自 ${inetAddr.hostAddress} 的代理连接，但该地址不在白名单中（使用原始IP）"
            }
            return null
        }

        @Throws(IOException::class)
        fun load(path: Path): ProxyWhitelist {
            val entries = mutableListOf<CIDR>()
            Files.newBufferedReader(path, StandardCharsets.UTF_8).use { reader ->
                var firstLine = true
                reader.lineSequence().forEach { line ->
                    val trimmed = line.trim()
                    when {
                        trimmed.isEmpty() || trimmed.startsWith("#") -> return@forEach
                        firstLine && trimmed.startsWith("YesIReallyWantToDisableWhitelist") -> {
                            return ProxyWhitelist(emptyList())
                        }
                        else -> entries.addAll(CIDR.parse(trimmed))
                    }
                    firstLine = false
                }
            }
            return ProxyWhitelist(entries)
        }

        @Throws(IOException::class)
        fun loadOrDefault(path: Path): ProxyWhitelist {
            Files.createDirectories(path.parent)
            if (!Files.exists(path) || Files.isDirectory(path)) {
                Files.write(path, listOf(
                    "# 允许的代理IP列表",
                    "#",
                    "# 空列表将允许所有代理连接但使用原始IP",
                    "# 每行可以是IP地址、域名或CIDR格式",
                    "# 域名仅在启动时解析一次",
                    "# 每个域名解析的所有IP都会被允许",
                    "# 域名中不能使用CIDR前缀",
                    "",
                    "127.0.0.0/8",
                    "::1/128"
                ), StandardCharsets.UTF_8)
            }
            return load(path)
        }
    }
}