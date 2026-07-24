package com.karinca.app.data.remote

import com.karinca.app.data.local.CategoryKind
import com.karinca.app.data.local.CoachTone
import com.karinca.app.data.local.PiggyBankEntryReason
import com.karinca.app.data.local.ReferenceType
import com.karinca.app.data.local.entity.CategoryEntity
import com.karinca.app.data.local.entity.ConsentEntity
import com.karinca.app.data.local.entity.GoalEntity
import com.karinca.app.data.local.entity.PiggyBankEntryEntity
import com.karinca.app.data.local.entity.ProfileEntity
import com.karinca.app.data.local.entity.TransactionEntity
import com.karinca.app.data.local.entity.WeeklyReportEntity
import com.karinca.app.data.remote.dto.CategoryDto
import com.karinca.app.data.remote.dto.ConsentDto
import com.karinca.app.data.remote.dto.GoalDto
import com.karinca.app.data.remote.dto.PiggyBankEntryDto
import com.karinca.app.data.remote.dto.ProfileDto
import com.karinca.app.data.remote.dto.TransactionDto
import com.karinca.app.data.remote.dto.WeeklyReportDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

// PostgREST returns timestamptz as ISO-8601 with an explicit offset (e.g. "+00:00"),
// not always the strict "Z" suffix Instant.parse requires — OffsetDateTime handles both.
internal fun parseInstant(value: String): Instant = OffsetDateTime.parse(value).toInstant()

fun ProfileDto.toEntity() = ProfileEntity(
    id = id,
    referenceType = ReferenceType.fromValue(referenceType),
    referenceAmount = referenceAmount,
    targetDate = targetDate?.let(LocalDate::parse),
    referenceSetAt = parseInstant(referenceSetAt),
    regularMonthlyIncome = regularMonthlyIncome,
    variableMonthlyIncome = variableMonthlyIncome,
    coachTone = CoachTone.fromValue(coachTone),
    createdAt = parseInstant(createdAt),
    updatedAt = parseInstant(updatedAt),
)

fun ProfileEntity.toDto() = ProfileDto(
    id = id,
    referenceType = referenceType.value,
    referenceAmount = referenceAmount,
    targetDate = targetDate?.toString(),
    referenceSetAt = referenceSetAt.toString(),
    regularMonthlyIncome = regularMonthlyIncome,
    variableMonthlyIncome = variableMonthlyIncome,
    coachTone = coachTone.value,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun ConsentDto.toEntity() = ConsentEntity(
    id = id,
    kvkkConsentAt = parseInstant(kvkkConsentAt),
    kvkkConsentVersion = kvkkConsentVersion,
    createdAt = parseInstant(createdAt),
    updatedAt = parseInstant(updatedAt),
)

fun ConsentEntity.toDto() = ConsentDto(
    id = id,
    kvkkConsentAt = kvkkConsentAt.toString(),
    kvkkConsentVersion = kvkkConsentVersion,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)

fun CategoryDto.toEntity() = CategoryEntity(
    id = id,
    userId = userId,
    kind = CategoryKind.fromValue(kind),
    name = name,
    color = color,
    icon = icon,
    createdAt = parseInstant(createdAt),
    updatedAt = parseInstant(updatedAt),
    deletedAt = deletedAt?.let(::parseInstant),
)

fun CategoryEntity.toDto() = CategoryDto(
    id = id,
    userId = userId,
    kind = kind.value,
    name = name,
    color = color,
    icon = icon,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    deletedAt = deletedAt?.toString(),
)

fun TransactionDto.toEntity() = TransactionEntity(
    id = id,
    userId = userId,
    categoryId = categoryId,
    type = CategoryKind.fromValue(type),
    amount = amount,
    occurredAt = LocalDate.parse(occurredAt),
    note = note,
    source = source,
    createdAt = parseInstant(createdAt),
    updatedAt = parseInstant(updatedAt),
    deletedAt = deletedAt?.let(::parseInstant),
)

fun TransactionEntity.toDto() = TransactionDto(
    id = id,
    userId = userId,
    categoryId = categoryId,
    type = type.value,
    amount = amount,
    occurredAt = occurredAt.toString(),
    note = note,
    source = source,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    deletedAt = deletedAt?.toString(),
)

fun GoalDto.toEntity() = GoalEntity(
    id = id,
    userId = userId,
    name = name,
    icon = icon,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    targetDate = LocalDate.parse(targetDate),
    createdAt = parseInstant(createdAt),
    updatedAt = parseInstant(updatedAt),
    deletedAt = deletedAt?.let(::parseInstant),
    completedAt = completedAt?.let(::parseInstant),
    lastEvaluatedDate = lastEvaluatedDate?.let(LocalDate::parse),
)

fun GoalEntity.toDto() = GoalDto(
    id = id,
    userId = userId,
    name = name,
    icon = icon,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    targetDate = targetDate.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    deletedAt = deletedAt?.toString(),
    completedAt = completedAt?.toString(),
    lastEvaluatedDate = lastEvaluatedDate?.toString(),
)

// No toDto() for weekly reports — they're a read-only mirror, never pushed.
fun WeeklyReportDto.toEntity() = WeeklyReportEntity(
    id = id,
    userId = userId,
    weekStart = LocalDate.parse(weekStart),
    weekEnd = LocalDate.parse(weekEnd),
    contractVersion = contractVersion,
    factsJson = Json.encodeToString(JsonElement.serializer(), facts),
    narration = narration,
    coachToneUsed = CoachTone.fromValue(coachToneUsed),
    profileSnapshotJson = Json.encodeToString(JsonElement.serializer(), profileSnapshot),
    createdAt = parseInstant(createdAt),
    updatedAt = parseInstant(updatedAt),
)

fun PiggyBankEntryDto.toEntity() = PiggyBankEntryEntity(
    id = id,
    userId = userId,
    amount = amount,
    reason = PiggyBankEntryReason.fromValue(reason),
    transactionId = transactionId,
    createdAt = parseInstant(createdAt),
    updatedAt = parseInstant(updatedAt),
)

fun PiggyBankEntryEntity.toDto() = PiggyBankEntryDto(
    id = id,
    userId = userId,
    amount = amount,
    reason = reason.value,
    transactionId = transactionId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
