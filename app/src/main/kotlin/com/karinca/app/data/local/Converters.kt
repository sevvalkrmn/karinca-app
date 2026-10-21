package com.karinca.app.data.local

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

// min SDK 26 means java.time is available natively — no desugaring needed.
class Converters {

    @TypeConverter
    fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun referenceTypeToValue(value: ReferenceType?): String? = value?.value

    @TypeConverter
    fun valueToReferenceType(value: String?): ReferenceType? = value?.let(ReferenceType::fromValue)

    @TypeConverter
    fun coachToneToValue(value: CoachTone?): String? = value?.value

    @TypeConverter
    fun valueToCoachTone(value: String?): CoachTone? = value?.let(CoachTone::fromValue)

    @TypeConverter
    fun categoryKindToValue(value: CategoryKind?): String? = value?.value

    @TypeConverter
    fun valueToCategoryKind(value: String?): CategoryKind? = value?.let(CategoryKind::fromValue)

    @TypeConverter
    fun recurringDestinationToValue(value: RecurringDestination?): String? = value?.value

    @TypeConverter
    fun valueToRecurringDestination(value: String?): RecurringDestination? = value?.let(RecurringDestination::fromValue)

    @TypeConverter
    fun piggyBankEntryReasonToValue(value: PiggyBankEntryReason?): String? = value?.value

    @TypeConverter
    fun valueToPiggyBankEntryReason(value: String?): PiggyBankEntryReason? = value?.let(PiggyBankEntryReason::fromValue)
}
