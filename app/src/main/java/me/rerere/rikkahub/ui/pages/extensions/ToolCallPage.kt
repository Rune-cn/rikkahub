package me.rerere.rikkahub.ui.pages.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.compose.koinInject

// 可配置自动允许的内置工具（MCP 工具同样受"自动允许"控制，但名字动态，无法在此逐一列出）
private val BUILT_IN_TOOL_NAMES = listOf(
    "workspace_read_file",
    "workspace_write_file",
    "workspace_edit_file",
    "workspace_shell",
    "ask_user",
    "calendar_query",
    "calendar_create",
    "memory_tool",
    "recent_chats",
    "conversation_search",
    "search_web",
    "scrape_web",
    "use_skill",
    "clipboard_tool",
    "eval_javascript",
    "get_screen_time",
    "text_to_speech",
    "get_time_info",
    "chart_display",
)

@Composable
fun ToolCallPage() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    val settingsStore = koinInject<SettingsStore>()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val autoAllow = settings.toolAutoAllowSetting

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.tool_call_page_title)) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    item(
                        headlineContent = { Text(stringResource(R.string.tool_call_auto_allow)) },
                        supportingContent = {
                            Text(
                                text = stringResource(R.string.tool_call_auto_allow_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = autoAllow.enabled,
                                onCheckedChange = { enabled ->
                                    coroutineScope.launch {
                                        settingsStore.update {
                                            it.copy(
                                                toolAutoAllowSetting = it.toolAutoAllowSetting.copy(
                                                    enabled = enabled,
                                                )
                                            )
                                        }
                                    }
                                },
                            )
                        },
                    )
                    // 开启后显示工具列表，可自由选择哪些工具自动允许
                    if (autoAllow.enabled) {
                        BUILT_IN_TOOL_NAMES.forEach { toolName ->
                            item(
                                headlineContent = {
                                    Text(
                                        text = toolName,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                trailingContent = {
                                    Switch(
                                        checked = toolName !in autoAllow.disabledTools,
                                        onCheckedChange = { allowed ->
                                            coroutineScope.launch {
                                                settingsStore.update {
                                                    val disabled = it.toolAutoAllowSetting.disabledTools
                                                    it.copy(
                                                        toolAutoAllowSetting = it.toolAutoAllowSetting.copy(
                                                            disabledTools = if (allowed) {
                                                                disabled - toolName
                                                            } else {
                                                                disabled + toolName
                                                            }
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}