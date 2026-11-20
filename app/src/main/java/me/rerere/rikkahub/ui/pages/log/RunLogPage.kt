package me.rerere.rikkahub.ui.pages.log

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Bookshelf01
import me.rerere.hugeicons.stroke.Copy01
import me.rerere.hugeicons.stroke.Database02
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Refresh01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.ui.theme.JetbrainsMono
import me.rerere.rikkahub.utils.RunLog
import me.rerere.rikkahub.utils.plus
import me.rerere.rikkahub.utils.writeClipboardText

/**
 * 运行日志页。
 *
 * 两个来源：
 * - 「本次运行」：内存环形日志，进程内实时可读，重启后清空。
 * - 「已保存」：落盘 runlog.txt，包含历次 W/E（含崩溃堆栈），重启后仍在。
 *
 * 右上角：导出全部（内存+落盘，一键复制）/ 刷新 / 清空。
 */
@Composable
fun RunLogPage() {
    var selectedTab by remember { mutableStateOf(0) }
    var entries by remember { mutableStateOf(RunLog.entriesList()) }
    var diskText by remember { mutableStateOf(RunLog.readDiskLog()) }
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    fun refresh() {
        entries = RunLog.entriesList()
        diskText = RunLog.readDiskLog()
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.run_log_page_title)) },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(
                        onClick = {
                            // 导出「内存 + 落盘」全量：崩溃排查时这一份就够
                            context.writeClipboardText(RunLog.exportText())
                            Toast.makeText(context, R.string.run_log_export_done, Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(
                            HugeIcons.Copy01,
                            contentDescription = stringResource(R.string.run_log_export),
                        )
                    }
                    IconButton(onClick = { refresh() }) {
                        Icon(
                            HugeIcons.Refresh01,
                            contentDescription = stringResource(R.string.run_log_refresh),
                        )
                    }
                    IconButton(
                        onClick = {
                            RunLog.clear()
                            refresh()
                        }
                    ) {
                        Icon(
                            HugeIcons.Delete01,
                            contentDescription = stringResource(R.string.run_log_clear),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = CustomColors.topBarColors.containerColor,
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; refresh() },
                    text = { Text(stringResource(R.string.run_log_tab_memory)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; refresh() },
                    text = { Text(stringResource(R.string.run_log_tab_disk)) },
                )
            }

            if (selectedTab == 0) {
                if (entries.isEmpty()) {
                    EmptyHint(
                        icon = HugeIcons.Bookshelf01,
                        title = stringResource(R.string.run_log_empty),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            RunLogRow(entry = entry)
                        }
                    }
                }
            } else {
                if (diskText.isBlank()) {
                    EmptyHint(
                        icon = HugeIcons.Database02,
                        title = stringResource(R.string.run_log_disk_empty),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 落盘内容按行渲染，方便单行复制
                        items(diskText.lines()) { line ->
                            DiskLogRow(line = line)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 8.dp)
                .size(112.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialShapes.Cookie9Sided.toShape(),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmallEmphasized,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RunLogRow(entry: RunLog.Entry) {
    val context = LocalContext.current
    val color = when (entry.level) {
        'E' -> MaterialTheme.colorScheme.error
        'W' -> MaterialTheme.colorScheme.tertiary
        'I' -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val line = "${entry.time} [${entry.level}] ${entry.tag}: ${entry.message}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.writeClipboardText(line)
                Toast.makeText(context, R.string.run_log_copied, Toast.LENGTH_SHORT).show()
            },
        colors = CustomColors.cardColorsOnSurfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = JetbrainsMono,
                )
                Text(
                    text = "[${entry.level}]",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontFamily = JetbrainsMono,
                )
                Text(
                    text = entry.tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = JetbrainsMono,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = entry.message,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                fontFamily = JetbrainsMono,
            )
        }
    }
}

@Composable
private fun DiskLogRow(line: String) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.writeClipboardText(line)
                Toast.makeText(context, R.string.run_log_copied, Toast.LENGTH_SHORT).show()
            },
        colors = CustomColors.cardColorsOnSurfaceContainer,
    ) {
        Text(
            text = line,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = JetbrainsMono,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}