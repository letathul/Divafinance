package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.data.repository.ReceiptRepository
import com.divafinance.core.model.Receipt

/**
 * One stored scan. This is the whole input to the review screen: seeding every field from the
 * persisted row is what makes "resume a pending receipt" and "review a fresh scan" the same
 * code path, and what lets a review survive process death without any draft state.
 */
class GetReceiptUseCase(
    private val receiptRepository: ReceiptRepository
) {
    suspend operator fun invoke(id: String): Receipt? = receiptRepository.getById(id)
}
