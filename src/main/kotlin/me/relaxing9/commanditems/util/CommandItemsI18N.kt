package me.relaxing9.commanditems.util

import me.relaxing9.commanditems.CommandItems
import org.bukkit.ChatColor
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.*

class CommandItemsI18N private constructor(private val plugin: CommandItems) {
    enum class MsgKey(private val defaultMessage: String) {
        // Keys with default message
        ITEM_DISABLED("&cThis item has been disabled!"), ITEM_NOPERMISSION("&cYou don't have permission to use this item!"), ITEM_COOLDOWN(
            "&cYou can only use this item once every \$TIME_PERIOD!"
        ),
        ITEM_ERROR("&cSomething went wrong during the execution of your command item, operators have been notified!");

        private val key: String
            private get() = name.lowercase(Locale.getDefault()).replace('_', '.')

        fun get(): String {
            return get(this)
        }

        operator fun get(params: Map<String, String?>): String {
            return CommandItemsI18N[this, params]
        }
    }

    private var messagesConfig: YamlConfiguration? = null

    init {
        reload()
    }

    private fun reload() {
        messagesConfig = YamlConfiguration.loadConfiguration(File(plugin.dataFolder, "messages.yml"))
    }

    fun getMessage(key: MsgKey, params: Map<String, String?>): String {
        var msg = messagesConfig!!.getString(key.getKey())
        if (msg == null) msg = key.getDefaultMessage()
        for ((key1, value): Map.Entry<String, String?> in params) msg = msg.replace("$"+key1,
        value
        )
        return ChatColor.translateAlternateColorCodes('&', msg!!)
    }

    companion object {
        private var instance: CommandItemsI18N? = null
            private set

        fun initialize(plugin: CommandItems) {
            instance = CommandItemsI18N(plugin)
        }

        @JvmOverloads
        operator fun get(key: MsgKey, params: Map<String, String?> = emptyMap<String, String>()): String {
            return instance!!.getMessage(key, params)
        }
    }
}