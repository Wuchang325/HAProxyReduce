package top.zient.haproxyreduce.paper

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.ProtocolDetectionResult
import io.netty.handler.codec.ProtocolDetectionState
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder
import io.netty.handler.codec.haproxy.HAProxyProtocolVersion
import org.slf4j.Logger
import top.zient.haproxyreduce.common.Config
import top.zient.haproxyreduce.common.ProxyAccessControl

class PaperDetectorHandler(private val logger: Logger, private val config: Config) : ByteToMessageDecoder() {
    init {
        isSingleDecode = true // 仅解码一次
    }

    override fun decode(ctx: ChannelHandlerContext, inBuf: ByteBuf, out: MutableList<Any>) {
        inBuf.markReaderIndex()  // 标记读指针位置

        try {
            // 检测HAProxy协议
            val detectionResult: ProtocolDetectionResult<HAProxyProtocolVersion> =
                HAProxyMessageDecoder.detectProtocol(inBuf)

            when (detectionResult.state()) {
                ProtocolDetectionState.NEEDS_MORE_DATA -> {
                    inBuf.resetReaderIndex()  // 重置读指针
                    return // 等待更多数据
                }
                ProtocolDetectionState.INVALID -> {
                    inBuf.resetReaderIndex()  // 重置读指针
                    // 非HAProxy协议，移除当前处理器
                    ctx.pipeline().remove(this)
                }
                ProtocolDetectionState.DETECTED -> {
                    // 检查远程地址是否被允许
                    val remoteAddr = ctx.channel().remoteAddress()
                    if (!ProxyAccessControl.isAllowed(remoteAddr)) {
                        // 不被允许：记录警告但允许连接（使用原始IP）
                        ProxyAccessControl.getWarningFor(remoteAddr, config)?.let { logger.warn(it) }
                        inBuf.resetReaderIndex()  // 重置读指针
                        ctx.pipeline().remove(this)  // 移除检测器但不关闭连接
                    } else {
                        // 被允许：替换为正式解码器
                        val pipeline = ctx.pipeline()
                        try {
                            pipeline.replace(this, "haproxy-decoder", HAProxyMessageDecoder())
                        } catch (ignored: IllegalArgumentException) {
                            pipeline.remove(this)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            inBuf.resetReaderIndex()  // 异常时重置读指针
            logger.warn("代理检测异常", t)
            ctx.pipeline().remove(this)
        }
    }
}
