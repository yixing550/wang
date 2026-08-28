package com.wangxq.consumable.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wangxq.consumable.ConsumableApplication
import com.wangxq.consumable.data.SparePartEntity
import com.wangxq.consumable.data.SparePartRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SparePartViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = SparePartRepository((app as ConsumableApplication).database.sparePartDao())

    val parts: StateFlow<List<SparePartEntity>> =
        repo.parts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val locations: StateFlow<List<String>> =
        repo.locations.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun upsert(part: SparePartEntity) {
        viewModelScope.launch {
            if (part.id == 0L) repo.insert(part) else repo.update(part)
        }
    }

    fun delete(part: SparePartEntity) {
        viewModelScope.launch { repo.delete(part) }
    }

    /** 卡片上直接 +/- 调数量，下限夹到 0 */
    fun adjustQuantity(part: SparePartEntity, delta: Int) {
        viewModelScope.launch {
            val q = (part.quantity + delta).coerceAtLeast(0)
            repo.update(part.copy(quantity = q))
        }
    }
}
