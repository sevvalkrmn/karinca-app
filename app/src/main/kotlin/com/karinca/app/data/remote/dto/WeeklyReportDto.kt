package com.karinca.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WeeklyReportDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("week_start") val weekStart: String,
    @SerialName("week_end") val weekEnd: String,
    @SerialName("contract_version") val contractVersion: String,
    @SerialName("facts_json") val facts: JsonElement,
    val narration: String? = null,
    @SerialName("coach_tone_used") val coachToneUsed: String,
    @SerialName("profile_snapshot_json") val profileSnapshot: JsonElement,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
)
