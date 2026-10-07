package me.rerere.rikkahub.ui.pages.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.RetryMode
import me.rerere.rikkahub.data.datastore.SettingsStore
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