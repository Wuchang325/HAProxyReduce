import org.slf4j.Logger
import org.slf4j.LoggerFactory
import top.zient.haproxyreduce.common.LogMessages
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object ConnectionTracker {
    private val pendingConnections = ConcurrentHashMap<String, ConnectionInfo>()
    private val playerConnections = ConcurrentHashMap<UUID, ConnectionInfo>()
    private val logger: org.slf4j.Logger = org.slf4j.LoggerFactory.getLogger("HAProxyReduce")

    data class ConnectionInfo(
        val realAddr: String,
        val frpcAddr: String,
        val channelAddr: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    /**
     * 记录连接
     */
    fun recordConnection(channelAddr: String, realAddr: String, frpcAddr: String) {
        pendingConnections[channelAddr] = ConnectionInfo(realAddr, frpcAddr, channelAddr)
        logger.debug(LogMessages.CONNECTION_TRACKED, channelAddr, realAddr)
    }

    /**
     * 通过 UUID 关联并输出
     */
    fun associateAndLog(playerUUID: UUID, username: String, channelAddr: String): ConnectionInfo? {
        val info = pendingConnections.remove(channelAddr)
            ?: pendingConnections.values.find {
                channelAddr.contains(it.realAddr.split(":")[0])
            }

        if (info != null) {
            playerConnections[playerUUID] = info
            logger.info(LogMessages.CONNECTION_ASSOCIATED, username, info.realAddr, info.frpcAddr)
            return info
        }

        logger.debug(LogMessages.CONNECTION_NOT_FOUND, channelAddr)
        return null
    }

    /**
     * 玩家断开时清理
     */
    fun removePlayer(playerUUID: UUID) {
        playerConnections.remove(playerUUID)
    }

    /**
     * 清理超时的待处理连接
     */
    fun cleanupOldConnections(maxAgeMs: Long = 30000) {
        val now = System.currentTimeMillis()
        pendingConnections.entries.removeIf { (_, value) ->
            (now - value.timestamp) > maxAgeMs
        }
    }
}
