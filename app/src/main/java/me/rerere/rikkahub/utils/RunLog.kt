package me.rerere.rikkahub.utils

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 环形运行日志：记录应用启动、搜索等关键操作与未捕获异常，
 * 供「设置 → 关于 → 运行日志」页面排查问题。
 *
 * 内存部分是环形缓冲（进程被杀即清空），只供 UI 展示；
 * 但 W/E 级别（尤其是崩溃）会同步追加到 [init] 指定的 runlog.txt，
 * 否则闪退后进程一死，崩溃堆栈就彻底丢了，无法排查。
 */
object RunLog {
    const val MAX_ENTRIES = 1000

    /** 落盘文件的保留上限，超出后裁剪；崩溃堆栈通常几十 KB，足够放很多次 */
    private const val MAX_DISK_BYTES = 512L * 1024

    data class Entry(
        val id: Long,
        val time: String,
        val level: Char, // V / D / I / W / E
        val tag: String,
        val message: String,
    )

    private val entries = ArrayDeque<Entry>()
    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val diskFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var seq = 0L

    /** 由 Application.onCreate 注入，注入前不做落盘 */
    @Volatile
    private var logFile: File? = null

    fun init(dir: File) {
        val file = File(dir, "runlog.txt")
        logFile = file
        runCatching {
            if (file.exists() && file.length() > MAX_DISK_BYTES) {
                // 只保留后半段，避免无限增长
                val kept = file.readText().takeLast((MAX_DISK_BYTES / 2).toInt())
                file.writeText(kept)
            }
        }
    }

    @Synchronized
    fun log(tag: String, message: String, level: Char = 'I') {
        entries.addLast(Entry(seq++, fmt.format(Date()), level, tag, message))
        if (entries.size > MAX_ENTRIES) entries.removeFirst()
        // 只有可能用于事后排查的级别才落盘，避免 I 级日志频繁写盘
        if (level == 'E' || level == 'W') {
            appendToDisk(level, tag, message)
        }
    }

    private fun appendToDisk(level: Char, tag: String, message: String) {
        val file = logFile ?: return
        runCatching {
            file.parentFile?.mkdirs()
            // 同步追加：崩溃时后面的代码可能不再执行，必须当场写完
            file.appendText("${diskFmt.format(Date())} $level/$tag: $message\n")
        }
    }

    /** 读回落盘日志，供崩溃页/日志页展示 */
    fun readDiskLog(): String = runCatching {
        logFile?.takeIf { it.exists() }?.readText().orEmpty()
    }.getOrDefault("")

    @Synchronized
    fun clear() = entries.clear()

    /** 返回快照（新→旧排序），列表 UI 直接用 */
    @Synchronized
    fun entriesList(): List<Entry> = entries.reversed()
}
