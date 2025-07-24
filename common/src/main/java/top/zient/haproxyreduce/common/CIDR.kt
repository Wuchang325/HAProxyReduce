package top.zient.haproxyreduce.common

import org.apache.commons.validator.routines.InetAddressValidator
import java.math.BigInteger
import java.net.InetAddress
import java.net.UnknownHostException

class CIDR private constructor(val address: InetAddress, val prefix: Int) {
    private val mask: BigInteger
    private val network: BigInteger

    init {
        val bytesLen = address.address.size
        require(prefix >= 0) { "前缀长度不能为负数" }
        require(prefix <= bytesLen * 8) { "无效的前缀长度: $prefix" }

        val buf = ByteArray(bytesLen) { 0xFF.toByte() }
        mask = BigInteger(1, buf).shiftRight(prefix).not()
        network = BigInteger(1, address.address).and(mask)
    }

    fun contains(other: String): Boolean {
        return try {
            contains(InetAddress.getByName(other))
        } catch (e: UnknownHostException) {
            false
        }
    }

    fun contains(other: InetAddress): Boolean {
        val bytes = other.address
        if (bytes.size != address.address.size) return false
        val target = BigInteger(1, bytes)
        return network == target.and(mask)
    }

    override fun toString() = "${address.hostAddress}/$prefix"

    companion object {
        fun parse(cidr: String): List<CIDR> {
            val idx = cidr.lastIndexOf('/')
            if (idx != -1) {
                val addrPart = cidr.substring(0, idx)
                require(addrPart.isNotEmpty()) { "无效的CIDR格式: \"$cidr\"" }
                require(InetAddressValidator.getInstance().isValid(addrPart)) {
                    "CIDR必须包含有效的IP地址: $addrPart"
                }

                return try {
                    val addr = InetAddress.getByName(addrPart)
                    val prefix = cidr.substring(idx + 1).toInt()
                    listOf(CIDR(addr, prefix))
                } catch (e: Exception) {
                    throw IllegalArgumentException("无效的CIDR格式: \"$cidr\"", e)
                }
            } else {
                require(cidr.isNotEmpty()) { "CIDR字符串不能为空" }
                val addresses = InetAddress.getAllByName(cidr)
                return addresses.map { CIDR(it, it.address.size * 8) }
            }
        }
    }
}