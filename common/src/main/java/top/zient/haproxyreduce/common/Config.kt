package top.zient.haproxyreduce.common

import org.yaml.snakeyaml.Yaml
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * HAProxyReduce 主配置类
 */
data class Config(
    val whitelist: WhitelistConfig = WhitelistConfig(),
    val blacklist: BlacklistConfig = BlacklistConfig(),
    val logging: LoggingConfig = LoggingConfig(),
    val connectionTracker: ConnectionTrackerConfig = ConnectionTrackerConfig(),
    val hotReload: HotReloadConfig = HotReloadConfig()
) {
    data class WhitelistConfig(
        /** 白名单模式 */
        val mode: WhitelistMode = WhitelistMode.EMPTY_DENY_ALL,
        /** IP 列表（支持 CIDR） */
        val ips: List<String> = listOf("127.0.0.0/8", "::1/128")
    )

    data class BlacklistConfig(
        /** 是否启用黑名单 */
        val enabled: Boolean = false,
        /** IP 列表（支持 CIDR） */
        val ips: List<String> = emptyList()
    )

    data class LoggingConfig(
        /** 调试模式 */
        val debug: Boolean = false,
        /** 相同 IP 只警告一次 */
        val warnOnce: Boolean = true
    )

    data class ConnectionTrackerConfig(
        /** 清理间隔（分钟） */
        val cleanupInterval: Int = 5,
        /** 连接超时时间（秒） */
        val timeout: Int = 30
    )

    data class HotReloadConfig(
        /** 是否启用配置热重载 */
        val enabled: Boolean = true,
        /** 检查间隔（秒） */
        val checkInterval: Int = 30
    )

    enum class WhitelistMode {
        /** 空列表时拒绝所有 HAProxy 连接 */
        EMPTY_DENY_ALL,
        /** 空列表时允许所有 HAProxy 连接 */
        EMPTY_ALLOW_ALL,
        /** 完全禁用白名单检查 */
        DISABLED
    }

    companion object {
        @JvmStatic
        fun load(path: Path): Config {
            Files.createDirectories(path.parent)

            if (!Files.exists(path)) {
                saveDefault(path)
            }

            return try {
                val yaml = Yaml()
                Files.newInputStream(path).use { inputStream ->
                    val map: Map<String, Any> = yaml.load(inputStream) ?: emptyMap()
                    parseConfig(map)
                }
            } catch (e: Exception) {
                throw IOException("无法加载配置文件: ${e.message}", e)
            }
        }

        private fun saveDefault(path: Path) {
            val content = """# HAProxyReduce 配置文件
# 此插件同时支持代理和直连连接

whitelist:
  # 白名单模式:
  # - EMPTY_DENY_ALL: 空列表时拒绝所有 HAProxy 连接（使用原始IP，不解析协议）
  # - EMPTY_ALLOW_ALL: 空列表时允许所有 HAProxy 连接（解析所有代理协议）
  # - DISABLED: 完全禁用白名单检查，允许所有连接
  mode: "EMPTY_DENY_ALL"

  # IP 白名单列表（支持 CIDR 格式，如 192.168.1.0/24）
  # 注意：只在 mode 不为 DISABLED 时生效
  ips:
    - "127.0.0.0/8"      # 本地回环
    - "::1/128"          # IPv6 本地回环
    # - "10.0.0.0/8"     # 私有网络示例
    # - "172.16.0.0/12"  # 私有网络示例
    # - "192.168.0.0/16" # 私有网络示例

blacklist:
  # 是否启用黑名单（优先级高于白名单）
  enabled: false

  # IP 黑名单列表（支持 CIDR 格式）
  # 黑名单中的 IP 将被拒绝 HAProxy 协议解析
  ips:
    # - "192.168.1.100/32"  # 单个IP示例
    # - "10.0.0.0/8"        # 网络段示例

logging:
  # 是否启用调试日志（输出详细连接信息）
  debug: false
  # 是否对同一 IP 只警告一次（避免日志刷屏）
  warn-once: true

connection-tracker:
  # 清理间隔（分钟）
  cleanup-interval: 5
  # 待处理连接超时时间（秒）
  timeout: 30

hot-reload:
  # 是否启用配置热重载（修改配置后自动生效，无需重启服务器）
  enabled: true
  # 配置检查间隔（秒）
  check-interval: 30
"""
            Files.write(path, content.toByteArray(StandardCharsets.UTF_8))
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseConfig(map: Map<String, Any>): Config {
            val whitelistMap = map["whitelist"] as? Map<String, Any> ?: emptyMap()
            val blacklistMap = map["blacklist"] as? Map<String, Any> ?: emptyMap()
            val loggingMap = map["logging"] as? Map<String, Any> ?: emptyMap()
            val trackerMap = map["connection-tracker"] as? Map<String, Any> ?: emptyMap()
            val hotReloadMap = map["hot-reload"] as? Map<String, Any> ?: emptyMap()

            val modeStr = (whitelistMap["mode"] as? String)?.uppercase() ?: "EMPTY_DENY_ALL"
            val mode = try {
                WhitelistMode.valueOf(modeStr)
            } catch (e: IllegalArgumentException) {
                WhitelistMode.EMPTY_DENY_ALL
            }

            return Config(
                whitelist = WhitelistConfig(
                    mode = mode,
                    ips = (whitelistMap["ips"] as? List<String>) ?: listOf("127.0.0.0/8", "::1/128")
                ),
                blacklist = BlacklistConfig(
                    enabled = (blacklistMap["enabled"] as? Boolean) ?: false,
                    ips = (blacklistMap["ips"] as? List<String>) ?: emptyList()
                ),
                logging = LoggingConfig(
                    debug = (loggingMap["debug"] as? Boolean) ?: false,
                    warnOnce = (loggingMap["warn-once"] as? Boolean) ?: true
                ),
                connectionTracker = ConnectionTrackerConfig(
                    cleanupInterval = (trackerMap["cleanup-interval"] as? Int) ?: 5,
                    timeout = (trackerMap["timeout"] as? Int) ?: 30
                ),
                hotReload = HotReloadConfig(
                    enabled = (hotReloadMap["enabled"] as? Boolean) ?: true,
                    checkInterval = (hotReloadMap["check-interval"] as? Int) ?: 30
                )
            )
        }
    }
}
