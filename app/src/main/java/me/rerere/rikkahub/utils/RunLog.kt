package me.rerere.rikkahub.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 内存环形运行日志：记录应用启动、搜索等关键操作与未捕获异常，
 * 供「设置 → 关于 → 运行日志」页面排查问题。
 *
 * 只在内存里（进程被杀即清空），不做任何持久化。
 */
object RunLog {
    const val MAX_ENTRIES = 1000

    data class Entry(
        val id: Long,
        val time: String,
        val level: Char, // V / D / I / W / E
        val tag: String,
        val message: String,
    )

    private val entries = ArrayDeque<Entry>()
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private var seq = 0L

    @Synchronized
    fun log(tag: String, message: String, level: Char = 'I') {
        entries.addLast(Entry(seq++, fmt.format(Date()), level, tag, message))
        if (entries.size > MAX_ENTRIES) entries.removeFirst()
    }

    @Synchronized
    fun clear() = entries.clear()

    /** 返回快照（新→旧排序），列表 UI 直接用 */
    @Synchronized
    fun entriesList(): List<Entry> = entries.reversed()
}