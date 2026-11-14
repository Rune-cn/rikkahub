package me.rerere.rikkahub.utils

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 运行日志：记录应用启动、生成、工具执行、异常等关键路径，
 * 供「设置 → 关于 → 运行日志」页面排查问题，崩溃后也能读回落盘内容。
 *
 * - 内存：环形缓冲（[MAX_ENTRIES] 条），进程内实时可读，UI 直接展示。
 * - 落盘：W/E 级同步追加到 runlog.txt，崩溃时进程被杀也能留下证据。
 * - 导出：可用 [exportText] 取全量文本，供用户复制/分享。
 */
object RunLog {
    const val MAX_ENTRIES = 2000

    /** 落盘文件的保留上限，超出后裁剪；崩溃堆栈通常几十 KB，足够放很多次 */
    private const val MAX_DISK_BYTES = 1024L * 1024

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

    /**
     * 记一条日志。
     *
     * @param level V/D/I/W/E；W/E 会同时落盘。
     * @param throwable 传入时自动附加完整堆栈，省得调用方自己 stackTraceToString。
     */
    @Synchronized
    fun log(tag: String, message: String, level: Char = 'I', throwable: Throwable? = null) {
        val text = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        entries.addLast(Entry(seq++, fmt.format(Date()), level, tag, text))
        while (entries.size > MAX_ENTRIES) entries.removeFirst()
        // 只有可能用于事后排查的级别才落盘，避免 I/V/D 频繁写盘
        if (level == 'E' || level == 'W') {
            appendToDisk(level, tag, text)
        }
    }

    /** 便捷方法：捕获异常并记录，返回 null 便于 `?: return` */
    inline fun <T> guard(tag: String, what: String, block: () -> T): T? =
        try {
            block()
        } catch (t: Throwable) {
            log(tag, "$what 失败", 'E', t)
            null
        }

    private fun appendToDisk(level: Char, tag: String, message: String) {
        val file = logFile ?: return
        runCatching {
            file.parentFile?.mkdirs()
            // 同步追加：崩溃时后面的代码可能不再执行，必须当场写完
            file.appendText("${diskFmt.format(Date())} $level/$tag: $message\n")
        }
    }

    /** 读回落盘日志（崩溃后内存已清空时用） */
    fun readDiskLog(): String = runCatching {
        logFile?.takeIf { it.exists() }?.readText().orEmpty()
    }.getOrDefault("")

    /** 全量文本：内存日志 + 落盘日志，供「复制全部 / 导出」用 */
    fun exportText(): String {
        val mem = entriesList().joinToString("\n") {
            "${it.time} [${it.level}] ${it.tag}: ${it.message}"
        }
        val disk = readDiskLog()
        return buildString {
            appendLine("===== 内存日志（本次进程，共 ${entries.size} 条）=====")
            appendLine(mem.ifBlank { "(空)" })
            appendLine()
            appendLine("===== 落盘日志（runlog.txt，含历史崩溃）=====")
            appendLine(disk.ifBlank { "(空)" })
        }
    }

    @Synchronized
    fun clear() {
        entries.clear()
        // 同时清掉落盘内容，避免清空后日志页仍显示旧历史
        runCatching { logFile?.takeIf { it.exists() }?.writeText("") }
    }

    /** 返回快照（新→旧排序），列表 UI 直接用 */
    @Synchronized
    fun entriesList(): List<Entry> = entries.reversed()
}
