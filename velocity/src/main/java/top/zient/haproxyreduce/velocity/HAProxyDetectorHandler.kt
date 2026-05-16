package top.zient.haproxyreduce.velocity

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.ProtocolDetectionResult
import io.netty.handler.codec.ProtocolDetectionState
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion
import org.slf4j.Logger
import top.zient.haproxyreduce.common.Config
import top.zient.haproxyreduce.common.LogMessages
import top.zient.haproxyreduce.common.ProxyAccessControl
import top.zient.haproxyreduce.common.ProxyWhitelist

class HAProxyDetectorHandler(
    private val logger: Logger,
    private val config: Config
) : ByteToMessageDecoder() {

    init {
        isSingleDecode = true
    }

    override fun decode(ctx: ChannelHandlerContext, inBuf: ByteBuf, out: MutableList<Any>) {
        inBuf.markReaderIndex()

        try {
            val detectionResult: ProtocolDetectionResult<HAProxyProtocolVersion> =
                HAProxyMessageDecoder.detectProtocol(inBuf)

            when (detectionResult.state()) {
                ProtocolDetectionState.NEEDS_MORE_DATA -> {
                    inBuf.resetReaderIndex()
                    return
                }
                ProtocolDetectionState.INVALID -> {
                    inBuf.resetReaderIndex()
                    ctx.pipeline().remove(this)
                }
                ProtocolDetectionState.DETECTED -> {
                    val addr = ctx.channel().remoteAddress()

                    if (ProxyAccessControl.shouldParse(addr)) {
                        // 应该解析 HAProxy 协议
                        val pipeline = ctx.pipeline()
                        inBuf.resetReaderIndex()
                        pipeline.replace(this, "haproxy-decoder", HAProxyMessageDecoder())
                        pipeline.addAfter(
                            "haproxy-decoder",
                            "haproxy-logger",
                            HAProxyConnectionLogger(logger)
                        )
                        logger.debug(LogMessages.HAPROXY_PROTOCOL_ENABLED, addr)
                    } else {
                        // 不应该解析 HAProxy 协议（空列表拒绝所有或不在白名单）
                        ProxyWhitelist.getWarningFor(addr, config)?.let { logger.warn(it) }
                        inBuf.resetReaderIndex()
                        ctx.pipeline().remove(this)
                    }
                }
            }
        } catch (t: Throwable) {
            inBuf.resetReaderIndex()
            logger.warn(LogMessages.PROXY_DETECTION_EXCEPTION, t)
            try {
                ctx.pipeline().remove(this)
            } catch (_: Exception) {
            }
        }
    }
}
