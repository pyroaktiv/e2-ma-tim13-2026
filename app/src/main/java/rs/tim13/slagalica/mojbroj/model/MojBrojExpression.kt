package rs.tim13.slagalica.mojbroj.model

/**
 * Gradi i validira matematički izraz igrača u „Moj broj".
 *
 * Pravila nadovezivanja (broj može doći posle operatora/otvorene zagrade, operator posle
 * broja/zatvorene zagrade, itd.) drže se ovde da bi ViewModel i fragment ostali tanki.
 * Svaki od šest ponuđenih brojeva sme se upotrebiti najviše jednom.
 */
class MojBrojExpression {

    private val _tokens = mutableListOf<ExpressionToken>()
    val tokens: List<ExpressionToken> get() = _tokens.toList()

    val display: String get() = _tokens.joinToString(" ") { it.text }
    val isEmpty: Boolean get() = _tokens.isEmpty()
    val usedNumberIndices: Set<Int> get() = _tokens.mapNotNull { it.numberIndex }.toSet()

    private val lastType: TokenType? get() = _tokens.lastOrNull()?.type
    private val openBracketBalance: Int
        get() = _tokens.count { it.type == TokenType.OPEN_BRACKET } -
                _tokens.count { it.type == TokenType.CLOSE_BRACKET }

    private fun canAddNumber(): Boolean =
        lastType == null || lastType == TokenType.OPERATOR || lastType == TokenType.OPEN_BRACKET

    private fun canAddOperator(): Boolean =
        lastType == TokenType.NUMBER || lastType == TokenType.CLOSE_BRACKET

    private fun canAddOpenBracket(): Boolean =
        lastType == null || lastType == TokenType.OPERATOR || lastType == TokenType.OPEN_BRACKET

    private fun canAddCloseBracket(): Boolean =
        (lastType == TokenType.NUMBER || lastType == TokenType.CLOSE_BRACKET) && openBracketBalance > 0

    /** Izraz je kompletan (može se evaluirati) kada su zagrade zatvorene i završava brojem/zagradom. */
    val isComplete: Boolean
        get() = _tokens.isNotEmpty() && openBracketBalance == 0 &&
                (lastType == TokenType.NUMBER || lastType == TokenType.CLOSE_BRACKET)

    fun addNumber(value: Int, numberIndex: Int): Boolean {
        if (!canAddNumber() || numberIndex in usedNumberIndices) return false
        _tokens.add(ExpressionToken(value.toString(), TokenType.NUMBER, numberIndex))
        return true
    }

    fun addOperator(symbol: String): Boolean {
        if (!canAddOperator()) return false
        _tokens.add(ExpressionToken(symbol, TokenType.OPERATOR))
        return true
    }

    fun addOpenBracket(): Boolean {
        if (!canAddOpenBracket()) return false
        _tokens.add(ExpressionToken("(", TokenType.OPEN_BRACKET))
        return true
    }

    fun addCloseBracket(): Boolean {
        if (!canAddCloseBracket()) return false
        _tokens.add(ExpressionToken(")", TokenType.CLOSE_BRACKET))
        return true
    }

    /** Uklanja poslednji element i vraća ga (npr. da fragment ponovo omogući dugme broja). */
    fun removeLast(): ExpressionToken? =
        if (_tokens.isEmpty()) null else _tokens.removeAt(_tokens.lastIndex)

    fun clear() = _tokens.clear()

    /** Vrednost izraza ili null ako nije kompletan / nije validan. */
    fun evaluate(): Int? = if (!isComplete) null else ExpressionEvaluator.evaluate(_tokens)
}
