package me.relaxing9.commanditems.parser

import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import me.relaxing9.commanditems.CommandItems
import me.relaxing9.commanditems.data.CommandItemsConfig
import me.relaxing9.commanditems.data.ItemDefinition
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import java.io.File
import java.io.IOException
import java.util.logging.Level

/**
 * Created by Yamakaja on 26.05.18.
 */
class ConfigManager(private val plugin: CommandItems) {
    var config: CommandItemsConfig? = null
        private set
    private val mapper: YAMLMapper = YAMLMapper()

    init {
        val module = SimpleModule()
        module.addDeserializer(ItemStack::class.java, ItemStackDeserializer())
        mapper.registerModule(module)
    }

    fun parse() {
        try {
            config = mapper.readValue(
                File(plugin.dataFolder, "config.yml"),
                CommandItemsConfig::class.java
            )
        } catch (e: IOException) {
            CommandItems.logger.log(Level.SEVERE, "Failed to read config!", e)
        }
        for ((key, value) in this.config?.items?.entries!!) {
            value.setKey(key)
            try {
                for (action in value.getActions()) action.init()
            } catch (e: RuntimeException) {
                CommandItems.logger.log(Level.SEVERE, "Failed to initialize command item: $key")
                CommandItems.logger.log(Level.SEVERE, "${e.message}")
                Bukkit.getPluginManager().disablePlugin(plugin)
            }
        }
    }
}