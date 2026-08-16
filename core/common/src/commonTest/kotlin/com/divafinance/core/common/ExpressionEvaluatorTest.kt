package com.divafinance.core.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExpressionEvaluatorTest {

    private fun eval(expression: String) = ExpressionEvaluator.evaluate(expression)

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
        assertNull(eval("(5+3)"))
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
