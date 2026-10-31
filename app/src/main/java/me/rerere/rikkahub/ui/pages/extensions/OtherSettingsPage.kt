package me.rerere.rikkahub.ui.pages.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
                    title = { Text(stringResource(R.string.extensions_page_multi_agent_enable)) },
                ) {
                    item(
                        headlineContent = {
                            Text(stringResource(R.string.extensions_page_multi_agent_enable))
                        },
                        supportingContent = {
                            Text(stringResource(R.string.extensions_page_multi_agent_enable_desc))
                        },
                        trailingContent = {
                            Switch(
                                checked = settings.enableSubAgent,
                                onCheckedChange = { enabled ->
                                    coroutineScope.launch {
                                        settingsStore.update { s -> s.copy(enableSubAgent = enabled) }
                                    }
                                },
                            )
                        },
                    )
                    if (settings.enableSubAgent) {
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
                                        type = me.rerere.ai.provider.ModelType.CHAT,
                                        allowClear = true,
                                        modifier = Modifier.padding(top = 4.dp),
                                        onSelect = { model ->
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
            }
        }
    }
}
