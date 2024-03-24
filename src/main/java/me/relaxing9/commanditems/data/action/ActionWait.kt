package me.relaxing9.commanditems.data.action

import com.fasterxml.jackson.annotation.JsonProperty
import me.relaxing9.commanditems.data.ItemDefinition.ExecutionTrace
import me.relaxing9.commanditems.interpreter.InterpretationContext
import org.bukkit.scheduler.BukkitRunnable

/**
 * Created by Yamakaja on 26.05.18.
 */
class ActionWait protected constructor() : Action(ActionType.WAIT) {
    @JsonProperty
    private val duration = 20

    @JsonProperty(required = true)
    private val actions: Array<Action>
    override fun init() {
        for (action in actions) action.init()
    }

    override fun trace(trace: MutableList<ExecutionTrace>, depth: Int) {
        val line: String = String.format("Wait for %d ticks:", duration)
        trace.add(ExecutionTrace(depth, line))
        for (action in actions) action.trace(trace, depth + 1)
    }

    override fun process(context: InterpretationContext) {
        val contextClone = context.copy()
        object : BukkitRunnable() {
            override fun run() {
                for (action in actions) action.process(contextClone)
                contextClone.release()
            }
        }.runTaskLater(context.plugin, duration.toLong())
    }
}