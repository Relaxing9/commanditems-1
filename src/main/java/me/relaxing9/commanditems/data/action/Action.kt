package me.relaxing9.commanditems.data.action

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import me.relaxing9.commanditems.data.ItemDefinition.ExecutionTrace
import me.relaxing9.commanditems.interpreter.InterpretationContext

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "action", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = ActionCommand::class, name = "COMMAND"),
    JsonSubTypes.Type(value = ActionMessage::class, name = "MESSAGE"),
    JsonSubTypes.Type(value = ActionRepeat::class, name = "REPEAT"),
    JsonSubTypes.Type(value = ActionWait::class, name = "WAIT"),
    JsonSubTypes.Type(value = ActionIterate::class, name = "ITER"),
    JsonSubTypes.Type(value = ActionCalc::class, name = "CALC"),
    JsonSubTypes.Type(value = ActionMathExpr::class, name = "MATH_EXPR")
)
abstract class Action protected constructor(@field:JsonProperty("action") val type: ActionType) {

    abstract fun process(context: InterpretationContext?)
    fun init() {
        // This is used in other Action Commands
    }

    abstract fun trace(trace: List<ExecutionTrace?>?, depth: Int)
}