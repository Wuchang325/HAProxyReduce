package top.zient.haproxyreduce.paper

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.haproxy.HAProxyMessage
import org.slf4j.Logger
import top.zient.haproxyreduce.common.LogMessages

/**
 * 记录玩家连接信息（Paper/Folia 版本）
 * 输出格式: player(真实IP -> 转发IP)
 */
class PaperConnectionLogger(private val logger: Logger) : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HAProxyMessage) {
            val sourceAddress = msg.sourceAddress()
            val destinationAddress = msg.destinationAddress()
            val sourcePort = msg.sourcePort()
            val destPort = msg.destinationPort()

            // ✅ 输出连接信息
            logger.info(LogMessages.CONNECTION_LOG, sourceAddress, sourcePort, destinationAddress, destPort)

            // 调试信息
            if (logger.isDebugEnabled) {
                logger.debug(LogMessages.PROTOCOL_VERSION, msg.protocolVersion())
                logger.debug(LogMessages.PROTOCOL_COMMAND, msg.command())
                logger.debug(LogMessages.SOURCE_ADDRESS, sourceAddress, sourcePort)
                logger.debug(LogMessages.DESTINATION_ADDRESS, destinationAddress, destPort)
            }
        }

        super.channelRead(ctx, msg)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn(LogMessages.CONNECTION_LOG_EXCEPTION, cause)
        ctx.fireExceptionCaught(cause)
    }
}
