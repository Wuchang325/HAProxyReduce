package top.zient.haproxyreduce.paper

import io.netty.channel.Channel
import org.slf4j.Logger
import top.zient.haproxyreduce.common.Config
import top.zient.haproxyreduce.common.LogMessages
import java.lang.reflect.Method

// 包装Bukkit原始通道初始化器，添加自定义处理器
class PaperChannelInitializerWrapper(
    private val original: Any,
    private val logger: Logger,
    private val config: Config
) {
    // 反射调用原始初始化方法
    fun initChannel(channel: Channel) {
        try {
            // 调用原始初始化逻辑
            val initMethod: Method = original.javaClass.getMethod("initChannel", Channel::class.java)
            initMethod.invoke(original, channel)

            // 添加自定义处理器
            channel.pipeline().addBefore(
                "packet_handler",
                "haproxy-detector",
                PaperDetectorHandler(logger, config)
            )
        } catch (e: Exception) {
            logger.error(LogMessages.PAPER_CHANNEL_INIT_FAILED, e)
        }
    }
}
