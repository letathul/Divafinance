package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.domain.fake.FakeTransactionRepository
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
}
