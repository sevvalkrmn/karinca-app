package com.karinca.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PiggyBankEntryDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val amount: Double,
    val reason: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
)
