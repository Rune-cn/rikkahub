package me.rerere.rikkahub.ui.pages.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.ai.provider.ModelType
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.RetryMode
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.ui.components.ai.ModelSelector
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.compose.koinInject

@Composable
fun OtherSettingsPage() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    val settingsStore = koinInject<SettingsStore>()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.extensions_page_other_category)) },
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
                    title = { Text(stringResource(R.string.extensions_page_sub_agent_section)) },
                ) {
                    item(
                        headlineContent = {
                            Text(stringResource(R.string.extensions_page_sub_agent_model))
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    text = stringResource(R.string.extensions_page_sub_agent_model_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                ModelSelector(
                                    modelId = settings.subAgentSetting.defaultModelId,
                                    providers = settings.providers,
                                    type = ModelType.CHAT,
                                    allowClear = true,
                                    modifier = Modifier.padding(top = 4.dp),
                                    onSelect = { model ->
                                        // 清除时 Model() 为空对象，用能否在 providers 里找到来区分
                                        val selectedId = model.id.takeIf { id ->
                                            settings.findModelById(id) != null
                                        }
                                        coroutineScope.launch {
                                            settingsStore.update { s ->
                                                s.copy(
                                                    subAgentSetting = s.subAgentSetting.copy(
                                                        defaultModelId = selectedId,
                                                    )
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                        },
                    )
                }
            }
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text(stringResource(R.string.extensions_page_sub_agent_section)) },
                ) {
                    item(
                        headlineContent = {
                            Text(stringResource(R.string.extensions_page_sub_agent_inherit_context))
                        },
                        supportingContent = {
                            Text(stringResource(R.string.extensions_page_sub_agent_inherit_context_desc))
                        },
                        trailingContent = {
                            Switch(
                                checked = settings.subAgentSetting.defaultInheritContext,
                                onCheckedChange = { enabled ->
                                    coroutineScope.launch {
                                        settingsStore.update { s ->
                                            s.copy(
                                                subAgentSetting = s.subAgentSetting.copy(
                                                    defaultInheritContext = enabled,
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
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text(stringResource(R.string.extensions_page_retry_section)) },
                ) {
                    item(
                        headlineContent = { Text(stringResource(R.string.extensions_page_auto_retry)) },
                        supportingContent = {
                            Text(stringResource(R.string.extensions_page_auto_retry_desc))
                        },
                        trailingContent = {
                            Switch(
                                checked = settings.networkSetting.enableRateLimitRetry,
                                onCheckedChange = { enabled ->
                                    coroutineScope.launch {
                                        settingsStore.update {
                                            it.copy(
                                                networkSetting = it.networkSetting.copy(
                                                    enableRateLimitRetry = enabled,
                                                )
                                            )
                                        }
                                    }
                                },
                            )
                        },
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.extensions_page_retry_interval)) },
                        supportingContent = {
                            val modes = listOf(
                                RetryMode.FIXED,
                                RetryMode.EXPONENTIAL_BACKOFF,
                                RetryMode.JITTER,
                            )
                            val modeLabels = listOf(
                                stringResource(R.string.extensions_page_retry_interval_fixed),
                                stringResource(R.string.extensions_page_retry_interval_exponential),
                                stringResource(R.string.extensions_page_retry_interval_jitter),
                            )
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                modes.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                                        selected = settings.networkSetting.rateLimitRetryMode == mode,
                                        onClick = {
                                            coroutineScope.launch {
                                                settingsStore.update {
                                                    it.copy(
                                                        networkSetting = it.networkSetting.copy(
                                                            rateLimitRetryMode = mode,
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                    ) {
                                        Text(modeLabels[index])
                                    }
                                }
                            }
                        },
                    )
                    item(
                        headlineContent = { Text(stringResource(R.string.extensions_page_retry_max_count)) },
                        supportingContent = {
                            val counts = listOf(3, 5)
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                counts.forEachIndexed { index, count ->
                                    SegmentedButton(
                                        shape = SegmentedButtonDefaults.itemShape(index, counts.size),
                                        selected = settings.networkSetting.rateLimitRetryCount == count,
                                        onClick = {
                                            coroutineScope.launch {
                                                settingsStore.update {
                                                    it.copy(
                                                        networkSetting = it.networkSetting.copy(
                                                            rateLimitRetryCount = count,
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                    ) {
                                        Text(count.toString())
                                    }
                                }
                            }
                        },
                    )
                    // 可重试状态码
                    item(
                        headlineContent = {
                            Text(stringResource(R.string.extensions_page_retry_status_codes))
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    text = stringResource(R.string.extensions_page_retry_status_codes_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                RetryChipListEditor(
                                    items = settings.networkSetting.retryableStatusCodes.map { it.toString() },
                                    keyboardType = KeyboardType.Number,
                                ) { list ->
                                    val codes = list.mapNotNull { it.toIntOrNull() }.filter { it in 100..599 }
                                    coroutineScope.launch {
                                        settingsStore.update {
                                            it.copy(
                                                networkSetting = it.networkSetting.copy(
                                                    retryableStatusCodes = codes,
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    )
                    // 重试关键字
                    item(
                        headlineContent = {
                            Text(stringResource(R.string.extensions_page_retry_keywords))
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    text = stringResource(R.string.extensions_page_retry_keywords_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                RetryChipListEditor(
                                    items = settings.networkSetting.retryKeywords,
                                ) { list ->
                                    coroutineScope.launch {
                                        settingsStore.update {
                                            it.copy(
                                                networkSetting = it.networkSetting.copy(
                                                    retryKeywords = list,
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    )
                    // 停止重试关键字
                    item(
                        headlineContent = {
                            Text(stringResource(R.string.extensions_page_stop_retry_keywords))
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    text = stringResource(R.string.extensions_page_stop_retry_keywords_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                RetryChipListEditor(
                                    items = settings.networkSetting.stopRetryKeywords,
                                ) { list ->
                                    coroutineScope.launch {
                                        settingsStore.update {
                                            it.copy(
                                                networkSetting = it.networkSetting.copy(
                                                    stopRetryKeywords = list,
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * 重试规则的标签列表编辑器
 *
 * 已有项显示为可删除的标签（点击标签即删除），点「添加」输入新项（回车确认）。
 */
@Composable
private fun RetryChipListEditor(
    items: List<String>,
    keyboardType: KeyboardType = KeyboardType.Text,
    onItemsChange: (List<String>) -> Unit,
) {
    var adding by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            InputChip(
                selected = false,
                onClick = { onItemsChange(items - item) },
                label = {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = HugeIcons.Cancel01,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                },
            )
        }
        if (adding) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.width(140.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val value = input.trim()
                        if (value.isNotEmpty() && value !in items) {
                            onItemsChange(items + value)
                        }
                        input = ""
                        adding = false
                    }
                ),
            )
        } else {
            AssistChip(
                onClick = { adding = true },
                label = { Text(stringResource(R.string.extensions_page_retry_add_tag)) },
            )
        }
    }
}