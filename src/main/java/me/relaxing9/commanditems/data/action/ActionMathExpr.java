package me.relaxing9.commanditems.data.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

import com.fasterxml.jackson.annotation.JsonProperty;

import me.relaxing9.commanditems.CommandItems;
import me.relaxing9.commanditems.data.ItemDefinition;
import me.relaxing9.commanditems.interpreter.InterpretationContext;

public class ActionMathExpr extends Action {

    @JsonProperty(required = true)
    private String target;

    @JsonProperty(required = true)
    private String expr;

    @JsonProperty(defaultValue = "false")
    private boolean round;

    @JsonProperty(required = true)
    private Action[] actions;

    private transient Expression ast;
    private static Expression nullValue;

    public ActionMathExpr() {
        super(ActionType.MATH_EXPR);
    }

    @Override
    public void trace(List<ItemDefinition.ExecutionTrace> trace, int depth) {
        String line = String.format("%s = %s%s", target, this.round ? "(rounded) " : "", expr);

        trace.add(new ItemDefinition.ExecutionTrace(depth, line));
        for (Action action : this.actions) action.trace(trace, depth + 1);
    }

    @Override
    public void init() {
        Expression ast = parse(this.expr);
        try {
            this.ast = ast;
            nullValue = ast;

            for (Action action : this.actions) action.init();
        } catch (RuntimeException e) {
            CommandItems.logger.log(Level.SEVERE, "Failed to parse math expression: ", e);
          }
    }

    @FunctionalInterface
    public
    interface Expression {
        double eval(Map<String, Double> params);
    }

    // ====================================================
    // Derived from: https://stackoverflow.com/a/26227947
    // ====================================================
    public static Expression parse(String str) {
        return new Object() {
            int position = -1;
            int currentChar;

            void nextChar() {
                currentChar = (++position < str.length()) ? str.charAt(position) : -1;
            }

            boolean eat(int expectedChar) {
                while (currentChar == ' ') nextChar();
                if (currentChar == expectedChar) {
                    nextChar();
                    return true;
                }
                return false;
            }

            Expression parse() {
                nextChar();
                Expression expression = parseExpression();
                if (position < str.length())
                    CommandItems.logger.log(Level.WARNING, "Unexpected: " + (char) currentChar);
                return expression;
            }

            // Grammar:
            // expression = term ((ADD | SUBTRACT) term)*
            // term = factor ((MULTIPLY | DIVIDE | MODULUS) factor)*
            // factor = (ADD | SUBTRACT)? term ((EXPONENTIATE) factor)?

            Expression parseExpression() {
                Expression expression = parseTerm();
                while (true) {
                    if (eat('+')) {
                        Expression left = expression;
                        Expression right = parseTerm();
                        expression = (params) -> left.eval(params) + right.eval(params);
                    } else if (eat('-')) {
                        Expression left = expression;
                        Expression right = parseTerm();
                        expression = (params) -> left.eval(params) - right.eval(params);
                    } else return expression;
                }
            }

            Expression parseTerm() {
                Expression expression = parseFactor();
                while (true) {
                    if (eat('*')) {
                        Expression left = expression;
                        Expression right = parseFactor();
                        expression = (params) -> left.eval(params) * right.eval(params);
                    } else if (eat('/')) {
                        Expression left = expression;
                        Expression right = parseFactor();
                        expression = (params) -> left.eval(params) / right.eval(params);
                    } else return expression;
                }
            }

            Expression parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) {
                    Expression expression = parseFactor();
                    return (params) -> -expression.eval(params); // unary minus
                }

                Expression expression = nullValue;
                int startPosition = this.position;
                if (eat('(')) { // parentheses
                    expression = parseExpression();
                    eat(')');
                } else if ((currentChar >= '0' && currentChar <= '9') || currentChar == '.') { // numbers
                    while ((currentChar >= '0' && currentChar <= '9') || currentChar == '.')
                        nextChar();
                    double result = Double.parseDouble(str.substring(startPosition, this.position));
                    expression = (params) -> result;
                } else if (Character.isLetter(currentChar) || currentChar == '_') { // symbols. May not start with a number or underscore
                    while (Character.isLetter(currentChar) || currentChar == '_' || Character.isDigit(currentChar))
                        nextChar();
                    String symbolName = str.substring(startPosition, this.position);
                    if (eat('(')) {
                        switch (symbolName) {
                            case "sqrt": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.sqrt(a.eval(params));
                                break;
                            }
                            case "sin": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.sin(a.eval(params));
                                break;
                            }
                            case "asin": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.asin(a.eval(params));
                                break;
                            }
                            case "cos": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.cos(a.eval(params));
                                break;
                            }
                            case "acos": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.acos(a.eval(params));
                                break;
                            }
                            case "tan": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.tan(a.eval(params));
                                break;
                            }
                            case "atan": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.atan(a.eval(params));
                                break;
                            }
                            case "ceil": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.ceil(a.eval(params));
                                break;
                            }
                            case "floor": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.floor(a.eval(params));
                                break;
                            }
                            case "abs": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.abs(a.eval(params));
                                break;
                            }
                            case "exp": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.exp(a.eval(params));
                                break;
                            }
                            case "log": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.log(a.eval(params));
                                break;
                            }
                            case "round": {
                                Expression a = parseExpression();
                                expression = (params) -> Math.round(a.eval(params));
                                break;
                            }
                            case "min": {
                                List<Expression> expressionList = new ArrayList<>();
                                do {
                                    Expression a = parseExpression();
                                    expressionList.add(a);
                                } while (eat(','));
                                expression = (params) -> {
                                    double min = expressionList.get(0).eval(params);
                                    for (int i = 1; i < expressionList.size(); i++) {
                                        double v = expressionList.get(i).eval(params);
                                        if (v < min)
                                            min = v;
                                    }
                                    return min;
                                };
                                break;
                            }
                            case "max": {
                                List<Expression> expressionList = new ArrayList<>();
                                do {
                                    Expression a = parseExpression();
                                    expressionList.add(a);
                                } while (eat(','));
                                expression = (params) -> {
                                    double max = expressionList.get(0).eval(params);
                                    for (int i = 1; i < expressionList.size(); i++) {
                                        double v = expressionList.get(i).eval(params);
                                        if (v > max)
                                            max = v;
                                    }
                                    return max;
                                };
                                break;
                            }
                            case "fmod": {
                                Expression a = parseExpression();
                                if (!eat(','))
                                    CommandItems.logger.log(Level.WARNING, "fmod requires two parameters!");
                                Expression b = parseExpression();

                                expression = (params) -> a.eval(params) % b.eval(params);
                                break;
                            }
                            case "sign": {
                                Expression a = parseExpression();

                                expression = (params) -> Math.signum(a.eval(params));
                                break;
                            }
                            case "rand":
                                expression = (params) -> Math.random();
                                break;

                            case "randn": {
                                Random random = new Random();
                                expression = (params) -> random.nextGaussian();
                                break;
                            }
                            default:
                                CommandItems.logger.log(Level.SEVERE, "Unknown function: " + symbolName);
                        }
                        if (!eat(')'))
                            CommandItems.logger.log(Level.WARNING, "Failed to find closing ')'.");
                    } else {
                        // Variable
                        if ("pi".equals(symbolName))
                            expression = (params) -> Math.PI;

                        else if ("e".equals(symbolName))
                            expression = (params) -> Math.E;

                        else expression = (params) -> {
                                if (!params.containsKey(symbolName))
                                    CommandItems.logger.log(Level.SEVERE, "Tried to access undefined variable: " + symbolName);
                                
                                return params.get(symbolName);
                            };
                    }
                } else {
                    CommandItems.logger.log(Level.WARNING, "Unexpected: " + (char) currentChar);
                }

                final Expression expression1 = expression;
                if (eat('^')) {
                    Expression power = parseFactor();
                    return (params) -> Math.pow(expression1.eval(params), power.eval(params)); // exponentiation
                }

                if (eat('%')) {
                    Expression modulus = parseFactor();
                    return (params) -> expression1.eval(params) % modulus.eval(params); // fmod
                }

                return expression;
            }
        }.parse();
    }
    // ====================================================

    @Override
    public void process(InterpretationContext context) {
        context.pushFrame();

        Map<String, Double> params = new HashMap<>();
        context.forEachNumericLocal(params::put);

        double rval = this.ast.eval(params);
        if (this.round)
            context.pushLocal(this.target, Long.toString(Math.round(rval)));
        else
            context.pushLocal(this.target, String.format("%f", rval));

        for (Action action : this.actions)
            action.process(context);

        context.popFrame();
    }

}
