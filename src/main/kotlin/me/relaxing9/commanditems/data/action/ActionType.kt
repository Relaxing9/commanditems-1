package me.relaxing9.commanditems.data.action

/**
 * Created by Yamakaja on 26.05.18.
 */
enum class ActionType(val actionClass: Class<out Action?>) {
    COMMAND(ActionCommand::class.java), MESSAGE(ActionMessage::class.java), REPEAT(
        ActionRepeat::class.java
    ),
    WAIT(ActionWait::class.java), ITER(
        ActionIterate::class.java
    ),
    CALC(ActionCalc::class.java), MATH_EXPR(
        ActionMathExpr::class.java
    );

}