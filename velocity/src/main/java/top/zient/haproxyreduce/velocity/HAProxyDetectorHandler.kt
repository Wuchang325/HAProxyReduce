package top.zient.haproxyreduce.velocity

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.ProtocolDetectionResult
import io.netty.handler.codec.ProtocolDetectionState
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion
import org.slf4j.Logger
import top.zient.haproxyreduce.common.ProxyWhitelist

class HAProxyDetectorHandler(private val logger: Logger) : ByteToMessageDecoder() {

    init {
        setSingleDecode(true)
    }

    override fun decode(ctx: ChannelHandlerContext, inBuf: ByteBuf, out: MutableList<Any>) {
        try {
            val detectionResult: ProtocolDetectionResult<HAProxyProtocolVersion> =
                HAProxyMessageDecoder.detectProtocol(inBuf)

            when (detectionResult.state()) {
                ProtocolDetectionState.NEEDS_MORE_DATA -> return
                ProtocolDetectionState.INVALID -> {
                    // 非proxy-protocol数据，移除检测器
                    ctx.pipeline().remove(this)
                }
                ProtocolDetectionState.DETECTED -> {
                    val addr = ctx.channel().remoteAddress()
                    if (ProxyWhitelist.check(addr)) {
                        // 白名单IP：启用解析器
                        val pipeline = ctx.pipeline()
                        pipeline.replace(this, "haproxy-decoder", HAProxyMessageDecoder())
                    } else {
                        // 非白名单IP：移除检测器，不解析协议
                        ProxyWhitelist.getWarningFor(addr)?.let { logger.warn(it) }
                        ctx.pipeline().remove(this)

                        // 可选：重置读取指针，避免协议头干扰服务端
                        inBuf.resetReaderIndex()
                    }
                }
            }
        } catch (t: Throwable) {
            logger.warn("检测代理时发生异常", t)
        }
    }
}