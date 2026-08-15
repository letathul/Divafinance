package com.divafinance.core.data.repository

import com.divafinance.core.database.DivaFinanceDb
import com.divafinance.core.model.BackupArchive
import com.divafinance.core.model.UserSettings
import com.divafinance.core.model.enums.AccountType
import com.divafinance.core.model.enums.CardNetwork
import com.divafinance.core.model.enums.CapPeriod
import com.divafinance.core.model.enums.FeedPostType
import com.divafinance.core.model.enums.ReceiptStatus
import com.divafinance.core.model.enums.RewardType
import com.divafinance.core.model.enums.SpendingCategory
import com.divafinance.core.model.enums.TransactionType
import com.divafinance.core.model.*
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class BackupRepositoryImpl(
    private val db: DivaFinanceDb
) : BackupRepository {

    override suspend fun exportAll(): BackupArchive {
        val accounts = db.accountQueries.selectAll().executeAsList().map { row ->
            Account(
                id = row.id, name = row.name, type = AccountType.valueOf(row.type),
                currency = row.currency, balance = row.balance, color = row.color, icon = row.icon,
                isActive = row.is_active == 1L, createdAt = Instant.parse(row.created_at),
                updatedAt = Instant.parse(row.updated_at),
            )
        }

        val cards = db.creditCardQueries.selectAll().executeAsList().map { row ->
            CreditCard(
                id = row.id, accountId = row.account_id, name = row.name, lastFour = row.last_four,
                network = runCatching { CardNetwork.valueOf(row.network) }.getOrDefault(CardNetwork.OTHER),
                color = row.color, creditLimit = row.credit_limit, currentBalance = row.current_balance,
                statementDate = row.statement_date?.toInt(), dueDate = row.due_date?.toInt(),
                annualFee = row.annual_fee, isActive = row.is_active == 1L,
                createdAt = Instant.parse(row.created_at), updatedAt = Instant.parse(row.updated_at),
            )
        }

        val rules = db.rewardRuleQueries.selectAll().executeAsList().map { row ->
            CardRewardRule(
                id = row.id, cardId = row.card_id, category = SpendingCategory.valueOf(row.category),
                multiplier = row.multiplier, rewardType = RewardType.valueOf(row.reward_type),
                capAmount = row.cap_amount,
                capPeriod = row.cap_period?.let { runCatching { CapPeriod.valueOf(it) }.getOrNull() },
                isActive = row.is_active == 1L,
            )
        }

        val transactions = db.transactionQueries.selectAll().executeAsList().map { row ->
            Transaction(
                id = row.id, accountId = row.account_id, cardId = row.card_id, amount = row.amount,
                currency = row.currency, category = SpendingCategory.valueOf(row.category),
                subcategory = row.subcategory, merchantName = row.merchant_name, note = row.note,
                date = LocalDate.parse(row.date), type = TransactionType.valueOf(row.type),
                location = run {
                    val lat = row.latitude
                    val lon = row.longitude
                    if (lat != null && lon != null) {
                        LocationTag(lat, lon, row.location_name)
                    } else null
                },
                receiptId = row.receipt_id, isRecurring = row.is_recurring == 1L,
                createdAt = Instant.parse(row.created_at),
            )
        }

        val receipts = db.receiptQueries.selectAll().executeAsList().map { row ->
            Receipt(
                id = row.id, transactionId = row.transaction_id, imagePath = row.image_path,
                ocrText = row.ocr_text, merchantName = row.merchant_name, totalAmount = row.total_amount,
                date = row.date?.let { LocalDate.parse(it) },
                status = runCatching { ReceiptStatus.valueOf(row.status) }.getOrDefault(ReceiptStatus.PENDING),
                createdAt = Instant.parse(row.created_at),
            )
        }

        val feedPosts = db.feedPostQueries.selectAll().executeAsList().map { row ->
            FeedPost(
                id = row.id,
                type = runCatching { FeedPostType.valueOf(row.type) }.getOrDefault(FeedPostType.TRANSACTION),
                title = row.title, body = row.body, transactionId = row.transaction_id,
                metadata = row.metadata, createdAt = Instant.parse(row.created_at),
            )
        }

        val settings = db.settingsQueries.selectAll().executeAsList().map { row ->
            UserSettings(key = row.key, value = row.value_)
        }

        val thresholds = db.graphThresholdQueries.selectAll().executeAsList().map { row ->
            GraphThreshold(
                id = row.id, category = SpendingCategory.valueOf(row.category),
                thresholdPercent = row.threshold_percent, isActive = row.is_active == 1L,
            )
        }

        return BackupArchive(
            version = 1,
            createdAt = Clock.System.now(),
            accounts = accounts,
            creditCards = cards,
            rewardRules = rules,
            transactions = transactions,
            receipts = receipts,
            feedPosts = feedPosts,
            settings = settings,
            thresholds = thresholds,
        )
    }

    override suspend fun importAll(archive: BackupArchive, replaceExisting: Boolean) {
        db.transaction {
            if (replaceExisting) {
                db.feedPostQueries.deleteOlderThan("9999-12-31T23:59:59Z")
                db.receiptQueries.selectAll().executeAsList().forEach { db.receiptQueries.delete(it.id) }
                db.transactionQueries.selectAll().executeAsList().forEach { db.transactionQueries.delete(it.id) }
                db.rewardRuleQueries.selectAll().executeAsList().forEach { db.rewardRuleQueries.delete(it.id) }
                db.creditCardQueries.selectAll().executeAsList().forEach { db.creditCardQueries.delete(it.id) }
                db.accountQueries.selectAll().executeAsList().forEach { db.accountQueries.delete(it.id) }
                db.graphThresholdQueries.selectAll().executeAsList().forEach { db.graphThresholdQueries.delete(it.id) }
                db.settingsQueries.deleteAll()
            }

            archive.accounts.forEach { account ->
                db.accountQueries.insert(
                    id = account.id, name = account.name, type = account.type.name,
                    currency = account.currency, balance = account.balance, color = account.color,
                    icon = account.icon, is_active = if (account.isActive) 1L else 0L,
                    created_at = account.createdAt.toString(), updated_at = account.updatedAt.toString(),
                )
            }

            archive.creditCards.forEach { card ->
                db.creditCardQueries.insert(
                    id = card.id, account_id = card.accountId, name = card.name,
                    last_four = card.lastFour, network = card.network.name, color = card.color,
                    credit_limit = card.creditLimit, current_balance = card.currentBalance,
                    statement_date = card.statementDate?.toLong(), due_date = card.dueDate?.toLong(),
                    annual_fee = card.annualFee, is_active = if (card.isActive) 1L else 0L,
                    created_at = card.createdAt.toString(), updated_at = card.updatedAt.toString(),
                )
            }

            archive.rewardRules.forEach { rule ->
                db.rewardRuleQueries.insert(
                    id = rule.id, card_id = rule.cardId, category = rule.category.name,
                    multiplier = rule.multiplier, reward_type = rule.rewardType.name,
                    cap_amount = rule.capAmount, cap_period = rule.capPeriod?.name,
                    is_active = if (rule.isActive) 1L else 0L,
                )
            }

            archive.transactions.forEach { tx ->
                db.transactionQueries.insert(
                    id = tx.id, account_id = tx.accountId, card_id = tx.cardId, amount = tx.amount,
                    currency = tx.currency, category = tx.category.name, subcategory = tx.subcategory,
                    merchant_name = tx.merchantName, note = tx.note, date = tx.date.toString(),
                    type = tx.type.name, latitude = tx.location?.latitude,
                    longitude = tx.location?.longitude, location_name = tx.location?.name,
                    receipt_id = tx.receiptId, is_recurring = if (tx.isRecurring) 1L else 0L,
                    created_at = tx.createdAt.toString(),
                )
            }

            archive.receipts.forEach { receipt ->
                db.receiptQueries.insert(
                    id = receipt.id, transaction_id = receipt.transactionId,
                    image_path = receipt.imagePath, ocr_text = receipt.ocrText,
                    merchant_name = receipt.merchantName, total_amount = receipt.totalAmount,
                    date = receipt.date?.toString(), status = receipt.status.name,
                    created_at = receipt.createdAt.toString(),
                )
            }

            archive.feedPosts.forEach { post ->
                db.feedPostQueries.insert(
                    id = post.id, type = post.type.name, title = post.title, body = post.body,
                    transaction_id = post.transactionId, metadata = post.metadata,
                    created_at = post.createdAt.toString(),
                )
            }

            archive.settings.forEach { setting ->
                db.settingsQueries.upsert(setting.key, setting.value)
            }

            archive.thresholds.forEach { threshold ->
                db.graphThresholdQueries.insert(
                    id = threshold.id, category = threshold.category.name,
                    threshold_percent = threshold.thresholdPercent,
                    is_active = if (threshold.isActive) 1L else 0L,
                )
            }
        }
    }
}
