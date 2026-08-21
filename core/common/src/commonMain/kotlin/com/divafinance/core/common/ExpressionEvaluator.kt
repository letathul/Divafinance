package com.divafinance.core.common

/**
 * Arithmetic for the quick-add keypad, so an amount can be entered as a sum ("12+8.50")
 * or a split ("120/3") instead of being worked out elsewhere first.
 *
 * Deliberately small: `+ - * /` over decimal literals, standard precedence, left
 * associative, optional leading minus. No parentheses — the keypad cannot produce them.
 */
object ExpressionEvaluator {

    /**
     * Evaluates a complete expression, or returns null if it is not one.
     *
     * Null means "do not accept this as an amount": empty, malformed, a dangling operator,
     * or a division by zero. Use this to gate saving.
     */
    fun evaluate(expression: String): Double? {
        val tokens = tokenize(expression) ?: return null
        return evaluateTokens(tokens)
    }

    /**
     * Evaluates the longest leading part of [expression] that forms a complete expression.
     *
     * Mid-typing the input is usually incomplete — "12+" is what the field holds between
     * the operator and the next digit — and a running total that blanked out on every
     * operator press would be useless. "12+" previews as 12, "12+8" as 20.
     */
    fun preview(expression: String): Double? {
        var candidate = expression.trim()
        while (candidate.isNotEmpty()) {
            evaluate(candidate)?.let { return it }
            candidate = candidate.dropLast(1).trimEnd()
        }
        return null
    }

    private sealed interface Token {
        data class Num(val value: Double) : Token
        data class Op(val symbol: Char) : Token
    }

    /** Returns null on any character or arrangement the grammar does not allow. */
    private fun tokenize(expression: String): List<Token>? {
        val source = expression.filterNot { it.isWhitespace() }
        if (source.isEmpty()) return null

        val tokens = mutableListOf<Token>()
        var index = 0

        while (index < source.length) {
            val char = source[index]
            when {
                char.isDigit() || char == '.' -> {
                    val start = index
                    var dots = 0
                    while (index < source.length &&
                        (source[index].isDigit() || source[index] == '.')
                    ) {
                        if (source[index] == '.' && ++dots > 1) return null
                        index++
                    }
                    val literal = source.substring(start, index)
                    // "." alone is not a number, though ".5" and "5." both are.
                    if (literal == ".") return null
                    tokens += Token.Num(literal.toDoubleOrNull() ?: return null)
                }

                char in OPERATORS -> {
                    // A leading minus is a sign, not a binary operator: "-5+2".
                    if (tokens.isEmpty()) {
                        if (char != '-') return null
                        tokens += Token.Num(0.0)
                    } else if (tokens.last() is Token.Op) {
                        // Two operators in a row has no reading here ("5+*3").
                        return null
                    }
                    tokens += Token.Op(char)
                    index++
                }

                else -> return null
            }
        }

        // A trailing operator means the expression is unfinished, not wrong — but it is
        // still not evaluable. preview() is what tolerates this case.
        if (tokens.lastOrNull() is Token.Op) return null
        return tokens
    }

    /**
     * Two-stack shunting-yard. Operands and operators are pushed separately, and an
     * operator of lower-or-equal precedence collapses everything above it first, which is
     * what makes the evaluation left associative ("100-30-20" is 50, not 90).
     */
    private fun evaluateTokens(tokens: List<Token>): Double? {
        val operands = ArrayDeque<Double>()
        val operators = ArrayDeque<Char>()

        for (token in tokens) {
            when (token) {
                is Token.Num -> operands.addLast(token.value)
                is Token.Op -> {
                    while (
                        operators.isNotEmpty() &&
                        precedence(operators.last()) >= precedence(token.symbol)
                    ) {
                        if (!collapse(operands, operators)) return null
                    }
                    operators.addLast(token.symbol)
                }
            }
        }
        while (operators.isNotEmpty()) {
            if (!collapse(operands, operators)) return null
        }

        val result = operands.singleOrNull() ?: return null
        // Overflow or a division that slipped through is not a usable amount.
        return if (result.isFinite()) result else null
    }

    /** Applies the top operator to the top two operands. False if that is not possible. */
    private fun collapse(operands: ArrayDeque<Double>, operators: ArrayDeque<Char>): Boolean {
        val operator = operators.removeLastOrNull() ?: return false
        val right = operands.removeLastOrNull() ?: return false
        val left = operands.removeLastOrNull() ?: return false

        val value = when (operator) {
            '+' -> left + right
            '-' -> left - right
            '*' -> left * right
            // Guarded rather than left to produce Infinity/NaN, so "10/0" is rejected
            // outright instead of surfacing as a nonsense amount.
            '/' -> if (right == 0.0) return false else left / right
            else -> return false
        }
        operands.addLast(value)
        return true
    }

    private fun precedence(operator: Char): Int = when (operator) {
        '*', '/' -> 2
        else -> 1
    }

    private val OPERATORS = charArrayOf('+', '-', '*', '/')
}
