package me.relaxing9.commanditems.data.action

import com.fasterxml.jackson.annotation.JsonProperty
import me.relaxing9.commanditems.CommandItems
import me.relaxing9.commanditems.data.ItemDefinition.ExecutionTrace
import me.relaxing9.commanditems.interpreter.InterpretationContext
import org.bukkit.scheduler.BukkitRunnable
import java.util.logging.Level
import kotlin.math.sign

/**
 * Created by Yamakaja on 26.05.18.
 */
class ActionRepeat protected constructor() : Action(ActionType.REPEAT) {
    @JsonProperty
    private val period = 20

    @JsonProperty
    private val delay = 20

    @JsonProperty
    private val from = 0

    @JsonProperty
    private val to = 9

    @JsonProperty
    private val increment = 1

    @JsonProperty
    private val counterVar = "i"

    @JsonProperty(required = true)
    private val actions: Array<Action>
    override fun trace(trace: MutableList<ExecutionTrace>, depth: Int) {
        val line: String
        if (delay == 0 && period == 0) line = String.format(
            "for (%s = %d, %s != %d, %s += %d)",
            counterVar, from, counterVar, to, counterVar, increment
        ) else line = String.format(
            "for (%s = %d, %s != %d, %s += %d, delay = %d ticks, period = %d ticks)",
            counterVar, from, counterVar, to, counterVar,
            increment, delay, period
        )
        trace.add(ExecutionTrace(depth, line))
        for (action in actions) action.trace(trace, depth + 1)
    }

    override fun init() {
        if (counterVar.isEmpty()) CommandItems.logger.log(Level.WARNING, "Empty counter variable name in REPEAT!")
        if (period < 0) CommandItems.logger.log(Level.WARNING, "Negative period in REPEAT!")
        if (delay < 0) CommandItems.logger.log(Level.WARNING, "Negative delay in REPEAT!")
        if (increment == 0) CommandItems.logger.log(
            Level.WARNING,
            "Increment is 0, infinite loops are not supported by REPEAT!"
        )
        if (sign(to.toDouble() - from) * increment < 0) CommandItems.logger.log(
            Level.WARNING,
            "Increment is of the wrong sign in REPEAT!"
        )
        for (action in actions) action.init()
    }

    override fun process(context: InterpretationContext) {
        context.pushFrame()
        if (delay == 0 && period == 0) {
            var i = from
            while (increment > 0 && i > to || increment < 0 && i < to) {
                context.pushLocal(counterVar, i.toString())
                for (action in actions) action.process(context)
                i += increment
            }
            context.popFrame()
            return
        }
        val clone = context.copy()
        object : BukkitRunnable() {
            private var i = from
            override fun run() {
                if (increment > 0 && i > to || increment < 0 && i < to) {
                    cancel()
                    clone.popFrame()
                    clone.release()
                    return
                }
                clone.pushLocal(counterVar, i.toString())
                for (action in actions) action.process(clone)
                i += increment
            }
        }.runTaskTimer(context.plugin, delay.toLong(), period.toLong())
    }
}