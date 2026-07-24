package com.karinca.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("reference_type") val referenceType: String,
    @SerialName("reference_amount") val referenceAmount: Double,
    @SerialName("target_date") val targetDate: String? = null,
    @SerialName("reference_set_at") val referenceSetAt: String,
    @SerialName("regular_monthly_income") val regularMonthlyIncome: Double,
    @SerialName("variable_monthly_income") val variableMonthlyIncome: Double? = null,
    @SerialName("coach_tone") val coachTone: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    // Only present on pull responses; omitted on push since the server trigger owns it.
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
)
