package com.wangxq.consumable.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wangxq.consumable.ConsumableApplication
import com.wangxq.consumable.data.ItemEntity
import com.wangxq.consumable.data.ItemRepository
import com.wangxq.consumable.data.ItemWithReplacements
import com.wangxq.consumable.data.ReplacementEntity
import com.wangxq.consumable.reminder.CalendarReminder
import com.wangxq.consumable.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ItemRepository((app as ConsumableApplication).database.itemDao())

    val items: StateFlow<List<ItemWithReplacements>> =
        repo.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locations: StateFlow<List<String>> =
        repo.locations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var leadDays: Int by mutableStateOf(30)
        private set

    fun itemById(id: Long): StateFlow<ItemWithReplacements?> =
        repo.itemById(id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private fun hasCalendarPerm(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(), Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    /** 把全部物品的“下次更换”写入系统日历日程；无权限或无可写日历时静默跳过 */
    fun syncCalendar() {
        if (!hasCalendarPerm()) return
        viewModelScope.launch {
            val calId = CalendarReminder.pickWritableCalendarId(getApplication()) ?: return@launch
            val lead = leadDays
            repo.allItems().forEach { wr ->
                val next = DateUtils.nextDueEpoch(
                    wr.item.cycleValue, wr.item.cycleUnit, wr.replacements.map { it.dateEpoch }
                ) ?: return@forEach
                // 若本地没记事件 id，尝试按物品 id 在日历里找回（重装/换机后）
                val oldId = wr.item.calendarEventId
                    ?: CalendarReminder.findEventIdByItem(getApplication(), wr.item.id)
                oldId?.let { CalendarReminder.removeEvent(getApplication(), it) }
                val newId = CalendarReminder.upsertEvent(
                    getApplication(), calId, wr.item.id, wr.item.name, wr.item.location,
                    "${wr.item.cycleValue}${wr.item.cycleUnit}", next, lead
                )
                if (newId != null && newId != wr.item.calendarEventId) {
                    repo.updateItem(wr.item.copy(calendarEventId = newId))
                }
            }
        }
    }

    /** 兼容旧调用名 */
    fun rescheduleNow() = syncCalendar()

    fun updateLeadDays(d: Int) {
        leadDays = d
        syncCalendar()
    }

    fun addItem(
        name: String,
        location: String,
        category: String,
        cycleValue: Int,
        cycleUnit: String,
        note: String,
        firstEpoch: Long
    ) {
        viewModelScope.launch {
            repo.insertItemWithFirst(
                ItemEntity(
                    name = name,
                    location = location,
                    category = category,
                    cycleValue = cycleValue,
                    cycleUnit = cycleUnit,
                    note = note
                ),
                firstEpoch
            )
            syncCalendar()
        }
    }

    fun addReplacement(itemId: Long, epoch: Long) {
        viewModelScope.launch {
            repo.insertReplacement(itemId, epoch)
            syncCalendar()
        }
    }

    fun deleteReplacement(r: ReplacementEntity) {
        viewModelScope.launch {
            repo.deleteReplacement(r)
            syncCalendar()
        }
    }

    fun updateReplacement(r: ReplacementEntity) {
        viewModelScope.launch {
            repo.updateReplacement(r)
            syncCalendar()
        }
    }

    fun updateItem(item: ItemEntity) {
        viewModelScope.launch {
            repo.updateItem(item)
            syncCalendar()
        }
    }

    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch {
            // 先删日历事件，再删本地数据
            item.calendarEventId?.let { CalendarReminder.removeEvent(getApplication(), it) }
            repo.deleteItem(item)
        }
    }
}
