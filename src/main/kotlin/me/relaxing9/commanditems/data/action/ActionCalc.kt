package me.relaxing9.commanditems.data.action

import com.fasterxml.jackson.annotation.JsonProperty
import me.relaxing9.commanditems.CommandItems
import me.relaxing9.commanditems.data.ItemDefinition.ExecutionTrace
import me.relaxing9.commanditems.interpreter.InterpretationContext
import java.util.logging.Level

/**
 * Created by Yamakaja on 27.05.18.
 */
class ActionCalc : Action(ActionType.CALC) {
    @JsonProperty(required = true, value = "op")
    private val operationType: OperationType? = null

    @JsonProperty(required = true)
    private val a: String? = null

    @JsonProperty(required = true)
    private val b: String? = null

    @JsonProperty
    private val target = "y"

    @JsonProperty(required = true)
    private val actions: Array<Action>
    override fun init() {
        for (action in actions) action.init()
    }

    override fun trace(trace: MutableList<ExecutionTrace>, depth: Int) {
        val line = ExecutionTrace(
            depth, String.format(
                "%s = %s %c %s",
                target, a, operationType!!.op, b
            )
        )
        trace.add(line)
        for (action in actions) action.trace(trace, depth + 1)
    }

    override fun process(context: InterpretationContext) {
        try {
            val a: Int = context.resolveLocalsInString(a).toInt()
            val b: Int = context.resolveLocalsInString(b).toInt()
            context.pushFrame()
            context.pushLocal(target, operationType!!.process(a, b).toString())
            for (action in actions) action.process(context)
            context.popFrame()
        } catch (e: NumberFormatException) {
            CommandItems.logger.log(Level.SEVERE, "Parsing numbers failed: ", e)
        }
    }

    enum class OperationType(val op: Char) {
        @Suppress("unused")
        ADD('+') {
            override fun process(a: Int, b: Int): Int {
                return a + b
            }
        },
        @Suppress("unused")
        SUB('-') {
            override fun process(a: Int, b: Int): Int {
                return a - b
            }
        },
        @Suppress("unused")
        MUL('*') {
            override fun process(a: Int, b: Int): Int {
                return a * b
            }
        },
        @Suppress("unused")
        DIV('/') {
            override fun process(a: Int, b: Int): Int {
                return a / b
            }
        };

        abstract fun process(a: Int, b: Int): Int
    }
}