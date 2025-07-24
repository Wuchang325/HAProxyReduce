package top.zient.haproxyreduce.velocity

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder
import org.slf4j.Logger
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles

class DetectorInitializer(
    private val logger: Logger,
    private val delegate: ChannelInitializer<Channel>
) : ChannelInitializer<Channel>() {

    companion object {
        private val INIT_CHANNEL: MethodHandle = run {
            val method = ChannelInitializer::class.java.getDeclaredMethod("initChannel", Channel::class.java)
            method.isAccessible = true
            MethodHandles.lookup().unreflect(method)
        }
    }

    override fun initChannel(ch: Channel) {
        try {
            INIT_CHANNEL.invoke(delegate, ch)
        } catch (e: Throwable) {
            logger.error("初始化通道失败", e)
            return
        }

        val pipeline = ch.pipeline()
        if (!ch.isOpen || pipeline.get("haproxy-detector") != null) return

        try {
            val decoder = pipeline.get(HAProxyMessageDecoder::class.java)
            pipeline.replace(decoder, "haproxy-detector", HAProxyDetectorHandler(logger))
        } catch (e: Exception) {
            logger.error("未启用HAProxy支持", e)
            throw RuntimeException("未启用HAProxy支持", e)
        }
    }
}