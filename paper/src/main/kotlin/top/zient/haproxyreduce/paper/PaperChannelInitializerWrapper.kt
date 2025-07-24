package top.zient.haproxyreduce.paper

import io.netty.channel.Channel
import org.slf4j.Logger
import java.lang.reflect.Method

// 包装Bukkit原始通道初始化器，添加自定义处理器
class PaperChannelInitializerWrapper(
    private val original: Any,
    private val logger: Logger
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
                PaperDetectorHandler(logger)
            )
        } catch (e: Exception) {
            logger.error("Paper通道初始化失败", e)
        }
    }
}