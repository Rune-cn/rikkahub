package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Code
import me.rerere.hugeicons.stroke.Earth
import me.rerere.hugeicons.stroke.File02
import me.rerere.hugeicons.stroke.Github
import me.rerere.hugeicons.stroke.Refresh
import me.rerere.hugeicons.stroke.SmartPhone01
import me.rerere.rikkahub.BuildConfig
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.CardGroupIcon
import me.rerere.rikkahub.ui.components.ui.drawAppLogo
import me.rerere.rikkahub.ui.components.ui.logoMorphShape
import me.rerere.rikkahub.ui.components.ui.rememberAppLogoLayers
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.SoundEffectPlayer
import me.rerere.rikkahub.utils.RunLog
import me.rerere.rikkahub.utils.UiState
import me.rerere.rikkahub.utils.UpdateChecker
import me.rerere.rikkahub.utils.UpdateInfo
import me.rerere.rikkahub.utils.openUrl
import me.rerere.rikkahub.utils.plus
import me.rerere.ui.easteregg.EmojiBurstHost
import me.rerere.ui.toast.ToastType
import org.koin.compose.koinInject
import kotlinx.coroutines.launch

@Composable
fun SettingAboutPage() {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val context = LocalContext.current
    val navController = LocalNavController.current
    val soundOptions = remember { listOf(R.raw.bingbingbing, R.raw.gangguan) }
    val soundEffectPlayer = remember(context) { SoundEffectPlayer(context) }
    val updateChecker = koinInject<UpdateChecker>()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    // 手动「检查更新」的结果（Success 时弹窗；Idle/Error 走 Toast 后置空）
    var manualUpdate by remember { mutableStateOf<UiState<UpdateInfo>?>(null) }
    DisposableEffect(soundEffectPlayer) {
        soundEffectPlayer.preload(*soundOptions.toIntArray())
        onDispose {
            soundEffectPlayer.release()
        }
    }
    val emojiOptions = remember {
        listOf(
            "🎉", "✨", "🌟", "💫", "🎊", "🥳", "🎈", "🎆", "🎇", "🧨",
            "🌈", "🧧", "🎁", "🍬", "🍭", "🍉", "🍓", "🍒", "🍍", "🥭",
            "🐱", "🐶", "🦊", "🐼", "🦁", "🐯", "🐵", "🦄",
            "❤️", "🧡", "💛", "💚", "💙", "💜",
            "🇨🇳", "🌏", "🌍", "🌎",
            "🤗", "🤩", "😆", "😺", "😸", "🤡",
            "💡", "🔥", "💥", "🚀", "⭐", "🌙"
        )
    }
    val logoLayers = rememberAppLogoLayers()
    // 表情从 logo 中心喷出，坐标要换算到 EmojiBurstHost 里
    var hostCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var logoCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.about_page_title))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor,
    ) { innerPadding ->
        EmojiBurstHost(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { hostCoordinates = it },
            emojiOptions = emojiOptions,
            burstCount = 12
        ) { onBurst ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding + PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    val interactionSource = remember { MutableInteractionSource() }
                    val pressed by interactionSource.collectIsPressedAsState()
                    val logoScale by animateFloatAsState(
                        targetValue = if (pressed) 0.88f else 1f,
                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                    )
                    // 每点一次，轮廓变形到下一个形状
                    var shapeIndex by remember { mutableIntStateOf(0) }
                    val shapePosition by animateFloatAsState(
                        targetValue = shapeIndex.toFloat(),
                        animationSpec = MaterialTheme.motionScheme.slowSpatialSpec(),
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp, end = 8.dp, bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .semantics { contentDescription = "Logo" }
                                .onGloballyPositioned { logoCoordinates = it }
                                .graphicsLayer {
                                    scaleX = logoScale
                                    scaleY = logoScale
                                    shape = logoMorphShape(shapePosition)
                                    clip = true
                                }
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = LocalIndication.current,
                                ) {
                                    val host = hostCoordinates
                                    val logo = logoCoordinates
                                    if (host != null && logo != null && host.isAttached && logo.isAttached) {
                                        onBurst(host.localPositionOf(logo, logo.size.center.toOffset()))
                                    }
                                    soundEffectPlayer.play(soundOptions.random())
                                    shapeIndex++
                                }
                                .drawAppLogo(logoLayers)
                        )

                        Text(
                            text = "RikkaHub",
                            style = MaterialTheme.typography.headlineLargeEmphasized.copy(
                                fontWeight = FontWeight.Black,
                            ),
                        )
                    }
                }

                item {
                    val containerColor = MaterialTheme.colorScheme.primaryContainer
                    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    CardGroup {
                        item(
                            modifier = Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = { navController.navigate(Screen.Debug) },
                            ),
                            leadingContent = { CardGroupIcon(HugeIcons.Code, containerColor = containerColor, contentColor = contentColor) },
                            supportingContent = {
                                Text("${BuildConfig.VERSION_NAME} / ${BuildConfig.VERSION_CODE}")
                            },
                            headlineContent = { Text(stringResource(R.string.about_page_version)) },
                        )
                        item(
                            onClick = {
                                scope.launch {
                                    manualUpdate = UiState.Loading
                                    RunLog.log("UpdateCheck", "手动检查更新: 开始")
                                    val result = updateChecker.checkNowOnce()
                                    RunLog.log(
                                        "UpdateCheck",
                                        "手动检查更新结果: " + when (result) {
                                            is UiState.Success -> "发现新版本 ${result.data.version}（${result.data.downloads.size} 个资产）"
                                            is UiState.Idle -> "已最新/不提示"
                                            is UiState.Error -> "失败: ${result.error.message}"
                                            UiState.Loading -> "Loading"
                                        },
                                    )
                                    manualUpdate = when (result) {
                                        is UiState.Success -> result
                                        is UiState.Error -> {
                                            toaster.show(
                                                result.error.message ?: context.getString(R.string.update_card_check_failed),
                                                type = ToastType.Error,
                                            )
                                            null
                                        }
                                        is UiState.Idle -> {
                                            toaster.show(context.getString(R.string.about_page_up_to_date), type = ToastType.Info)
                                            null
                                        }
                                        UiState.Loading -> null
                                    }
                                }
                            },
                            leadingContent = { CardGroupIcon(HugeIcons.Refresh, containerColor = containerColor, contentColor = contentColor) },
                            supportingContent = { Text(stringResource(R.string.about_page_check_update_desc)) },
                            headlineContent = { Text(stringResource(R.string.about_page_check_update)) },
                        )
                        item(
                            leadingContent = { CardGroupIcon(HugeIcons.SmartPhone01, containerColor = containerColor, contentColor = contentColor) },
                            supportingContent = {
                                Text("${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} / Android ${android.os.Build.VERSION.RELEASE} / SDK ${android.os.Build.VERSION.SDK_INT}")
                            },
                            headlineContent = { Text(stringResource(R.string.about_page_system)) },
                        )
                    }
                }

                item {
                    val containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    val contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    CardGroup {
                        item(
                            onClick = { context.openUrl("https://github.com/Rune-cn/rikkahub") },
                            leadingContent = { CardGroupIcon(HugeIcons.Github, containerColor = containerColor, contentColor = contentColor) },
                            supportingContent = { Text("https://github.com/Rune-cn/rikkahub") },
                            headlineContent = { Text(stringResource(R.string.about_page_github)) },
                        )
                        item(
                            onClick = { context.openUrl("https://github.com/Rune-cn/rikkahub/blob/master/LICENSE") },
                            leadingContent = { CardGroupIcon(HugeIcons.File02, containerColor = containerColor, contentColor = contentColor) },
                            supportingContent = { Text("https://github.com/Rune-cn/rikkahub/blob/master/LICENSE") },
                            headlineContent = { Text(stringResource(R.string.about_page_license)) },
                        )
                    }
                }
            }
        }
    }

    // 手动「检查更新」的结果弹窗（Success 时展示版本+更新内容+下载按钮）
    val updateInfo = (manualUpdate as? UiState.Success)?.data
    if (updateInfo != null) {
        AlertDialog(
            onDismissRequest = { manualUpdate = null },
            title = { Text(stringResource(R.string.update_card_new_version_found, updateInfo.version)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (updateInfo.changelog.isNotBlank()) {
                        Text(updateInfo.changelog, style = MaterialTheme.typography.bodyMedium)
                    }
                    if (updateInfo.downloads.isEmpty()) {
                        Text(stringResource(R.string.about_page_check_update_no_asset))
                    } else {
                        updateInfo.downloads.forEach { download ->
                            Text(
                                download.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (updateInfo.downloads.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            updateChecker.downloadUpdate(context, updateInfo.downloads.first())
                            manualUpdate = null
                            toaster.show(context.getString(R.string.update_card_downloading), type = ToastType.Info)
                        }
                    ) {
                        Text(stringResource(R.string.about_page_download))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { manualUpdate = null }) {
                    Text(stringResource(R.string.update_card_close))
                }
            },
        )
    }
}

