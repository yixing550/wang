package com.wangxq.consumable.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ReplaceStatus { NONE, NORMAL, SOON, OVERDUE }

object DateUtils {
    const val SOON_THRESHOLD = 45L

    fun todayEpoch(): Long = LocalDate.now().toEpochDay()

    fun epochToDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    fun fmt(epochDay: Long): String {
        val d = epochToDate(epochDay)
        return "%04d-%02d-%02d".format(d.year, d.monthValue, d.dayOfMonth)
    }

    fun epochDayToMillis(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun millisToEpochDay(millis: Long): Long =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

    fun nextDueEpoch(cycleValue: Int, cycleUnit: String, replacements: List<Long>): Long? {
        if (replacements.isEmpty()) return null
        val last = replacements.maxOrNull()!!
        val base = epochToDate(last)
        val next = if (cycleUnit == "年") base.plusYears(cycleValue.toLong())
        else base.plusMonths(cycleValue.toLong())
        return next.toEpochDay()
    }

    fun daysLeft(next: Long?): Long? = next?.let { it - todayEpoch() }

    fun statusOf(next: Long?): ReplaceStatus {
        if (next == null) return ReplaceStatus.NONE
        val dl = next - todayEpoch()
        return when {
            dl < 0 -> ReplaceStatus.OVERDUE
            dl <= SOON_THRESHOLD -> ReplaceStatus.SOON
            else -> ReplaceStatus.NORMAL
        }
    }
}
