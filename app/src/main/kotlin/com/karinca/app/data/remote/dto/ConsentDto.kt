package com.karinca.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConsentDto(
    val id: String,
    @SerialName("kvkk_consent_at") val kvkkConsentAt: String,
    @SerialName("kvkk_consent_version") val kvkkConsentVersion: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    // Only present on pull responses; omitted on push since the server trigger owns it.
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
)
