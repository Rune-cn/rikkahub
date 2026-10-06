package me.rerere.rikkahub.ui.pages.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import me.rerere.hugeicons.HugeIcons
import me.rerere.rikkahub.R
import me.rerere.hugeicons.stroke.Book03
import me.rerere.hugeicons.stroke.File02
import me.rerere.hugeicons.stroke.Folder01
import me.rerere.hugeicons.stroke.Puzzle
import me.rerere.hugeicons.stroke.Zap
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.RetryMode
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.compose.koinInject

@Composable
fun ExtensionsPage() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
    val settingsStore = koinInject<SettingsStore>()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.extensions_page_title)) },
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
                    title = { Text(stringResource(R.string.extensions_page_section_extensions)) },
                ) {
                    item(
                        onClick = { navController.navigate(Screen.QuickMessages) },
                        leadingContent = { Icon(HugeIcons.Zap, null) },
                        headlineContent = { Text(stringResource(R.string.assistant_page_quick_messages)) },
                        supportingContent = { Text(stringResource(R.string.extensions_page_quick_messages_desc)) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.Prompts) },
                        leadingContent = { Icon(HugeIcons.Book03, null) },
                        headlineContent = { Text(stringResource(R.string.extensions_page_prompts)) },
                        supportingContent = { Text(stringResource(R.string.extensions_page_prompts_desc)) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.Skills) },
                        leadingContent = { Icon(HugeIcons.Puzzle, null) },
                        headlineContent = { Text(stringResource(R.string.extensions_page_agent_skills)) },
                        supportingContent = { Text(stringResource(R.string.extensions_page_agent_skills_desc)) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.Workspaces) },
                        leadingContent = { Icon(HugeIcons.Folder01, null) },
                        headlineContent = { Text(stringResource(R.string.extensions_page_workspace)) },
                        supportingContent = { Text(stringResource(R.string.extensions_page_workspace_desc)) },
                    )
                }
            }

            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text(stringResource(R.string.extensions_page_other_category)) },
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
                }
            }
        }
    }
}