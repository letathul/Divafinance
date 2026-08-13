package com.divafinance.core.common

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

object UuidGenerator {
    @OptIn(ExperimentalUuidApi::class)
    fun generate(): String = Uuid.random().toString()
}
