package com.karinca.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoalDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val icon: String,
    @SerialName("target_amount") val targetAmount: Double,
    @SerialName("current_amount") val currentAmount: Double,
    @SerialName("target_date") val targetDate: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("last_evaluated_date") val lastEvaluatedDate: String? = null,
    @SerialName("server_updated_at") val serverUpdatedAt: String? = null,
)
