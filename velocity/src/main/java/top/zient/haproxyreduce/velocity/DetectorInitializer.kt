package top.zient.haproxyreduce.velocity

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder
import org.slf4j.Logger
import top.zient.haproxyreduce.common.Config
import top.zient.haproxyreduce.common.LogMessages
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field

class DetectorInitializer(
    private val logger: Logger,
    private val config: Config,
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
            logger.error(LogMessages.CHANNEL_INIT_FAILED, e)
            return
        }

        val pipeline = ch.pipeline()

        if (!ch.isOpen) {
            logger.debug(LogMessages.CHANNEL_CLOSED_SKIP_INJECTION)
            return
        }

        if (pipeline.get("haproxy-detector") != null) {
            logger.debug(LogMessages.DETECTOR_ALREADY_EXISTS)
            return
        }

        val decoder = pipeline.get(HAProxyMessageDecoder::class.java)

        if (decoder == null) {
            logger.error(LogMessages.DECODER_NOT_FOUND)
            throw IllegalStateException(
                "HAProxy 支持未启用。请在 velocity.toml 中设置 proxy-protocol = true"
            )
        }

        try {
            // 传递 config 给 HAProxyDetectorHandler
            pipeline.replace(decoder, "haproxy-detector", HAProxyDetectorHandler(logger, config))
            logger.debug(LogMessages.DETECTOR_INJECTION_SUCCESS)
        } catch (e: Exception) {
            logger.error(LogMessages.DECODER_REPLACEMENT_FAILED, e)
            throw IllegalStateException("注入 HAProxy 检测器失败", e)
        }
    }
}
