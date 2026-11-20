package me.rerere.rikkahub.ui.pages.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import me.rerere.ai.provider.ModelType
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.AiBrain01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.findModelById
import me.rerere.rikkahub.ui.components.ai.ModelField
import me.rerere.rikkahub.ui.components.ai.ModelListSheet
import me.rerere.rikkahub.ui.components.ai.rememberModelListState
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.CardGroupIcon
import me.rerere.rikkahub.ui.components.ui.switchItem
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.compose.koinInject

@Composable
fun OtherSettingsPage() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    val settingsStore = koinInject<SettingsStore>()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()

    // 子 Agent 默认模型：与「设置 → 默认模型」页同一套组件（ModelField + ModelListSheet）
    val subAgentModel = rememberModelListState(
        modelId = settings.subAgentSetting.defaultModelId,
        providers = settings.providers,
        type = ModelType.CHAT,
    )

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
            contentPadding = innerPadding + PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                CardGroup {
                    switchItem(
                        checked = settings.enableSubAgent,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                settingsStore.update { s -> s.copy(enableSubAgent = enabled) }
                            }
                        },
                        headlineContent = {
                            Text(stringResource(R.string.extensions_page_multi_agent_enable))
                        },
                        supportingContent = {
                            Text(stringResource(R.string.extensions_page_multi_agent_enable_desc))
                        },
                    )
                    if (settings.enableSubAgent) {
                        // 用 formItem：标题行 + 下方整行带底色的选择器，与「默认模型」页一致
                        formItem(
                            headlineContent = {
                                Text(stringResource(R.string.extensions_page_sub_agent_model))
                            },
                            supportingContent = {
                                Text(stringResource(R.string.extensions_page_sub_agent_model_desc))
                            },
                            leadingContent = {
                                CardGroupIcon(
                                    HugeIcons.AiBrain01,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            },
                        ) {
                            ModelField(
                                model = subAgentModel.currentModel,
                                onClick = { subAgentModel.open() },
                                onClear = {
                                    coroutineScope.launch {
                                        settingsStore.update { s ->
                                            s.copy(
                                                subAgentSetting = s.subAgentSetting.copy(
                                                    defaultModelId = null,
                                                )
                                            )
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    ModelListSheet(state = subAgentModel) { model ->
        val selectedId = model.id.takeIf { id -> settings.findModelById(id) != null }
        coroutineScope.launch {
            settingsStore.update { s ->
                s.copy(
                    subAgentSetting = s.subAgentSetting.copy(defaultModelId = selectedId)
                )
            }
        }
    }
}
