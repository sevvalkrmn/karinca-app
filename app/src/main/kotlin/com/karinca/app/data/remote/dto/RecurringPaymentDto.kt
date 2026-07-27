package com.karinca.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// last_processed_month is deliberately absent — server-only bookkeeping the client
// never reads or writes. The Supabase client's default Json (ignoreUnknownKeys =
// true) silently drops it from pull responses rather than failing to decode.
@Serializable
data class RecurringPaymentDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val amount: Double,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("day_of_month") val dayOfMonth: Int,
    val active: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
)
