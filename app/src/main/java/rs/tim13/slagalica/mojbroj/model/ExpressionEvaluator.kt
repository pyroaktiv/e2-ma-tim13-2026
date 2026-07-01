package rs.tim13.slagalica.mojbroj.model

/**
 * Računa vrednost izraza sastavljenog od [ExpressionToken]-a koristeći celobrojnu
 * aritmetiku. Deljenje mora biti tačno (bez ostatka); u suprotnom, kao i kod deljenja
 * nulom ili nevalidnog izraza, vraća se null.
 */
object ExpressionEvaluator {

    fun evaluate(tokens: List<ExpressionToken>): Int? {
        val rpn = toReversePolish(tokens) ?: return null
        return evaluateRpn(rpn)
    }

    private fun precedence(op: String): Int = when (op) {
        "*", "/" -> 2
        "+", "-" -> 1
        else -> 0
    }

    /** Shunting-yard: infiks -> postfiks. Vraća null ako su zagrade neuravnotežene. */
    private fun toReversePolish(tokens: List<ExpressionToken>): List<ExpressionToken>? {
        val output = mutableListOf<ExpressionToken>()
        val operators = ArrayDeque<ExpressionToken>()

        for (token in tokens) {
            when (token.type) {
                TokenType.NUMBER -> output.add(token)
                TokenType.OPERATOR -> {
                    while (operators.isNotEmpty() &&
                        operators.last().type == TokenType.OPERATOR &&
                        precedence(operators.last().text) >= precedence(token.text)
                    ) {
                        output.add(operators.removeLast())
                    }
                    operators.addLast(token)
                }
                TokenType.OPEN_BRACKET -> operators.addLast(token)
                TokenType.CLOSE_BRACKET -> {
                    while (operators.isNotEmpty() && operators.last().type != TokenType.OPEN_BRACKET) {
                        output.add(operators.removeLast())
                    }
                    if (operators.isEmpty()) return null // nedostaje '('
                    operators.removeLast() // ukloni '('
                }
            }
        }

        while (operators.isNotEmpty()) {
            val op = operators.removeLast()
            if (op.type == TokenType.OPEN_BRACKET) return null // višak '('
            output.add(op)
        }
        return output
    }

    private fun evaluateRpn(rpn: List<ExpressionToken>): Int? {
        val stack = ArrayDeque<Int>()
        for (token in rpn) {
            when (token.type) {
                TokenType.NUMBER -> stack.addLast(token.text.toIntOrNull() ?: return null)
                TokenType.OPERATOR -> {
                    if (stack.size < 2) return null
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    val result = when (token.text) {
                        "+" -> a + b
                        "-" -> a - b
                        "*" -> a * b
                        "/" -> if (b == 0 || a % b != 0) return null else a / b
                        else -> return null
                    }
                    stack.addLast(result)
                }
                else -> return null
            }
        }
        return if (stack.size == 1) stack.last() else null
    }
}
