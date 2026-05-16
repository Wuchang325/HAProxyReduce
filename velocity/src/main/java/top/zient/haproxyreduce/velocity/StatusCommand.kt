package top.zient.haproxyreduce.velocity

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.command.SimpleCommand.Invocation
import net.kyori.adventure.text.Component
import top.zient.haproxyreduce.api.HAProxyReduceAPI

class StatusCommand : SimpleCommand {
    override fun execute(invocation: Invocation) {
        val source = invocation.source()
        if (!source.hasPermission("haproxyreduce.status")) {
            source.sendMessage(Component.text("没有权限执行此命令"))
            return
        }
        val api = HAProxyReduceAPI.getInstance()
        val whitelist = api?.getWhitelist() ?: emptyList<String>()
        val blacklist = api?.getBlacklist() ?: emptyList<String>()
        source.sendMessage(Component.text("HAProxyReduce 状态： 白名单=${whitelist.size} 黑名单=${blacklist.size}"))
    }
}
