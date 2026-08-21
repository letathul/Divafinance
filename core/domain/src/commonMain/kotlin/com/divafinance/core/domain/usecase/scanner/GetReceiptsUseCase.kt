package com.divafinance.core.domain.usecase.scanner

import com.divafinance.core.data.repository.ReceiptRepository
import com.divafinance.core.model.Receipt
import kotlinx.coroutines.flow.Flow

/** Every scan, newest first — the backing query already orders by `created_at DESC`. */
class GetReceiptsUseCase(
    private val receiptRepository: ReceiptRepository
) {
    operator fun invoke(): Flow<List<Receipt>> = receiptRepository.getAll()
}
