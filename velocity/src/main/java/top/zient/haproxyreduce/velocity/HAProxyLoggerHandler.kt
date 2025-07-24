/*
* 此功能开发中，当前类无任何作用
* - Wuchang325
*
* */

package top.zient.haproxyreduce.velocity

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.haproxy.HAProxyMessage
import org.slf4j.Logger

class HAProxyLoggerHandler(private val logger: Logger) : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is HAProxyMessage) {
            val srcAddress = msg.sourceAddress()
            val destAddress = msg.destinationAddress()

            logger.info("HAProxy连接 - 源地址: $srcAddress, 目标地址: $destAddress")
        }
        super.channelRead(ctx, msg) // 继续传递消息
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.warn("HAProxy日志处理异常", cause)
        ctx.fireExceptionCaught(cause)
    }
}