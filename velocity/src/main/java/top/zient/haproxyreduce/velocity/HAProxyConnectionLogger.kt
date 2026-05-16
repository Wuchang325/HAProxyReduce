package top.zient.haproxyreduce.velocity

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.haproxy.HAProxyMessage
import org.slf4j.Logger
import top.zient.haproxyreduce.common.LogMessages

/**
 * 记录玩家连接信息到 ConnectionTracker
 */
class HAProxyConnectionLogger(private val logger: Logger) : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HAProxyMessage) {
            val sourceAddress = msg.sourceAddress()
            val sourcePort = msg.sourcePort()

            val proxyChannelAddr = ctx.channel().remoteAddress()?.toString() ?: "unknown"
            val frpcAddr = proxyChannelAddr.removePrefix("/")
            ConnectionTracker.recordConnection(
                channelAddr = proxyChannelAddr,
                realAddr = "$sourceAddress:$sourcePort",
                frpcAddr = frpcAddr
            )

            if (logger.isDebugEnabled) {
                logger.debug(LogMessages.CONNECTION_RECORDED, sourceAddress, sourcePort, frpcAddr, proxyChannelAddr)
            }
        }

        super.channelRead(ctx, msg)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn(LogMessages.HAPROXY_CONNECTION_LOG_EXCEPTION, cause)
        ctx.fireExceptionCaught(cause)
    }
}
