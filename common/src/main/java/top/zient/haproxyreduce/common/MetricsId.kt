package top.zient.haproxyreduce.common

import org.bstats.charts.CustomChart
import org.bstats.charts.SimplePie

object MetricsId {
    const val KEY_WHITELIST_MODE = "whitelist_mode"
    const val KEY_WHITELIST_COUNT = "whitelist_count"

    fun createWhitelistModeChart(): CustomChart {
        return SimplePie(KEY_WHITELIST_MODE) {
            ProxyWhitelist.getModeDescription()
        }
    }

    fun createWhitelistCountChart(): CustomChart {
        return SimplePie(KEY_WHITELIST_COUNT) {
            // 通过检查特定 IP 来估算白名单大小
            // 由于新设计封装了 entries，我们返回模式信息作为补充
            when (ProxyWhitelist.whitelist?.let { "configured" } ?: "not_loaded") {
                "configured" -> "已配置"
                else -> "未加载"
            }
        }
    }
}
