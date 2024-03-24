package me.relaxing9.commanditems

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Maps
import com.google.common.collect.Table
import de.tr7zw.changeme.nbtapi.NBTItem
import me.relaxing9.commanditems.data.ItemDefinition
import me.relaxing9.commanditems.util.CommandItemsI18N
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.logging.Level

class CommandItemManager(private val plugin: CommandItems) : Listener {
    var iter = 0
    private val lastUse: Table<UUID, String, Long> = HashBasedTable.create()

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    private fun checkCooldown(player: Player, command: String, duration: Long): Boolean {
        var lastUse: Long = 0
        if (this.lastUse.contains(player.uniqueId, command)) lastUse = this.lastUse[player.uniqueId, command]
        if (System.currentTimeMillis() < lastUse + duration * 1000) return false
        this.lastUse.put(player.uniqueId, command, System.currentTimeMillis())
        return true
    }

    private fun getSecondsUntilNextUse(player: Player, command: String, duration: Long): Long {
        var lastUse: Long = 0
        if (this.lastUse.contains(player.uniqueId, command)) lastUse = this.lastUse[player.uniqueId, command]
        val difference = lastUse + duration * 1000 - System.currentTimeMillis()
        return if (difference < 0) 0 else Math.ceil(difference / 1000.0).toLong()
    }

    @EventHandler
    fun onInteract(event: PlayerInteractEvent) {
        if (!isValidInteraction(event)) {
            return
        }
        if (event.item == null) {
            return
        }
        val itemMeta = event.item!!.itemMeta ?: return
        val command = NBTItem(event.item).getOrCreateCompound("cmdi").getString("command")
        if (command == null || command.isEmpty()) {
            return
        }
        val itemDefinition = plugin.configManager.config.items[command]
        if (itemDefinition == null) {
            event.player.sendMessage(CommandItemsI18N.MsgKey.ITEM_DISABLED.get())
            return
        }
        event.isCancelled = true
        if (!isValidPlayer(event.player, itemDefinition, command)) {
            return
        }
        val params: Map<String, String> = NBTItem(event.item).getOrCreateCompound("cmdi").getObject<Map<*, *>>(
            "params",
            MutableMap::class.java
        )
        if (itemDefinition.isConsumed()) {
            val contents = runConsume(event)
            event.player.inventory.setItem(iter, contents)
        }
        try {
            plugin.executor.processInteraction(event.player, itemDefinition, params)
        } catch (e: RuntimeException) {
            CommandItems.logger.log(Level.SEVERE, "Failed to process command item: $command")
            event.player.sendMessage(CommandItemsI18N.MsgKey.ITEM_ERROR.get())
            e.printStackTrace()
        }
    }

    private fun isValidInteraction(event: PlayerInteractEvent): Boolean {
        return event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK
    }

    private fun isValidPlayer(player: Player, itemDefinition: ItemDefinition, command: String): Boolean {
        if (itemDefinition.isSneaking() && !player.isSneaking) {
            return false
        }
        if (!player.hasPermission("cmdi.item.$command")) {
            player.sendMessage(CommandItemsI18N.MsgKey.ITEM_NOPERMISSION.get())
            return false
        }
        if (!checkCooldown(player, command, itemDefinition.getCooldown())) {
            val params: MutableMap<String, String?> = Maps.newHashMap()
            params.put("TIME_PERIOD", getTimeString(itemDefinition.getCooldown()))
            params.put(
                "TIME_REMAINING",
                getTimeString(getSecondsUntilNextUse(player, command, itemDefinition.getCooldown()))
            )
            player.sendMessage(CommandItemsI18N.MsgKey.ITEM_COOLDOWN[params])
            return false
        }
        return true
    }

    fun runConsume(event: PlayerInteractEvent): ItemStack? {
        val contents: Array<ItemStack?> = event.player.inventory.contents
        var i: Int
        i = 0
        while (i < contents.size) {
            if (contents[i] != null && contents[i]!!.isSimilar(event.item)) {
                val amount = contents[i]!!.amount
                if (amount == 1) contents[i] = null else contents[i]!!.amount = amount - 1
                break
            }
            i++
        }
        iter = i
        return contents[i]
    }

    companion object {
        private fun getTimeString(d: Long): String {
            var duration = d
            val seconds = (duration % 60).toInt()
            duration /= 60
            val minutes = (duration % 60).toInt()
            duration /= 60
            val hours = (duration % 60).toInt()
            duration /= 24
            val days = duration.toInt()
            val builder = StringBuilder()
            if (days != 0) {
                builder.append(days)
                builder.append('d')
            }
            if (hours != 0) {
                if (builder.length > 0) builder.append(' ')
                builder.append(hours)
                builder.append('h')
            }
            if (minutes != 0) {
                if (builder.length > 0) builder.append(' ')
                builder.append(minutes)
                builder.append('m')
            }
            if (seconds != 0) {
                if (builder.length > 0) builder.append(' ')
                builder.append(seconds)
                builder.append('s')
            }
            return builder.toString()
        }
    }
}