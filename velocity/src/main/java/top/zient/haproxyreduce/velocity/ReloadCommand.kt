package top.zient.haproxyreduce.velocity

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.command.SimpleCommand.Invocation
import net.kyori.adventure.text.Component
import top.zient.haproxyreduce.api.HAProxyReduceAPI

class ReloadCommand : SimpleCommand {
    override fun execute(invocation: Invocation) {
        val source = invocation.source()
        if (!source.hasPermission("haproxyreduce.reload")) {
            source.sendMessage(Component.text("没有权限执行此命令"))
            return
        }
        val api = HAProxyReduceAPI.getInstance()
        val ok = api?.reloadConfig() ?: false
        source.sendMessage(Component.text(if (ok) "配置重载成功" else "配置重载失败"))
    }
}
