package me.relaxing9.commanditems

import co.aikar.commands.*
import me.relaxing9.commanditems.data.ItemDefinition
import me.relaxing9.commanditems.interpreter.ItemExecutor
import me.relaxing9.commanditems.parser.ConfigManager
import me.relaxing9.commanditems.util.CMDIGlow
import me.relaxing9.commanditems.util.CommandItemsI18N
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import java.util.function.Function
import java.util.function.Predicate
import java.util.logging.Logger
import java.util.stream.Collectors

/**
 * Created by Yamakaja on 07.06.17.
 */
class CommandItems : JavaPlugin() {
    var configManager: ConfigManager? = null
        private set
    var executor: ItemExecutor? = null
        private set
    private var commandItemManager: CommandItemManager? = null

    override fun onEnable() {
        Metrics(this, 1002)
        val debug = System.getProperty("me.yamakaja.debug") != null
        saveResource("config.yml", debug)
        saveResource("messages.yml", debug)
        CommandItemsI18N.initialize(this)
        configManager = ConfigManager(this)
        configManager!!.parse()
        val commandManager: BukkitCommandManager = PaperCommandManager(this)
        commandManager.commandContexts.registerContext(
            ItemDefinition::class.java
        ) { context: BukkitCommandExecutionContext ->
            val itemDef: ItemDefinition =
                configManager.getConfig().getItems().get(context.popFirstArg())
                    ?: throw InvalidCommandArgument("Unknown item definition!")
            itemDef
        }
        commandManager.commandCompletions.registerCompletion(
            "itemdefs"
        ) { context: BukkitCommandCompletionContext ->
            configManager.getConfig().getItems().keys.stream()
                .filter(Predicate { key: String ->
                    key.lowercase(Locale.getDefault()).startsWith(context.input.lowercase(Locale.getDefault()))
                })
                .collect<List<String>, Any>(Collectors.toList<String>())
        }
        commandManager.commandCompletions.registerCompletion(
            "itemparams"
        ) { context: BukkitCommandCompletionContext ->
            val itemDefinition =
                context.getContextValue(
                    ItemDefinition::class.java
                )
            itemDefinition.getParameters().entries.stream()
                .filter(Predicate { (key): Map.Entry<String, String?> ->
                    key.lowercase(
                        Locale.getDefault()
                    ).startsWith(context.input)
                })
                .map<String>(Function<Map.Entry<String, String>, String> { (key, value): Map.Entry<String, String> -> "$key=$value" })
                .collect<List<String>, Any>(Collectors.toList<String>())
        }
        commandManager.registerCommand(CommandCMDI(this))
        commandManager.enableUnstableAPI("help")
        executor = ItemExecutor(this)
        commandItemManager = CommandItemManager(this)
        CMDIGlow.getGlow()
    }

    companion object {
        val logger: Logger = Logger.getLogger("CommandItems")
    }
}