package me.relaxing9.commanditems.interpreter

import me.relaxing9.commanditems.CommandItems
import me.relaxing9.commanditems.data.ItemDefinition
import org.bukkit.entity.Player

/**
 * Created by Yamakaja on 26.05.18.
 */
class ItemExecutor(private val plugin: CommandItems) {
    fun processInteraction(player: Player, definition: ItemDefinition, params: Map<String?, String?>) {
        val context = InterpretationContext(plugin, player)
        context.pushFrame()
        context.pushLocal("player", player.name)
        context.pushLocal("uuid", player.uniqueId.toString())
        context.pushLocal("x", player.location.x.toString())
        context.pushLocal("y", player.location.y.toString())
        context.pushLocal("z", player.location.z.toString())
        context.pushLocal("yaw", player.location.yaw.toString())
        context.pushLocal("pitch", player.location.pitch.toString())
        context.pushLocal("food", player.foodLevel.toString())
        context.pushLocal("health", player.health.toString())
        if (definition.parameters != null) for ((key, value) in definition.parameters.entries) context.pushLocal(
            key, params.getOrDefault(key, value))
        for (action in definition.getActions()) action.process(context)
        context.popFrame()
    }
}