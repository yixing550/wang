package com.wangxq.consumable.reminder

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import com.wangxq.consumable.util.DateUtils
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/**
 * 提醒改为「写入系统日历日程」：
 * 每条物品的“下次更换”在系统日历里建立一条日程事件，
 * 事件在开始时间（下次更换 - 提前天数）自动触发日历提醒。
 * 不再使用 AlarmManager / 系统通知。
 */
object CalendarReminder {

    /** 选择一个可写的日历账户；找不到（如未登录任何日历账号）返回 null */
    fun pickWritableCalendarId(ctx: Context): Long? {
        val uri = CalendarContract.Calendars.CONTENT_URI
        val proj = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        // 优先取有写入权限的可见日历
        ctx.contentResolver.query(
            uri, proj,
            "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ? AND ${CalendarContract.Calendars.VISIBLE} = 1",
            arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
            null
        )?.use { c ->
            if (c.moveToFirst()) return c.getLong(0)
        }
        // 兜底：任意可见且可写的日历
        ctx.contentResolver.query(uri, proj, "${CalendarContract.Calendars.VISIBLE} = 1", null, null)?.use { c ->
            while (c.moveToNext()) {
                if (c.getInt(1) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR) return c.getLong(0)
            }
        }
        return null
    }

    private fun dayMillisAt(epochDay: Long, hour: Int): Long =
        LocalDate.ofEpochDay(epochDay).atTime(hour, 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    /**
     * 在系统日历写入/更新一条更换日程。
     * 事件从「提前 N 天」当天 09:00 开始，到「下次更换日」09:00 结束，
     * 事件起始即弹提醒，保证提前天数生效（不依赖日历提醒分钟上限）。
     * 返回日历事件 id（用于后续更新/删除）；失败返回 null。
     */
    fun upsertEvent(
        ctx: Context,
        calendarId: Long,
        itemId: Long,
        name: String,
        location: String,
        cycleText: String,
        nextDueEpoch: Long,
        leadDays: Int
    ): Long? {
        val tz = TimeZone.getDefault().id
        val remindDay = (nextDueEpoch - leadDays).coerceAtMost(nextDueEpoch)
        val start: Long = dayMillisAt(remindDay, 9)
        val end: Long = kotlin.math.max(dayMillisAt(nextDueEpoch, 9), start + 60 * 60 * 1000L)

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, "更换：$name（$location）")
            // 描述内嵌 #id= 标记，作为跨重装定位事件的唯一标识。
            // 不要用 _sync_id：该字段只有系统同步适配器可写，普通 App 写入会抛
            // IllegalArgumentException: Only sync adapters may write to _sync_id（部分机型必崩）。
            put(CalendarContract.Events.DESCRIPTION, "易耗品更换提醒 · 周期 $cycleText · #id=$itemId")
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.EVENT_TIMEZONE, tz)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        return try {
            val eventUri: Uri = ctx.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                ?: return null
            val eventId = eventUri.lastPathSegment?.toLongOrNull() ?: return null

            // 事件开始时提醒（即提前 leadDays 天）
            val rem = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, 0)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            ctx.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, rem)
            eventId
        } catch (e: Exception) {
            // 日历账户异常等情况下不阻塞主流程（避免再闪退）
            e.printStackTrace()
            null
        }
    }

    /** 删除某条日历事件 */
    fun removeEvent(ctx: Context, eventId: Long) {
        val uri = Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventId.toString())
        try {
            ctx.contentResolver.delete(uri, null, null)
        } catch (_: Exception) {
            // 事件可能已不在，忽略
        }
    }

    /** 根据物品 id 查找已写入的日历事件 id（同设备重装后再次进入时定位用） */
    fun findEventIdByItem(ctx: Context, itemId: Long): Long? {
        val proj = arrayOf(CalendarContract.Events._ID)
        // 通过描述中的 #id= 标记定位（不使用 _sync_id）
        ctx.contentResolver.query(
            CalendarContract.Events.CONTENT_URI, proj,
            "${CalendarContract.Events.DESCRIPTION} LIKE ?",
            arrayOf("%#id=$itemId%"),
            null
        )?.use { c ->
            if (c.moveToFirst()) return c.getLong(0)
        }
        return null
    }
}
