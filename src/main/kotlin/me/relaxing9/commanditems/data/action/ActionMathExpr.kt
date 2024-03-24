package me.relaxing9.commanditems.data.action

import com.fasterxml.jackson.annotation.JsonProperty
import me.relaxing9.commanditems.CommandItems
import me.relaxing9.commanditems.data.ItemDefinition
import me.relaxing9.commanditems.data.action.ActionMathExpr.Expression
import me.relaxing9.commanditems.interpreter.InterpretationContext
import java.util.*
import java.util.logging.Level
import kotlin.math.*

class ActionMathExpr : Action(ActionType.MATH_EXPR) {
    @JsonProperty(required = true)
    private val target: String? = null

    @JsonProperty(required = true)
    private val expr: String? = null

    @JsonProperty(defaultValue = "false")
    private val round = false

    @JsonProperty(required = true)
    private val actions: Array<Action>

    @kotlin.jvm.Transient
    private var ast: Expression? = null
    override fun trace(trace: MutableList<ItemDefinition.ExecutionTrace>, depth: Int) {
        val line: String = String.format("%s = %s%s", target, if (round) "(rounded) " else "", expr)
        trace.add(ItemDefinition.ExecutionTrace(depth, line))
        for (action in actions) action.trace(trace, depth + 1)
    }

    override fun init() {
        val ast = parse(expr)
        try {
            this.ast = ast
            nullValue = ast
            for (action in actions) action.init()
        } catch (e: RuntimeException) {
            CommandItems.logger.log(Level.SEVERE, "Failed to parse math expression: ", e)
        }
    }

    fun interface Expression {
        fun eval(params: Map<String?, Double?>?): Double
    }

    // ====================================================
    override fun process(context: InterpretationContext) {
        context.pushFrame()
        val params: MutableMap<String?, Double?> = HashMap()
        context.forEachNumericLocal { key: String?, value: Double? ->
            params[key] = value
        }
        val rval = ast!!.eval(params)
        if (round) context.pushLocal(target, rval.roundToLong().toString()) else context.pushLocal(
            target, String.format("%f", rval)
        )
        for (action in actions) action.process(context)
        context.popFrame()
    }

    companion object {
        private var nullValue: Expression? = null

        // ====================================================
        // Derived from: https://stackoverflow.com/a/26227947
        // ====================================================
        fun parse(str: String?): Expression {
            return object : Any() {
                var pos = -1
                var ch = 0
                fun nextChar() {
                    ch = if (++pos < str!!.length) str[pos].code else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Expression {
                    nextChar()
                    val x = parseExpression()
                    if (pos < str!!.length) CommandItems.logger.log(
                        Level.WARNING,
                        "Unexpected: " + ch.toChar()
                    )
                    return x
                }

                // Grammar:
                // expression = term | expression `+` term | expression `-` term
                // term = factor | term `*` factor | term `/` factor
                // factor = `+` factor | `-` factor | `(` expression `)`
                //        | number | functionName factor | factor `^` factor
                fun parseExpression(): Expression {
                    val x = parseTerm()
                    while (true) {
                        if (eat('+'.code)) {
                            val a: Expression = me.relaxing9.commanditems.data.action.x
                            val b = parseTerm()
                            me.relaxing9.commanditems.data.action.x =
                                Expression { params: Map<String?, Double?>? ->
                                    me.relaxing9.commanditems.data.action.a.eval(
                                        params
                                    ) + me.relaxing9.commanditems.data.action.b.eval(params)
                                } // addition
                        } else if (eat('-'.code)) {
                            val a: Expression = me.relaxing9.commanditems.data.action.x
                            val b = parseTerm()
                            me.relaxing9.commanditems.data.action.x =
                                Expression { params: Map<String?, Double?>? ->
                                    me.relaxing9.commanditems.data.action.a.eval(
                                        params
                                    ) - me.relaxing9.commanditems.data.action.b.eval(params)
                                } // subtraction
                        } else return me.relaxing9.commanditems.data.action.x
                    }
                }

                fun parseTerm(): Expression {
                    val x = parseFactor()
                    while (true) {
                        if (eat('*'.code)) {
                            val a: Expression = me.relaxing9.commanditems.data.action.x
                            val b = parseFactor()
                            me.relaxing9.commanditems.data.action.x =
                                Expression { params: Map<String?, Double?>? ->
                                    me.relaxing9.commanditems.data.action.a.eval(
                                        params
                                    ) * me.relaxing9.commanditems.data.action.b.eval(params)
                                }
                        } else if (eat('/'.code)) {
                            val a: Expression = me.relaxing9.commanditems.data.action.x
                            val b = parseFactor()
                            me.relaxing9.commanditems.data.action.x =
                                Expression { params: Map<String?, Double?>? ->
                                    me.relaxing9.commanditems.data.action.a.eval(
                                        params
                                    ) / me.relaxing9.commanditems.data.action.b.eval(params)
                                }
                        } else return me.relaxing9.commanditems.data.action.x
                    }
                }

                fun parseFactor(): Expression {
                    if (eat('+'.code)) return parseFactor() // unary plus
                    if (eat('-'.code)) {
                        val x = parseFactor()
                        return Expression { params: Map<String?, Double?>? ->
                            -me.relaxing9.commanditems.data.action.x.eval(
                                params
                            )
                        } // unary minus
                    }
                    val x = nullValue
                    val startPos = pos
                    if (eat('('.code)) { // parentheses
                        me.relaxing9.commanditems.data.action.x = parseExpression()
                        eat(')'.code)
                    } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) { // numbers
                        while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                        val res: Double = str.substring(me.relaxing9.commanditems.data.action.startPos, pos).toDouble()
                        me.relaxing9.commanditems.data.action.x =
                            Expression { params: Map<String?, Double?>? -> me.relaxing9.commanditems.data.action.res }
                    } else if (ch >= 'a'.code && ch <= 'z'.code || ch >= 'A'.code && ch <= 'Z'.code) { // symbols. May not start with a number or underscore
                        while (ch >= 'a'.code && ch <= 'z'.code || ch >= 'A'.code && ch <= 'Z'.code || ch >= '0'.code && ch <= '9'.code || ch == '_'.code) nextChar()
                        val symbolName: String = str.substring(
                            me.relaxing9.commanditems.data.action.startPos,
                            pos
                        )
                        if (eat('('.code)) {
                            when (me.relaxing9.commanditems.data.action.symbolName) {
                                "sqrt" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            sqrt(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "sin" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            sin(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "asin" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            asin(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "cos" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            cos(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "acos" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            acos(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "tan" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            tan(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "atan" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            atan(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "ceil" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            ceil(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "floor" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            floor(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "abs" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            abs(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "exp" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            exp(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "log" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            ln(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "round" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            a.eval(params).roundToLong()
                                                .toDouble()
                                        }
                                }

                                "min" -> {
                                    val expressionList: MutableList<Expression> = ArrayList()
                                    do {
                                        val a = parseExpression()
                                        expressionList.add(a)
                                    } while (eat(','.code))
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            var min = expressionList[0].eval(params)
                                            var i = 1
                                            while (i < expressionList.size) {
                                                val v = expressionList[i].eval(params)
                                                if (v < min) min = v
                                                i++
                                            }
                                            min
                                        }
                                }

                                "max" -> {
                                    val expressionList: MutableList<Expression> = ArrayList()
                                    do {
                                        val a = parseExpression()
                                        expressionList.add(a)
                                    } while (eat(','.code))
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            var max = expressionList[0].eval(params)
                                            var i = 1
                                            while (i < expressionList.size) {
                                                val v = expressionList[i].eval(params)
                                                if (v > max) max = v
                                                i++
                                            }
                                            max
                                        }
                                }

                                "fmod" -> {
                                    val a = parseExpression()
                                    if (!eat(','.code)) CommandItems.logger.log(
                                        Level.WARNING,
                                        "fmod requires two parameters!"
                                    )
                                    val b = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            a.eval(
                                                params
                                            ) % b.eval(params)
                                        }
                                }

                                "sign" -> {
                                    val a = parseExpression()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? ->
                                            sign(
                                                a.eval(params)
                                            )
                                        }
                                }

                                "rand" -> me.relaxing9.commanditems.data.action.x =
                                    Expression { params: Map<String?, Double?>? -> Math.random() }

                                "randn" -> {
                                    val random = Random()
                                    me.relaxing9.commanditems.data.action.x =
                                        Expression { params: Map<String?, Double?>? -> random.nextGaussian() }
                                }

                                else -> CommandItems.logger.log(
                                    Level.SEVERE,
                                    "Unknown function: " + me.relaxing9.commanditems.data.action.symbolName
                                )
                            }
                            if (!eat(')'.code)) CommandItems.logger.log(Level.WARNING, "Failed to find closing ')'.")
                        } else {
                            // Variable
                            if ("pi" == me.relaxing9.commanditems.data.action.symbolName) me.relaxing9.commanditems.data.action.x =
                                Expression { params: Map<String?, Double?>? -> Math.PI } else if ("e" == me.relaxing9.commanditems.data.action.symbolName) me.relaxing9.commanditems.data.action.x =
                                Expression { params: Map<String?, Double?>? -> Math.E } else me.relaxing9.commanditems.data.action.x =
                                Expression { params: Map<String?, Double?> ->
                                    if (!params.containsKey(me.relaxing9.commanditems.data.action.symbolName)) CommandItems.logger.log(
                                        Level.SEVERE,
                                        "Tried to access undefined variable: " + me.relaxing9.commanditems.data.action.symbolName
                                    )
                                    params[me.relaxing9.commanditems.data.action.symbolName]!!
                                }
                        }
                    } else {
                        CommandItems.logger.log(
                            Level.WARNING,
                            "Unexpected: " + ch.toChar()
                        )
                    }
                    val x1: Expression = me.relaxing9.commanditems.data.action.x
                    if (eat('^'.code)) {
                        val p = parseFactor()
                        return Expression { params: Map<String?, Double?>? ->
                            me.relaxing9.commanditems.data.action.x1.eval(params)
                                .pow(me.relaxing9.commanditems.data.action.p.eval(params))
                        } // exponentiation
                    }
                    if (eat('%'.code)) {
                        val m = parseFactor()
                        return Expression { params: Map<String?, Double?>? ->
                            me.relaxing9.commanditems.data.action.x1.eval(
                                params
                            ) % me.relaxing9.commanditems.data.action.m.eval(params)
                        } // fmod
                    }
                    return me.relaxing9.commanditems.data.action.x
                }
            }.parse()
        }
    }
}