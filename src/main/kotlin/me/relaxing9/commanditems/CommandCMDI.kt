package me.relaxing9.commanditems

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import co.aikar.commands.bukkit.contexts.OnlinePlayer
import com.google.common.collect.Maps
import de.tr7zw.changeme.nbtapi.NBTItem
import me.relaxing9.commanditems.data.ItemDefinition
import me.relaxing9.commanditems.data.action.ActionMathExpr
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.util.*

@CommandAlias("cmdi")
class CommandCMDI(private val plugin: CommandItems) : BaseCommand() {
    @Default
    fun onDefault(issuer: CommandSender) {
        issuer.sendMessage(
            ChatColor.AQUA.toString() + "Running " + ChatColor.GOLD + "CommandItems v" + plugin.description.version
                    + ChatColor.AQUA + " by " + ChatColor.GOLD + "Yamakaja" + ChatColor.AQUA + " & " + ChatColor.GOLD + "Relaxing9" + ChatColor.AQUA + "!"
        )
        issuer.sendMessage(ChatColor.AQUA.toString() + "See " + ChatColor.GOLD + "/cmdi help" + ChatColor.AQUA + " for more information!")
    }

    @CommandPermission("cmdi.help")
    @Syntax("[page]")
    @HelpCommand
    fun onHelp(@Default("1") page: Int?, help: CommandHelp) {
        help.page = (page)!!
        help.showHelp()
    }

    @Subcommand("give")
    @CommandPermission("cmdi.give")
    @Syntax("<player> <item> [amount] [KEY=VAL]...")
    @CommandCompletion("@players @itemdefs @nothing @itemparams")
    fun onGive(
        issuer: CommandSender,
        player: OnlinePlayer,
        definition: ItemDefinition,
        @Default("1") amount: Int,
        vararg params: String
    ) {
        val paramMap: MutableMap<String?, String?> = Maps.newHashMap()
        for (param: String in params) {
            val split: Array<String> = param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (split.size != 2) {
                issuer.sendMessage(ChatColor.RED.toString() + "Parameter need to be of the form KEY=VAL")
                return
            }
            paramMap[split[0]] = split[1]
        }
        val item = definition.getItem(paramMap)
        item.amount = amount
        val leftovers: Map<Int, ItemStack> = player.player.inventory.addItem(item)
        for (itemStack: ItemStack? in leftovers.values) player.getPlayer().world.dropItem(
            player.getPlayer().location,
            (itemStack)!!
        )
        issuer.sendMessage(ChatColor.GREEN.toString() + "Successfully gave " + player.player.name + " " + amount + " " + "command items!")
    }

    @Subcommand("reload")
    @CommandPermission("cmdi.reload")
    fun onReload(sender: CommandSender) {
        try {
            plugin.configManager.parse()
            sender.sendMessage(ChatColor.GREEN.toString() + "Successfully reloaded config!")
        } catch (e: RuntimeException) {
            sender.sendMessage(ChatColor.RED.toString() + "Failed to read the configuration:")
            sender.sendMessage(ChatColor.RED.toString() + e.cause!!.message)
        }
    }

    @Subcommand("inspect")
    @CommandPermission("cmdi.inspect")
    fun onInspect(player: Player) {
        val itemInMainHand = player.inventory.itemInMainHand
        if (itemInMainHand.type == Material.AIR) {
            player.sendMessage(ChatColor.RED.toString() + "Please hold a command item in your main hand that you want to inspect!")
            return
        }
        val itemMeta = itemInMainHand.itemMeta
        var command: String
        if (itemMeta == null || (NBTItem(itemInMainHand).getOrCreateCompound("cmdi").getString("command").also {
                command = it
            }) == null) {
            player.sendMessage(ChatColor.RED.toString() + "This is not a command item!")
            return
        }
        val params: Map<String, String> = NBTItem(itemInMainHand).getOrCreateCompound("cmdi").getObject<Map<*, *>>(
            "params",
            MutableMap::class.java
        )
        player.sendMessage(ChatColor.AQUA.toString() + "===========================")
        player.sendMessage(ChatColor.AQUA.toString() + "  Command: " + ChatColor.GOLD + command)
        player.sendMessage(ChatColor.AQUA.toString() + "  Parameters:")
        for (entry: Map.Entry<String, String> in params.entries) player.sendMessage(
            (ChatColor.AQUA.toString() + "  - " + ChatColor.GOLD + entry.key + ChatColor.AQUA
                    + " = " + ChatColor.GOLD + entry.value)
        )
        player.sendMessage(ChatColor.AQUA.toString() + "  Execution Trace:")
        val itemDefinition = plugin.configManager.config.items[command]
        if (itemDefinition == null) player.sendMessage(ChatColor.AQUA.toString() + "  - " + ChatColor.RED + "This item has been disabled!") else {
            val trace: List<ItemDefinition.ExecutionTrace> = itemDefinition.getExecutionTrace()
            for (item: ItemDefinition.ExecutionTrace in trace) player.sendMessage(
                (ChatColor.AQUA.toString() + getDepthPrefix(item.depth)
                        + ChatColor.GOLD + item.label)
            )
        }
        player.sendMessage(ChatColor.AQUA.toString() + "===========================")
    }

    @Subcommand("calc")
    @Syntax("<expression> [<VAR>=<VAL>]...")
    @CommandPermission("cmdi.math")
    fun onCalc(sender: CommandSender, expression: String, vararg args: String) {
        val ast: ActionMathExpr.Expression
        try {
            ast = ActionMathExpr.parse(expression)
        } catch (e: RuntimeException) {
            sender.sendMessage(ChatColor.RED.toString() + "Invalid expression: " + e.message)
            return
        }
        val params: MutableMap<String, Double> = HashMap()
        for (arg: String in args) {
            val split: Array<String> = arg.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (split.size != 2) {
                sender.sendMessage(ChatColor.RED.toString() + "Invalid parameter description, should be <VAR>=<VAL>")
                return
            }
            var x: Double
            try {
                x = split[1].toDouble()
            } catch (e: NumberFormatException) {
                sender.sendMessage(ChatColor.RED.toString() + "Invalid parameter description, <VAL> should be a number")
                return
            }
            params[split[0]] = x
        }
        try {
            sender.sendMessage(
                ChatColor.GREEN.toString() + expression + ChatColor.GRAY + " -> " + ChatColor.GREEN + ast.eval(
                    params
                )
            )
        } catch (e: RuntimeException) {
            sender.sendMessage(ChatColor.RED.toString() + "Evaluation failed: " + e.message)
        }
    }

    companion object {
        private fun getDepthPrefix(depth: Int): String {
            val builder = StringBuilder()
            builder.append("  ")
            for (i in 0 until depth) builder.append("| ")
            builder.append("|-")
            return builder.toString()
        }
    }
}