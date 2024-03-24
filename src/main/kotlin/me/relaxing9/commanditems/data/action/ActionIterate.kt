package me.relaxing9.commanditems.data.action

import com.fasterxml.jackson.annotation.JsonProperty
import me.relaxing9.commanditems.data.ItemDefinition.ExecutionTrace
import me.relaxing9.commanditems.interpreter.InterpretationContext
import org.bukkit.Bukkit
import org.bukkit.entity.Player

/**
 * Created by Yamakaja on 27.05.18.
 */
class ActionIterate protected constructor() : Action(ActionType.ITER) {
    @JsonProperty
    private val perm: String? = null

    @JsonProperty(required = true)
    private val actions: Array<Action>

    @JsonProperty(value = "what")
    private val target: IterationTarget? = null
    override fun init() {
        for (action in actions) action.init()
    }

    override fun trace(trace: MutableList<ExecutionTrace>, depth: Int) {
        val line: String
        line = if (perm == null) "for (all online players)" else String.format("for (all online players with permission %s)", perm)
        trace.add(ExecutionTrace(depth, line))
        for (action in actions) action.trace(trace, depth + 1)
    }

    override fun process(context: InterpretationContext) {
        context.pushFrame()
        target!!.process(this, context)
        context.popFrame()
    }

    enum class IterationTarget {
        ONLINE_PLAYERS {
            override fun process(action: ActionIterate, context: InterpretationContext) {
                Bukkit.getOnlinePlayers().stream()
                    .filter { player: Player? -> action.perm == null || player!!.hasPermission(action.perm) }
                    .forEach { player: Player? ->
                        context.pushLocal("iter_locX", player!!.location.blockX.toString())
                        context.pushLocal("iter_locY", player.location.blockY.toString())
                        context.pushLocal("iter_locZ", player.location.blockZ.toString())
                        context.pushLocal("iter_name", player.name)
                        context.pushLocal("iter_displayname", player.displayName)
                        context.pushLocal("iter_uuid", player.uniqueId.toString())
                        context.pushLocal("iter_health", player.health.toInt().toString())
                        context.pushLocal("iter_level", player.level.toString())
                        context.pushLocal("iter_food", player.foodLevel.toString())
                        for (subAction in action.actions) subAction.process(
                            context
                        )
                    }
            }
        };

        abstract fun process(action: ActionIterate?, context: InterpretationContext?)
    }
}