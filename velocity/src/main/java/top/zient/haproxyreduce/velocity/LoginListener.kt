package top.zient.haproxyreduce.velocity

import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import org.slf4j.Logger
import top.zient.haproxyreduce.common.LogMessages

class LoginListener(private val logger: Logger) {

    @Subscribe
    fun onLogin(event: LoginEvent) {
        val player = event.player
        val username = player.username
        val playerUUID = player.uniqueId
        val channelAddr = player.remoteAddress.toString()

        val connInfo = ConnectionTracker.associateAndLog(playerUUID, username, channelAddr)

        if (connInfo == null) {
            val addr = channelAddr.removePrefix("/")
            logger.info(LogMessages.PLAYER_CONNECTED_DIRECT, username, addr)
        }
    }

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        val player = event.player
        ConnectionTracker.removePlayer(player.uniqueId)
        logger.info(LogMessages.PLAYER_DISCONNECTED, player.username)
    }
}
