package com.divafinance.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ExpressionEvaluatorTest {

    private fun eval(expression: String) = ExpressionEvaluator.evaluate(expression)

    // --- typing rules ------------------------------------------------------

    /** Types [keys] one at a time, the way the keypad does. */
    private fun type(keys: String, from: String = "") =
        keys.fold(from) { text, key -> ExpressionEvaluator.append(text, key) }

    @Test
    fun typingBuildsAnExpression() {
        assertEquals("12+8.50", type("12+8.50"))
        assertEquals("(2+3)*4", type("(2+3)*4"))
    }

    /** Reaching for a second operator is a correction; "12+*" has no reading at all. */
    @Test
    fun anOperatorReplacesADanglingOne() {
        assertEquals("12*", type("12+*"))
        assertEquals("12/", type("12+-*/"))
    }

    @Test
    fun anOperatorNeedsSomethingToOperateOn() {
        assertEquals("", type("+"))
        assertEquals("", type("*"))
        assertEquals("(", type("(+"))
    }

    /** The one operator the grammar reads as a sign rather than an operation. */
    @Test
    fun aLeadingMinusIsAllowedThrough() {
        assertEquals("-5", type("-5"))
        assertEquals("(-5+2)", type("(-5+2)"))
    }

    @Test
    fun aNumberTakesOnlyOneDecimalPoint() {
        assertEquals("1.5", type("1.5."))
        assertEquals("1.5+2.5", type("1.5.+2.5."))
    }

    /** The grammar rejects a bare ".", so the keypad never produces one. */
    @Test
    fun aLeadingPointBecomesZeroPoint() {
        assertEquals("0.5", type(".5"))
        assertEquals("2+0.5", type("2+.5"))
        assertEquals("(0.", type("(."))
    }

    @Test
    fun aLeadingZeroIsReplacedRatherThanStacked() {
        assertEquals("5", type("05"))
        assertEquals("0.5", type("0.5"))
        assertEquals("2+5", type("2+05"))
    }

    /** Money. A third decimal would only be rounded away by roundToCents() on save. */
    @Test
    fun aTypedAmountStopsAtTwoDecimals() {
        assertEquals("1.55", type("1.555"))
        assertEquals("1.55+2", type("1.5555+2"))
        assertEquals("1555", type("1555"))
    }

    /** "(3+4)5" is multiplication everywhere it is written; the grammar only had the other way. */
    @Test
    fun aDigitAfterAGroupMultiplies() {
        assertEquals("(3+4)*5", type("(3+4)5"))
        assertEquals(35.0, eval(type("(3+4)5")))
    }

    @Test
    fun aCloseBracketNeedsAnOpenGroupWithSomethingInIt() {
        assertEquals("12", type("12)"))
        assertEquals("(12", type("()12"))
        assertEquals("(12+", type("(12+)"))
        assertEquals("(12+3)", type("(12+3)"))
    }

    /** Whatever is typed, the result is something evaluate() or preview() can read. */
    @Test
    fun typingNeverBuildsAnUnreadableExpression() {
        val keys = "+*.).5+-/(7..8))*3+"
        var text = ""
        for (key in keys) {
            text = ExpressionEvaluator.append(text, key)
            if (text.isNotEmpty()) assertNotNull(ExpressionEvaluator.preview(text), text)
        }
    }

    // --- plain numbers -----------------------------------------------------

    @Test
    fun evaluatesABareNumber() {
        assertEquals(5.0, eval("5"))
        assertEquals(12.75, eval("12.75"))
    }

    @Test
    fun acceptsLeadingAndTrailingDecimalPoints() {
        assertEquals(0.5, eval(".5"))
        assertEquals(5.0, eval("5."))
    }

    @Test
    fun ignoresWhitespace() {
        assertEquals(8.0, eval(" 5 + 3 "))
    }

    // --- arithmetic --------------------------------------------------------

    @Test
    fun addsAndSubtracts() {
        assertEquals(8.0, eval("5+3"))
        assertEquals(2.0, eval("5-3"))
    }

    @Test
    fun multipliesAndDivides() {
        assertEquals(15.0, eval("5*3"))
        assertEquals(2.5, eval("10/4"))
    }

    /** The bill-splitting case the keypad exists for. */
    @Test
    fun splitsABill() {
        assertEquals(40.0, eval("120/3"))
    }

    @Test
    fun appliesMultiplicationBeforeAddition() {
        assertEquals(14.0, eval("2+3*4"))
        assertEquals(10.0, eval("2*3+4"))
        assertEquals(7.0, eval("1+2*3"))
    }

    @Test
    fun appliesDivisionBeforeSubtraction() {
        assertEquals(8.0, eval("10-4/2"))
    }

    /** Left associative: 100-30-20 is 50, not 90. */
    @Test
    fun subtractionIsLeftAssociative() {
        assertEquals(50.0, eval("100-30-20"))
    }

    @Test
    fun divisionIsLeftAssociative() {
        assertEquals(2.0, eval("20/2/5"))
    }

    @Test
    fun handlesALongerChain() {
        assertEquals(23.5, eval("10+2*5+3.5"))
    }

    @Test
    fun supportsALeadingMinus() {
        assertEquals(5.0, eval("-5+10"))
        assertEquals(-8.0, eval("-5-3"))
    }

    // --- rejection ---------------------------------------------------------

    @Test
    fun rejectsEmptyInput() {
        assertNull(eval(""))
        assertNull(eval("   "))
    }

    @Test
    fun rejectsADanglingOperator() {
        assertNull(eval("5+"))
        assertNull(eval("5*"))
    }

    @Test
    fun rejectsRepeatedOperators() {
        assertNull(eval("5++3"))
        assertNull(eval("5+*3"))
    }

    @Test
    fun rejectsALoneDecimalPoint() {
        assertNull(eval("."))
    }

    @Test
    fun rejectsMalformedNumbers() {
        assertNull(eval("1.2.3"))
    }

    @Test
    fun rejectsUnknownCharacters() {
        assertNull(eval("5%3"))
        assertNull(eval("abc"))
    }

    // --- grouping -----------------------------------------------------------

    @Test
    fun groupingBeatsPrecedence() {
        assertEquals(20.0, eval("(2+3)*4"))
        assertEquals(14.0, eval("2+3*4"))
    }

    @Test
    fun groupsNest() {
        assertEquals(27.0, eval("3*((1+2)*3)"))
    }

    @Test
    fun aGroupCanOpenWithASign() {
        assertEquals(2.0, eval("(-3+5)"))
    }

    /** "2(3+4)" is multiplication everywhere else it is written. */
    @Test
    fun aGroupNextToANumberMultiplies() {
        assertEquals(14.0, eval("2(3+4)"))
        assertEquals(24.0, eval("(2+2)(3+3)"))
    }

    @Test
    fun rejectsUnbalancedOrEmptyGroups() {
        assertNull(eval("(2+3"))
        assertNull(eval("2+3)"))
        assertNull(eval("()"))
        assertNull(eval("(+)"))
    }

    /**
     * A group still being typed is unfinished, not wrong: collapsing all the way back to
     * the operand before it would make the running total useless mid-bracket.
     */
    @Test
    fun previewsAnUnclosedGroup() {
        assertEquals(84.0, ExpressionEvaluator.preview("12*(3+4"))
        assertEquals(12.0, ExpressionEvaluator.preview("12*("))
    }

    @Test
    fun rejectsALeadingBinaryOperator() {
        assertNull(eval("+5"))
        assertNull(eval("*5"))
    }

    /**
     * Division by zero must not surface as an amount. Left unguarded this is Infinity,
     * which would sail through a null check and be stored.
     */
    @Test
    fun rejectsDivisionByZero() {
        assertNull(eval("10/0"))
        assertNull(eval("10/0.0"))
        assertNull(eval("5+10/0"))
    }

    // --- preview -----------------------------------------------------------

    @Test
    fun previewsAnUnfinishedExpression() {
        assertEquals(12.0, ExpressionEvaluator.preview("12+"))
        assertEquals(20.0, ExpressionEvaluator.preview("12+8"))
        assertEquals(8.0, ExpressionEvaluator.preview("5+3*"))
    }

    @Test
    fun previewMatchesEvaluateForCompleteInput() {
        assertEquals(14.0, ExpressionEvaluator.preview("2+3*4"))
    }

    @Test
    fun previewIsNullWhenNoPrefixIsValid() {
        assertNull(ExpressionEvaluator.preview(""))
        assertNull(ExpressionEvaluator.preview("+"))
        assertNull(ExpressionEvaluator.preview("."))
    }

    /** A dangling division by zero should not preview as the numerator. */
    @Test
    fun previewFallsBackPastADivisionByZero() {
        assertEquals(10.0, ExpressionEvaluator.preview("10/0"))
    }

    // --- money precision ---------------------------------------------------

    @Test
    fun roundsBinaryDriftToCents() {
        val raw = eval("0.1+0.2")!!
        assertEquals(0.3, raw.roundToCents())
    }

    @Test
    fun roundsARepeatingSplitToCents() {
        // 100/3 is 33.333...; an amount has to land on a cent.
        assertEquals(33.33, eval("100/3")!!.roundToCents())
    }

    @Test
    fun roundToCentsIsHalfUpAndSignPreserving() {
        assertEquals(2.35, 2.345.roundToCents())
        assertEquals(-2.35, (-2.345).roundToCents())
        assertEquals(0.0, 0.001.roundToCents())
    }
}
