package me.rerere.rikkahub.ui.pages.log

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Refresh01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.RunLog
import me.rerere.rikkahub.utils.writeClipboardText

/**
 * 运行日志页：展示内存环形运行日志（时间 / 级别 / 标签 / 内容）。
 * 点击某条复制该行；右上角刷新 / 清空。
 */
@Composable
fun RunLogPage() {
    var entries by remember { mutableStateOf(RunLog.entriesList()) }
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.run_log_page_title)) },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(
                        onClick = { entries = RunLog.entriesList() }
                    ) {
                        Icon(
                            HugeIcons.Refresh01,
                            contentDescription = stringResource(R.string.run_log_refresh),
                        )
                    }
                    IconButton(
                        onClick = {
                            RunLog.clear()
                            entries = RunLog.entriesList()
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
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.run_log_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(entries, key = { it.id }) { entry ->
                    RunLogRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun RunLogRow(entry: RunLog.Entry) {
    val context = LocalContext.current
    val color = when (entry.level) {
        'E' -> MaterialTheme.colorScheme.error
        'W' -> Color(0xFFB25C00)
        'I' -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = "${entry.time} [${entry.level}] ${entry.tag}: ${entry.message}",
        color = color,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.writeClipboardText("${entry.time} [${entry.level}] ${entry.tag}: ${entry.message}")
                Toast.makeText(context, R.string.run_log_copied, Toast.LENGTH_SHORT).show()
            },
    )
}