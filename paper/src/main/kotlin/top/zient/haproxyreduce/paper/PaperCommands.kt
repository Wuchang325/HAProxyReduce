package top.zient.haproxyreduce.paper

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import top.zient.haproxyreduce.api.HAProxyReduceAPI

class PaperCommands(private val plugin: PaperMain) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        when (command.name.lowercase()) {
            "haproxyreload" -> {
                if (!sender.hasPermission("haproxyreduce.reload")) {
                    sender.sendMessage("§c没有权限执行此命令")
                    return true
                }
                val api = HAProxyReduceAPI.getInstance()
                val ok = api?.reloadConfig() ?: false
                sender.sendMessage(if (ok) "§a配置重载成功" else "§c配置重载失败")
                return true
            }
            "haproxystatus" -> {
                if (!sender.hasPermission("haproxyreduce.status")) {
                    sender.sendMessage("§c没有权限执行此命令")
                    return true
                }
                val api = HAProxyReduceAPI.getInstance()
                val whitelist = api?.getWhitelist() ?: emptyList()
                val blacklist = api?.getBlacklist() ?: emptyList()
                sender.sendMessage("§6HAProxyReduce 状态： 白名单=${whitelist.size} 黑名单=${blacklist.size}")
                return true
            }
        }
        return false
    }
}
