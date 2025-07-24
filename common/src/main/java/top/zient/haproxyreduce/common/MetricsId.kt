package top.zient.haproxyreduce.common

import org.bstats.charts.CustomChart
import org.bstats.charts.SimplePie

object MetricsId {
    const val KEY_WHITELIST_COUNT = "whitelist_count"

    fun createWhitelistCountChart(): CustomChart {
        return SimplePie(KEY_WHITELIST_COUNT) {
            ProxyWhitelist.whitelist?.size?.toString() ?: "0"
        }
    }
}