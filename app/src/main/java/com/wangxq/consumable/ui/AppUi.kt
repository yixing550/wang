package com.wangxq.consumable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.wangxq.consumable.data.SparePartEntity
import com.wangxq.consumable.util.CsvExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.wangxq.consumable.data.ItemEntity
import com.wangxq.consumable.data.ItemWithReplacements
import com.wangxq.consumable.data.ReplacementEntity
import com.wangxq.consumable.ui.theme.AmberStatus
import com.wangxq.consumable.ui.theme.GreenStatus
import com.wangxq.consumable.ui.theme.RedStatus
import com.wangxq.consumable.ui.theme.TealContainer
import com.wangxq.consumable.util.DateUtils
import com.wangxq.consumable.util.ReplaceStatus
import java.time.LocalDate

val CATS = listOf("清洁配件", "净水滤芯", "电池", "滤网", "其他")

fun statusColor(s: ReplaceStatus) = when (s) {
    ReplaceStatus.OVERDUE -> RedStatus
    ReplaceStatus.SOON -> AmberStatus
    ReplaceStatus.NORMAL -> GreenStatus
    else -> Color(0xFF5F5E5A)
}

fun statusBg(s: ReplaceStatus) = when (s) {
    ReplaceStatus.OVERDUE -> Color(0xFFFCEBEB)
    ReplaceStatus.SOON -> Color(0xFFFAEEDA)
    ReplaceStatus.NORMAL -> Color(0xFFEAF3DE)
    else -> Color(0xFFF1EFE8)
}

fun statusLabel(s: ReplaceStatus) = when (s) {
    ReplaceStatus.OVERDUE -> "已逾期"
    ReplaceStatus.SOON -> "即将到期"
    ReplaceStatus.NORMAL -> "正常"
    else -> "待设置"
}

fun catColors(cat: String): Pair<Color, Color> = when (cat) {
    "清洁配件" -> Color(0xFFE1F5EE) to Color(0xFF0F6E56)
    "净水滤芯" -> Color(0xFFE6F1FB) to Color(0xFF185FA5)
    "电池" -> Color(0xFFFAEEDA) to Color(0xFF854F0B)
    "滤网" -> Color(0xFFEEEDFE) to Color(0xFF534AB7)
    else -> Color(0xFFF1EFE8) to Color(0xFF5F5E5A)
}

fun catShort(cat: String) = when (cat) {
    "清洁配件" -> "清"
    "净水滤芯" -> "水"
    "电池" -> "电"
    "滤网" -> "滤"
    else -> "他"
}

fun nextDue(wr: ItemWithReplacements): Long? =
    DateUtils.nextDueEpoch(wr.item.cycleValue, wr.item.cycleUnit, wr.replacements.map { it.dateEpoch })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    var tab by remember { mutableStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var showSpareAdd by remember { mutableStateOf(false) }
    var detailId by remember { mutableStateOf<Long?>(null) }
    var calDenied by remember { mutableStateOf(false) }
    var showExport by remember { mutableStateOf(false) }

    val items by vm.items.collectAsState()
    val locations by vm.locations.collectAsState()
    val spareVm: SparePartViewModel = viewModel()
    val parts by spareVm.parts.collectAsState()
    val ctx = LocalContext.current

    val calPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { res ->
        if (res[Manifest.permission.WRITE_CALENDAR] == true) vm.syncCalendar()
        else calDenied = true
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_CALENDAR)
            == PackageManager.PERMISSION_GRANTED
        ) {
            vm.syncCalendar()
        } else {
            calPermLauncher.launch(
                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (tab) {
                            0 -> "清单"
                            1 -> "统计"
                            else -> "备件"
                        }, fontWeight = FontWeight.Medium
                    )
                },
                actions = {
                    IconButton(onClick = { showExport = true }) {
                        Icon(Icons.Filled.Share, "导出")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.List, null) }, label = { Text("清单") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.BarChart, null) }, label = { Text("统计") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.Widgets, null) }, label = { Text("备件") })
            }
        },
        floatingActionButton = {
            if (tab == 0) FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Filled.Add, null)
            }
            if (tab == 2) FloatingActionButton(onClick = { showSpareAdd = true }) {
                Icon(Icons.Filled.Add, null)
            }
        }
    ) { padding ->
        Box(
            Modifier.padding(padding).fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (calDenied) {
                Text(
                    "未授权日历权限，到期提醒不会写入系统日历；可在系统设置中为本应用开启“日历”权限。",
                    fontSize = 12.sp, color = Color(0xFF854F0B),
                    modifier = Modifier.padding(12.dp)
                        .background(Color(0xFFFAEEDA), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                )
            }
            when (tab) {
                0 -> ListScreen(vm, items, locations) { detailId = it.item.id }
                1 -> StatsScreen(items)
                2 -> SparePartsScreen(vm = spareVm, showAdd = showSpareAdd, onAddDismiss = { showSpareAdd = false })
            }
        }
    }

    if (showAdd) AddItemDialog(vm, locations) { showAdd = false }
    if (detailId != null) DetailDialog(vm, detailId!!) { detailId = null }
    if (showExport) ExportDialog(items, parts) { showExport = false }
}

@Composable
fun ExportDialog(
    items: List<ItemWithReplacements>,
    parts: List<SparePartEntity>,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var selectedItems by remember { mutableStateOf(setOf<Long>()) }
    var selectedParts by remember { mutableStateOf(setOf<Long>()) }

    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) {
            try {
                val csv = CsvExporter.build(
                    items.filter { it.item.id in selectedItems },
                    parts.filter { it.id in selectedParts }
                )
                ctx.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(csv.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(ctx, "已导出（可用 Excel/备忘录打开）", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(ctx, "导出失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出数据") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = {
                        selectedItems = items.map { it.item.id }.toSet()
                        selectedParts = parts.map { it.id }.toSet()
                    }) { Text("全选") }
                    TextButton(onClick = {
                        selectedItems = emptySet()
                        selectedParts = emptySet()
                    }) { Text("全不选") }
                    Spacer(Modifier.weight(1f))
                    val total = selectedItems.size + selectedParts.size
                    Text("已选 $total 项", color = Color(0xFF6B7280), fontSize = 12.sp)
                }
                Spacer(Modifier.height(4.dp))

                Text("易耗品（${items.size}）", fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 4.dp))
                items.forEach { wr ->
                    RowCheck(wr.item.id in selectedItems, wr.item.name, wr.item.location) { chk ->
                        selectedItems = if (chk) selectedItems + wr.item.id else selectedItems - wr.item.id
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("备件（${parts.size}）", fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 4.dp))
                parts.forEach { p ->
                    RowCheck(p.id in selectedParts, p.name, p.location) { chk ->
                        selectedParts = if (chk) selectedParts + p.id else selectedParts - p.id
                    }
                }
            }
        },
        confirmButton = {
            val total = selectedItems.size + selectedParts.size
            TextButton(
                enabled = total > 0,
                onClick = {
                    val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                    saver.launch("易耗品更换记录_$ts.csv")
                }
            ) { Text("导出为 CSV") }
        },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }
    )
}

@Composable
private fun RowCheck(checked: Boolean, name: String, sub: String, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle(!checked) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onToggle)
        Column(Modifier.padding(start = 8.dp)) {
            Text(name, fontSize = 13.sp)
            if (sub.isNotBlank()) Text("· $sub", color = Color(0xFF6B7280), fontSize = 12.sp)
        }
    }
}

@Composable
fun RowScope.SummaryCard(n: Int, label: String, color: Color = MaterialTheme.colorScheme.onBackground) {
    Column(
        Modifier.weight(1f).background(Color(0xFFEDEDEA), RoundedCornerShape(8.dp)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("$n", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = color)
        Text(label, fontSize = 12.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun ProgressBar(pct: Float, color: Color) {
    Box(
        Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFDfDfDf))
    ) {
        Box(
            Modifier.fillMaxWidth(pct.coerceIn(0f, 1f)).height(6.dp)
                .clip(RoundedCornerShape(3.dp)).background(color)
        )
    }
}

@Composable
fun ItemCard(wr: ItemWithReplacements, onClick: () -> Unit) {
    val nxt = nextDue(wr)
    val status = DateUtils.statusOf(nxt)
    val dl = DateUtils.daysLeft(nxt)
    val pct = if (nxt != null && wr.replacements.isNotEmpty()) {
        val last = wr.replacements.maxOf { it.dateEpoch }
        val total = (nxt - last).toFloat().coerceAtLeast(1f)
        val elapsed = (DateUtils.todayEpoch() - last).toFloat()
        (elapsed / total).coerceIn(0f, 1f)
    } else 0f
    val (bg, fg) = catColors(wr.item.category)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(bg),
                contentAlignment = Alignment.Center
            ) { Text(catShort(wr.item.category), color = fg, fontWeight = FontWeight.Medium) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(wr.item.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
                    Box(
                        Modifier.clip(RoundedCornerShape(8.dp)).background(statusBg(status))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) { Text(statusLabel(status), color = statusColor(status), fontSize = 12.sp) }
                }
                Text(
                    "${wr.item.location} · 周期 ${wr.item.cycleValue}${wr.item.cycleUnit} · 已换 ${wr.replacements.size} 次",
                    color = Color(0xFF6B7280), fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp)
                )
                ProgressBar(pct, statusColor(status))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("下次 ${nxt?.let { DateUtils.fmt(it) } ?: "—"}", color = Color(0xFF6B7280), fontSize = 12.sp)
                    Text(
                        dl?.let { if (it < 0) "逾期 ${-it} 天" else "剩 $it 天" } ?: "未设置",
                        color = statusColor(status), fontWeight = FontWeight.Medium, fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ListScreen(
    vm: MainViewModel,
    items: List<ItemWithReplacements>,
    locations: List<String>,
    onItemClick: (ItemWithReplacements) -> Unit
) {
    var loc by remember { mutableStateOf("全部") }
    val filtered = if (loc == "全部") items else items.filter { it.item.location == loc }

    val total = items.size
    val soon = items.count { DateUtils.statusOf(nextDue(it)) == ReplaceStatus.SOON }
    val over = items.count { DateUtils.statusOf(nextDue(it)) == ReplaceStatus.OVERDUE }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("我的家", fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Text("易耗品更换提醒", color = Color(0xFF6B7280), fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.height(12.dp))

        // 原“提醒”栏的到期提醒设置，迁移到清单页顶部
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TealContainer)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("到期自动提醒", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = Color(0xFF0F6E56))
                Text("开启提醒的物品，临近更换时自动加入系统日历日程，由日历在当天提醒", fontSize = 12.sp, color = Color(0xFF0F6E56), modifier = Modifier.padding(top = 4.dp))
                Row(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("提前提醒", fontSize = 13.sp, color = Color(0xFF0F6E56))
                    Row {
                        listOf(7, 15, 30, 60).forEach { d ->
                            val sel = vm.leadDays == d
                            Box(
                                Modifier.padding(start = 6.dp).clip(RoundedCornerShape(8.dp))
                                    .background(if (sel) MaterialTheme.colorScheme.primary else Color.White)
                                    .clickable { vm.updateLeadDays(d) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) { Text("${d}天", color = if (sel) Color.White else Color(0xFF6B7280), fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(total, "总数")
            SummaryCard(soon, "即将到期", AmberStatus)
            SummaryCard(over, "已逾期", RedStatus)
        }
        Spacer(Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            Text("这个地址下还没有记录，点右下角 + 添加", color = Color(0xFF6B7280), fontSize = 13.sp, modifier = Modifier.padding(top = 40.dp))
        }
        filtered.forEach { ItemCard(it) { onItemClick(it) } }
    }
}

@Composable
fun StatsScreen(items: List<ItemWithReplacements>) {
    val now = LocalDate.now()
    val months = (0..5).map { now.plusMonths(it.toLong()) }
    val counts = months.map { m ->
        items.count { wr ->
            wr.replacements.isNotEmpty() && run {
                val nd = nextDue(wr)!!
                val d = LocalDate.ofEpochDay(nd)
                d.year == m.year && d.monthValue == m.monthValue
            }
        }
    }
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)

    val total = items.size
    val soon = items.count { DateUtils.statusOf(nextDue(it)) == ReplaceStatus.SOON }
    val over = items.count { DateUtils.statusOf(nextDue(it)) == ReplaceStatus.OVERDUE }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("统计", fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(total, "物品总数")
            SummaryCard(soon, "即将到期", AmberStatus)
            SummaryCard(over, "已逾期", RedStatus)
        }
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(14.dp)) {
                Text("未来6个月到期分布", fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
                Row(
                    Modifier.fillMaxWidth().height(120.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    months.forEachIndexed { i, _ ->
                        Column(
                            Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text("${counts[i]}", fontSize = 11.sp, color = Color(0xFF6B7280))
                            Box(
                                Modifier.width(18.dp).height((counts[i].toFloat() / max * 90).dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            )
                            Text("${months[i].monthValue}月", fontSize = 11.sp, color = Color(0xFF6B7280), modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(label: String, epochDay: Long, onChanged: (Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    val state = rememberDatePickerState(initialSelectedDateMillis = DateUtils.epochDayToMillis(epochDay))
    OutlinedTextField(
        value = DateUtils.fmt(epochDay),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            // 日历图标也可点击
            Icon(
                Icons.Filled.DateRange, null,
                modifier = Modifier.clickable { show = true }
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            // 用 pointerInput 而非 clickable，规避 OutlinedTextField(readOnly=true) 拦截手势
            .pointerInput(epochDay) {
                detectTapGestures(onTap = { show = true })
            }
    )
    if (show) {
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    show = false
                    state.selectedDateMillis?.let { onChanged(DateUtils.millisToEpochDay(it)) }
                }) { Text("确定") }
            }
        ) { DatePicker(state = state) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    title: String,
    initialEpoch: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = DateUtils.epochDayToMillis(initialEpoch))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onConfirm(DateUtils.millisToEpochDay(it)) }
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }
    ) { DatePicker(state = state, title = { Text(title) }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onValueChange(opt); expanded = false })
            }
        }
    }
}

@Composable
fun AddItemDialog(vm: MainViewModel, locations: List<String>, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATS[0]) }
    var cycleValue by remember { mutableStateOf("1") }
    var cycleUnit by remember { mutableStateOf("年") }
    var note by remember { mutableStateOf("") }
    var firstEpoch by remember { mutableStateOf(DateUtils.todayEpoch()) }
    var remind by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加更换记录") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("物品名称") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
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
                DropdownField("类别", CATS, category, onValueChange = { category = it })
                Spacer(Modifier.height(12.dp))
                DateField("首次更换日期", firstEpoch) { firstEpoch = it }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        cycleValue, { cycleValue = it.filter { c -> c.isDigit() } },
                        label = { Text("更换周期") }, modifier = Modifier.weight(1f)
                    )
                    Box(Modifier.weight(1f)) {
                        DropdownField("单位", listOf("月", "年"), cycleUnit, onValueChange = { cycleUnit = it })
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(note, { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth().height(80.dp), singleLine = false)
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clickable { remind = !remind }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = remind, onCheckedChange = { remind = it })
                    Column(Modifier.padding(start = 8.dp)) {
                        Text("创建日程提醒", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("到期后加入系统日历并提醒（取消则仅在应用内显示）", color = Color(0xFF6B7280), fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    vm.addItem(
                        name, location.ifBlank { "未命名地址" }, category,
                        cycleValue.toIntOrNull() ?: 1, cycleUnit, note, firstEpoch, remind
                    )
                    onDismiss()
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }
    )
}

@Composable
fun EditItemDialog(vm: MainViewModel, item: ItemEntity, locations: List<String>, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(item.name) }
    var location by remember { mutableStateOf(item.location) }
    var category by remember { mutableStateOf(item.category) }
    var cycleValue by remember { mutableStateOf(item.cycleValue.toString()) }
    var cycleUnit by remember { mutableStateOf(item.cycleUnit) }
    var note by remember { mutableStateOf(item.note) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑物品") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("物品名称") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp))
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
                DropdownField("类别", CATS, category, onValueChange = { category = it })
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        cycleValue, { cycleValue = it.filter { c -> c.isDigit() } },
                        label = { Text("更换周期") }, modifier = Modifier.weight(1f)
                    )
                    Box(Modifier.weight(1f)) {
                        DropdownField("单位", listOf("月", "年"), cycleUnit, onValueChange = { cycleUnit = it })
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(note, { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth().height(80.dp), singleLine = false)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    vm.updateItem(
                        item.copy(
                            name = name,
                            location = location.ifBlank { "未命名地址" },
                            category = category,
                            cycleValue = cycleValue.toIntOrNull() ?: item.cycleValue,
                            cycleUnit = cycleUnit,
                            note = note
                        )
                    )
                    onDismiss()
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onDismiss) { Text("取消") } }
    )
}

@Composable
fun DetailDialog(vm: MainViewModel, itemId: Long, onDismiss: () -> Unit) {
    val flow = remember(itemId) { vm.itemById(itemId) }
    val wr by flow.collectAsState(initial = null)
    var newEpoch by remember { mutableStateOf(DateUtils.todayEpoch()) }
    var showEdit by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editingR by remember { mutableStateOf<ReplacementEntity?>(null) }
    val locations by vm.locations.collectAsState()

    if (wr == null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onDismiss) { Text("关闭") } },
            text = { Text("未找到物品") }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(wr!!.item.name, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, null) }
                }
                Text(
                    "${wr!!.item.location} · ${wr!!.item.category} · 周期 ${wr!!.item.cycleValue}${wr!!.item.cycleUnit}",
                    color = Color(0xFF6B7280), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { showEdit = true }) { Text("编辑物品") }
                    TextButton(onClick = { confirmDelete = true }) { Text("删除物品", color = Color(0xFFB42318)) }
                }

                val nxt = nextDue(wr!!)
                val status = DateUtils.statusOf(nxt)
                val dl = DateUtils.daysLeft(nxt)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDEDEA))
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("下次更换", color = Color(0xFF6B7280), fontSize = 12.sp)
                        Text(nxt?.let { DateUtils.fmt(it) } ?: "—", color = statusColor(status), fontWeight = FontWeight.Medium, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp))
                        Text(
                            dl?.let { if (it < 0) "逾期 ${-it} 天" else "剩 $it 天" } ?: "未设置",
                            color = statusColor(status), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clickable {
                        vm.updateItem(wr!!.item.copy(remindEnabled = !wr!!.item.remindEnabled))
                    }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = wr!!.item.remindEnabled,
                        onCheckedChange = { vm.updateItem(wr!!.item.copy(remindEnabled = it)) }
                    )
                    Column(Modifier.padding(start = 8.dp)) {
                        Text("日程提醒", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("开启后临近更换会写入系统日历并提醒", color = Color(0xFF6B7280), fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("更换历史（${wr!!.replacements.size} 次）", fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                val sorted = wr!!.replacements.sortedBy { it.dateEpoch }
                sorted.forEachIndexed { idx, r ->
                    val isLast = idx == sorted.lastIndex
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 10.dp)
                            .border(0.5.dp, Color(0xFFE0E0E0)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(Modifier.weight(1f).clickable { editingR = r }, verticalAlignment = Alignment.CenterVertically) {
                            Text(DateUtils.fmt(r.dateEpoch), fontSize = 13.sp)
                            if (isLast) Text(" 当前", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                            else if (idx == sorted.lastIndex - 1) Text(" 上次", color = Color(0xFF6B7280), fontSize = 11.sp)
                        }
                        IconButton(onClick = { editingR = r }) {
                            Icon(Icons.Filled.Edit, null, tint = Color(0xFF6B7280))
                        }
                        if (wr!!.replacements.size > 1) {
                            TextButton(onClick = { vm.deleteReplacement(r) }) {
                                Text("删除", color = Color(0xFF6B7280), fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                DateField("记录本次更换日期", newEpoch) { newEpoch = it }
                Spacer(Modifier.height(10.dp))
                Button(onClick = { vm.addReplacement(itemId, newEpoch) }, modifier = Modifier.fillMaxWidth()) {
                    Text("记录本次更换")
                }
            }
        }
    )

    if (showEdit) EditItemDialog(vm, wr!!.item, locations, onDismiss = { showEdit = false })
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除物品") },
            text = { Text("确定删除「${wr!!.item.name}」及其全部更换记录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteItem(wr!!.item)
                    confirmDelete = false
                    onDismiss()
                }) { Text("删除", color = Color(0xFFB42318)) }
            },
            dismissButton = { TextButton({ confirmDelete = false }) { Text("取消") } }
        )
    }
    if (editingR != null) {
        DatePickerModal(
            title = "修改更换日期",
            initialEpoch = editingR!!.dateEpoch,
            onConfirm = { vm.updateReplacement(editingR!!.copy(dateEpoch = it)); editingR = null },
            onDismiss = { editingR = null }
        )
    }
}
