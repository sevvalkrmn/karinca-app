package com.karinca.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EntitlementDto(
    @SerialName("user_id") val userId: String,
    @SerialName("is_pro") val isPro: Boolean,
    @SerialName("updated_at") val updatedAt: String,
    // Only present on pull responses; this table is never pushed by the client.
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
)
