 package top.zient.haproxyreduce.api

import top.zient.haproxyreduce.common.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.net.SocketAddress

/**
 * HAProxyReduce API 实现
 */
class HAProxyReduceAPIImpl private constructor(
    private val configPath: Path,
    private var config: Config
) : HAProxyReduceAPI {

    init {
        // 初始化访问控制
        ProxyAccessControl.load(config)
    }

    override fun shouldParseHAProxy(address: SocketAddress?): Boolean {
        return ProxyAccessControl.shouldParse(address)
    }

    override fun isAllowed(address: SocketAddress?): Boolean {
        return ProxyAccessControl.isAllowed(address)
    }

    override fun isBlacklisted(address: SocketAddress?): Boolean {
        return ProxyAccessControl.isBlacklisted(address)
    }

    override fun isWhitelisted(address: SocketAddress?): Boolean {
        return ProxyAccessControl.isWhitelisted(address)
    }

    override fun getConfig(): Config {
        return config
    }

    override fun getAccessControlStats(): ProxyAccessControl.AccessControlStats {
        return ProxyAccessControl.getStats()
    }

    override fun reloadConfig(): Boolean {
        return try {
            val newConfig = Config.load(configPath)
            config = newConfig
            ProxyAccessControl.load(config)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun addToWhitelist(ip: String): Boolean {
        return try {
            val currentIps = config.whitelist.ips.toMutableList()
            if (!currentIps.contains(ip)) {
                currentIps.add(ip)
                val newConfig = config.copy(
                    whitelist = config.whitelist.copy(ips = currentIps)
                )
                saveConfig(newConfig)
                config = newConfig
                ProxyAccessControl.load(config)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun removeFromWhitelist(ip: String): Boolean {
        return try {
            val currentIps = config.whitelist.ips.toMutableList()
            if (currentIps.remove(ip)) {
                val newConfig = config.copy(
                    whitelist = config.whitelist.copy(ips = currentIps)
                )
                saveConfig(newConfig)
                config = newConfig
                ProxyAccessControl.load(config)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun addToBlacklist(ip: String): Boolean {
        return try {
            val currentIps = config.blacklist.ips.toMutableList()
            if (!currentIps.contains(ip)) {
                currentIps.add(ip)
                val newConfig = config.copy(
                    blacklist = config.blacklist.copy(
                        enabled = true,
                        ips = currentIps
                    )
                )
                saveConfig(newConfig)
                config = newConfig
                ProxyAccessControl.load(config)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun removeFromBlacklist(ip: String): Boolean {
        return try {
            val currentIps = config.blacklist.ips.toMutableList()
            if (currentIps.remove(ip)) {
                val newConfig = config.copy(
                    blacklist = config.blacklist.copy(ips = currentIps)
                )
                saveConfig(newConfig)
                config = newConfig
                ProxyAccessControl.load(config)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    override fun getWhitelist(): List<String> {
        return config.whitelist.ips
    }

    override fun getBlacklist(): List<String> {
        return config.blacklist.ips
    }

    private fun saveConfig(config: Config) {
        // 这里需要实现配置保存逻辑
        // 由于Config类没有保存方法，我们需要手动序列化
        val yaml = org.yaml.snakeyaml.Yaml()
        val configMap = mapOf(
            "whitelist" to mapOf(
                "mode" to config.whitelist.mode.name,
                "ips" to config.whitelist.ips
            ),
            "blacklist" to mapOf(
                "enabled" to config.blacklist.enabled,
                "ips" to config.blacklist.ips
            ),
            "logging" to mapOf(
                "debug" to config.logging.debug,
                "warn-once" to config.logging.warnOnce
            ),
            "connection-tracker" to mapOf(
                "cleanup-interval" to config.connectionTracker.cleanupInterval,
                "timeout" to config.connectionTracker.timeout
            ),
            "hot-reload" to mapOf(
                "enabled" to config.hotReload.enabled,
                "check-interval" to config.hotReload.checkInterval
            )
        )

        Files.createDirectories(configPath.parent)
        Files.newBufferedWriter(configPath).use { writer ->
            yaml.dump(configMap, writer)
        }
    }

    companion object {
        @Volatile
        var instance: HAProxyReduceAPIImpl? = null
            private set

        /**
         * 创建API实例
         */
        fun create(configPath: Path, config: Config): HAProxyReduceAPIImpl {
            return HAProxyReduceAPIImpl(configPath, config).also {
                instance = it
            }
        }

        /**
         * 销毁API实例
         */
        fun destroy() {
            instance = null
        }
    }
}
