package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.testing.fake.FakeTransactionRepository
import com.divafinance.core.testing.fake.TestData
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportStatementUseCaseTest {

    private val txRepo = FakeTransactionRepository()
    private val useCase = ImportStatementUseCase(txRepo)

    @Test
    fun importsValidCsvLines() = runTest {
        val csv = """
            Date,Description,Amount
            2024-06-15,Coffee Shop,-5.50
            2024-06-16,Salary Deposit,3000.00
        """.trimIndent()

        val count = useCase(csv, "acc-1", "card-1")

        assertEquals(2, count)
        assertEquals(2, txRepo.count())
    }

    @Test
    fun setsDebitForNegativeAmounts() = runTest {
        val csv = "Date,Description,Amount\n2024-06-15,Purchase,-100.00"

        useCase(csv, "acc-1", null)

        val tx = txRepo.getByAccountId("acc-1").first()
        assertEquals(TransactionType.DEBIT, tx.type)
        assertEquals(100.0, tx.amount)
    }

    @Test
    fun setsCreditForPositiveAmounts() = runTest {
        val csv = "Date,Description,Amount\n2024-06-15,Refund,50.00"

        useCase(csv, "acc-1", null)

        val tx = txRepo.getByAccountId("acc-1").first()
        assertEquals(TransactionType.CREDIT, tx.type)
    }

    @Test
    fun skipsInvalidLines() = runTest {
        val csv = """
            Date,Description,Amount
            2024-06-15,Valid,-10.00
            invalid-date,Bad,abc
            short
        """.trimIndent()

        val count = useCase(csv, "acc-1", null)

        assertEquals(1, count)
    }

    @Test
    fun skipsHeaderRow() = runTest {
        val csv = "Date,Description,Amount\n2024-06-15,Item,-25.00"

        val count = useCase(csv, "acc-1", null)

        assertEquals(1, count)
    }

    @Test
    fun handlesQuotedCsvFields() = runTest {
        val csv = "Date,Description,Amount\n2024-06-15,\"Coffee, Tea\",-12.50"

        val count = useCase(csv, "acc-1", null)

        assertEquals(1, count)
        val tx = txRepo.getByAccountId("acc-1").first()
        assertEquals("Coffee, Tea", tx.merchantName)
    }

    /**
     * Every imported row used to land in OTHER regardless of what it said, which made a
     * statement import useless for any of the category-driven screens.
     */
    @Test
    fun categorisesAnImportedRowFromMatchingHistory() = runTest {
        txRepo.insert(
            TestData.transaction(
                id = "seed",
                accountId = "history-acc",
                merchantName = "Blue Bottle Coffee",
                category = SpendingCategory.DINING,
                type = TransactionType.DEBIT,
            )
        )

        useCase("Date,Description,Amount\n2024-06-15,Blue Bottle Coffee,-5.50", "acc-1", null)

        val imported = txRepo.getByAccountId("acc-1").single()
        assertEquals(SpendingCategory.DINING, imported.category)
    }

    /** Nothing to reason from is not a licence to invent one. */
    @Test
    fun fallsBackToOtherWithNoHistory() = runTest {
        useCase("Date,Description,Amount\n2024-06-15,Some Unknown Shop,-5.50", "acc-1", null)

        assertEquals(SpendingCategory.OTHER, txRepo.getByAccountId("acc-1").single().category)
    }

    /** Income has no spending category, so guessing one would be nonsense. */
    @Test
    fun leavesCreditRowsAsOther() = runTest {
        txRepo.insert(
            TestData.transaction(
                id = "seed",
                accountId = "history-acc",
                merchantName = "Salary Deposit",
                category = SpendingCategory.DINING,
                type = TransactionType.DEBIT,
            )
        )

        useCase("Date,Description,Amount\n2024-06-16,Salary Deposit,3000.00", "acc-1", null)

        val credit = txRepo.getByAccountId("acc-1").single { it.type == TransactionType.CREDIT }
        assertEquals(SpendingCategory.OTHER, credit.category)
    }
}
