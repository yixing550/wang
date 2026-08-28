package com.wangxq.consumable.util

import com.wangxq.consumable.data.ItemWithReplacements
import com.wangxq.consumable.data.SparePartEntity
import com.wangxq.consumable.util.ReplaceStatus

/** 把物品与备件导出为 CSV（UTF-8 + BOM，Excel 直接打开中文不乱码） */
object CsvExporter {

    private val HEADER = listOf(
        "类型", "名称", "地址", "存放位置", "类别", "周期", "单位",
        "数量", "备注", "更换日期", "下次到期", "状态"
    )

    fun build(items: List<ItemWithReplacements>, parts: List<SparePartEntity>): String {
        val sb = StringBuilder()
        sb.append('\uFEFF') // BOM
        sb.appendLine(HEADER.joinToString(","))

        val itemRows = items.flatMap { wr ->
            val next = DateUtils.nextDueEpoch(
                wr.item.cycleValue, wr.item.cycleUnit, wr.replacements.map { it.dateEpoch }
            )
            val statusText = when (DateUtils.statusOf(next)) {
                ReplaceStatus.OVERDUE -> "已逾期"
                ReplaceStatus.SOON -> "即将到期"
                ReplaceStatus.NORMAL -> "正常"
                else -> "待设置"
            }
            val nextStr = next?.let { DateUtils.fmt(it) } ?: ""
            if (wr.replacements.isEmpty()) {
                listOf(
                    row("物品", wr.item.name, wr.item.location, "", wr.item.category,
                        wr.item.cycleValue.toString(), wr.item.cycleUnit, "", wr.item.note,
                        "", nextStr, statusText)
                )
            } else {
                wr.replacements.sortedBy { it.dateEpoch }.map { r ->
                    row("物品", wr.item.name, wr.item.location, "", wr.item.category,
                        wr.item.cycleValue.toString(), wr.item.cycleUnit, "", wr.item.note,
                        DateUtils.fmt(r.dateEpoch), nextStr, statusText)
                }
            }
        }

        val partRows = parts.map { p ->
            row("备件", p.name, p.location, p.storageLocation, "", "", "",
                p.quantity.toString(), p.note, "", "", "")
        }

        (itemRows + partRows).forEach { sb.appendLine(it) }
        return sb.toString()
    }

    private fun row(
        type: String, name: String, location: String, storage: String, category: String,
        cycle: String, unit: String, qty: String, note: String, date: String,
        next: String, status: String
    ): String {
        return listOf(type, name, location, storage, category, cycle, unit, qty, note, date, next, status)
            .joinToString(",") { escape(it) }
    }

    /** 含逗号/引号/换行时用双引号包裹，内部引号转义为两个引号 */
    private fun escape(v: String): String =
        if (v.contains(',') || v.contains('"') || v.contains('\n')) {
            "\"" + v.replace("\"", "\"\"") + "\""
        } else v
}
