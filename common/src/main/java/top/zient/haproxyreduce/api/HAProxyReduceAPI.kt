package top.zient.haproxyreduce.api

import top.zient.haproxyreduce.common.Config
import top.zient.haproxyreduce.common.ProxyAccessControl
import java.net.SocketAddress

/**
 * HAProxyReduce API 接口
 * 其他插件可以通过此接口访问HAProxyReduce的功能
 */
interface HAProxyReduceAPI {

    /**
     * 检查地址是否应该解析HAProxy协议
     * @param address 客户端地址
     * @return true表示应该解析协议，false表示使用原始IP
     */
    fun shouldParseHAProxy(address: SocketAddress?): Boolean

    /**
     * 检查地址是否被允许连接
     * @param address 客户端地址
     * @return true表示允许，false表示拒绝
     */
    fun isAllowed(address: SocketAddress?): Boolean

    /**
     * 检查地址是否在黑名单中
     * @param address 客户端地址
     * @return true表示在黑名单中
     */
    fun isBlacklisted(address: SocketAddress?): Boolean

    /**
     * 检查地址是否在白名单中
     * @param address 客户端地址
     * @return true表示在白名单中
     */
    fun isWhitelisted(address: SocketAddress?): Boolean

    /**
     * 获取当前配置
     * @return 当前配置对象
     */
    fun getConfig(): Config

    /**
     * 获取访问控制统计信息
     * @return 统计信息
     */
    fun getAccessControlStats(): ProxyAccessControl.AccessControlStats

    /**
     * 重新加载配置
     * @return true表示重载成功，false表示失败
     */
    fun reloadConfig(): Boolean

    /**
     * 添加IP到白名单
     * @param ip IP地址（支持CIDR格式）
     * @return true表示添加成功，false表示失败
     */
    fun addToWhitelist(ip: String): Boolean

    /**
     * 从白名单中移除IP
     * @param ip IP地址（支持CIDR格式）
     * @return true表示移除成功，false表示失败
     */
    fun removeFromWhitelist(ip: String): Boolean

    /**
     * 添加IP到黑名单
     * @param ip IP地址（支持CIDR格式）
     * @return true表示添加成功，false表示失败
     */
    fun addToBlacklist(ip: String): Boolean

    /**
     * 从黑名单中移除IP
     * @param ip IP地址（支持CIDR格式）
     * @return true表示移除成功，false表示失败
     */
    fun removeFromBlacklist(ip: String): Boolean

    /**
     * 获取白名单IP列表
     * @return 白名单IP列表
     */
    fun getWhitelist(): List<String>

    /**
     * 获取黑名单IP列表
     * @return 黑名单IP列表
     */
    fun getBlacklist(): List<String>

    companion object {
        /**
         * 获取API实例
         * @return API实例，如果插件未启用则返回null
         */
        @JvmStatic
        fun getInstance(): HAProxyReduceAPI? {
            return HAProxyReduceAPIImpl.instance
        }
    }
}
