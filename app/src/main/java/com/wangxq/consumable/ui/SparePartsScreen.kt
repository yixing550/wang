package com.wangxq.consumable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wangxq.consumable.data.SparePartEntity

@Composable
fun SparePartsScreen(
    vm: SparePartViewModel = viewModel(),
    showAdd: Boolean = false,
    onAddDismiss: () -> Unit = {}
) {
    var loc by remember { mutableStateOf("全部") }
    var editPart by remember { mutableStateOf<SparePartEntity?>(null) }

    val parts by vm.parts.collectAsState()
    val locations by vm.locations.collectAsState()

    val filtered = if (loc == "全部") parts else parts.filter { it.location == loc }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("备件库存", fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Text("记录备件的存放位置、数量与备注", color = Color(0xFF6B7280), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            (listOf("全部") + locations).forEach { l ->
                val sel = l == loc
                Box(
                    Modifier.padding(end = 8.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (sel) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .border(0.5.dp, Color(0xFFD0D0D0), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                        .clickable { loc = l }
                ) { Text(l, color = if (sel) Color.White else Color(0xFF6B7280), fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Text("这个位置下还没有备件，点右下角 + 添加", color = Color(0xFF6B7280), fontSize = 13.sp, modifier = Modifier.padding(top = 40.dp))
        }
        filtered.forEach { part ->
            SparePartCard(
                part = part,
                onEdit = { editPart = part },
                onInc = { vm.adjustQuantity(part, +1) },
                onDec = { vm.adjustQuantity(part, -1) }
            )
            Spacer(Modifier.height(10.dp))
        }
    }

    if (showAdd) {
        SparePartDialog(
            locations = locations,
            onDismiss = onAddDismiss,
            onSave = { name, location, storageLocation, qty, note ->
                vm.upsert(SparePartEntity(name = name, location = location, storageLocation = storageLocation, quantity = qty, note = note))
                onAddDismiss()
            }
        )
    }
    if (editPart != null) {
        SparePartDialog(
            part = editPart,
            locations = locations,
            onDismiss = { editPart = null },
            onSave = { name, location, storageLocation, qty, note ->
                vm.upsert(editPart!!.copy(name = name, location = location, storageLocation = storageLocation, quantity = qty, note = note))
                editPart = null
            },
            onDelete = { vm.delete(editPart!!); editPart = null }
        )
    }
}

@Composable
fun SparePartCard(
    part: SparePartEntity,
    onEdit: () -> Unit,
    onInc: () -> Unit,
    onDec: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(part.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                Text(part.location, color = Color(0xFF6B7280), fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                if (part.storageLocation.isNotBlank()) {
                    Text("存放：${part.storageLocation}", color = Color(0xFF6B7280), fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
                if (part.note.isNotBlank()) {
                    Text(part.note, color = Color(0xFF9CA3AF), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDec) { Icon(Icons.Filled.Remove, null, tint = Color(0xFF6B7280)) }
                Text("${part.quantity}", fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 4.dp))
                IconButton(onClick = onInc) { Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
fun SparePartDialog(
    part: SparePartEntity? = null,
    locations: List<String>,
    onDismiss: () -> Unit,
    onSave: (name: String, location: String, storageLocation: String, quantity: Int, note: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(part?.name ?: "") }
    var location by remember { mutableStateOf(part?.location ?: "") }
    var storageLocation by remember { mutableStateOf(part?.storageLocation ?: "") }
    var qty by remember { mutableStateOf((part?.quantity ?: 1).coerceAtLeast(1)) }
    var note by remember { mutableStateOf(part?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (part == null) "添加备件" else "编辑备件") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                OutlinedTextField(location, { location = it }, label = { Text("地址（可输入新地址）") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).horizontalScroll(rememberScrollState())) {
                    locations.forEach { l ->
                        Box(
                            Modifier.padding(end = 6.dp).clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF1EFE8)).clickable { location = l }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) { Text(l, fontSize = 12.sp, color = Color(0xFF5F5E5A)) }
                    }
                }
                OutlinedTextField(storageLocation, { storageLocation = it }, label = { Text("存放位置（如：客厅储物柜第2格）") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    Text("数量", fontSize = 13.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(end = 12.dp))
                    IconButton(onClick = { qty = (qty - 1).coerceAtLeast(0) }) { Icon(Icons.Filled.Remove, null) }
                    Text("$qty", fontWeight = FontWeight.Medium, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 6.dp))
                    IconButton(onClick = { qty += 1 }) { Icon(Icons.Filled.Add, null, tint = MaterialTheme.colorScheme.primary) }
                }
                OutlinedTextField(note, { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth().height(80.dp), singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) onSave(name, location.ifBlank { "未命名地址" }, storageLocation, qty, note)
            }) { Text("保存") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text("删除", color = Color(0xFFB42318)) }
                }
                TextButton(onDismiss) { Text("取消") }
            }
        }
    )
}
