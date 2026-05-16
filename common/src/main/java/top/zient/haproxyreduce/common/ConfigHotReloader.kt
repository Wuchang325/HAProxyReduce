package top.zient.haproxyreduce.common

import org.slf4j.Logger
import java.nio.file.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime

/**
 * 配置热重载管理器
 */
class ConfigHotReloader(
    private val configPath: Path,
    private val logger: Logger,
    private val onConfigReload: (Config) -> Unit
) {
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "HAProxyReduce-Config-Reloader").apply {
            isDaemon = true
        }
    }

    private var lastModified: Long = 0
    private var isRunning = false

    /**
     * 启动热重载
     */
    fun start(config: Config) {
        if (isRunning) return

        if (!config.hotReload.enabled) {
            logger.info(LogMessages.HOT_RELOAD_DISABLED)
            return
        }

        updateLastModified()
        isRunning = true

        executor.scheduleWithFixedDelay(
            { checkAndReload() },
            config.hotReload.checkInterval.toLong(),
            config.hotReload.checkInterval.toLong(),
            TimeUnit.SECONDS
        )

        logger.info(LogMessages.HOT_RELOAD_ENABLED, config.hotReload.checkInterval)
    }

    /**
     * 停止热重载
     */
    fun stop() {
        if (!isRunning) return

        isRunning = false
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }

        logger.info(LogMessages.HOT_RELOAD_STOPPED)
    }

    /**
     * 手动触发重载检查
     */
    fun checkNow(): Boolean {
        return checkAndReload()
    }

    private fun checkAndReload(): Boolean {
        if (!isRunning) return false

        try {
            if (!configPath.exists()) {
                logger.warn(LogMessages.CONFIG_FILE_NOT_EXISTS, configPath)
                return false
            }

            val currentModified = configPath.getLastModifiedTime().toMillis()
            if (currentModified > lastModified) {
                logger.info(LogMessages.CONFIG_CHANGE_DETECTED)
                lastModified = currentModified

                val newConfig = Config.load(configPath)
                onConfigReload(newConfig)
                logger.info(LogMessages.CONFIG_RELOAD_COMPLETE)
                return true
            }
        } catch (e: Exception) {
            logger.error(LogMessages.CONFIG_RELOAD_FAILED, e)
        }

        return false
    }

    private fun updateLastModified() {
        try {
            if (configPath.exists()) {
                lastModified = configPath.getLastModifiedTime().toMillis()
            }
        } catch (e: Exception) {
            logger.warn(LogMessages.FILE_MODIFICATION_TIME_ERROR, e)
        }
    }
}
