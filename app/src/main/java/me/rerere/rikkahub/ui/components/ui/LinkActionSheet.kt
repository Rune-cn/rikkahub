package me.rerere.rikkahub.ui.components.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Copy01
import me.rerere.hugeicons.stroke.Earth
import me.rerere.hugeicons.stroke.Share03
import me.rerere.hugeicons.stroke.WebDesign01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.utils.openUrl
import me.rerere.rikkahub.utils.writeClipboardText

/**
 * 链接操作面板：在浏览器打开 / 内嵌打开 / 分享
 *
 * 由调用方持有 [url] 状态，为 null 时不显示。
 */
@Composable
fun LinkActionSheet(
    url: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val navController = LocalNavController.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = url,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            val actions = listOf(
                Triple(
                    HugeIcons.Earth,
                    stringResource(R.string.link_action_open_browser),
                    {
                        context.openUrl(url)
                        onDismiss()
                    },
                ),
                Triple(
                    HugeIcons.WebDesign01,
                    stringResource(R.string.link_action_open_in_app),
                    {
                        navController.navigate(Screen.WebView(url = url))
                        onDismiss()
                    },
                ),
                Triple(
                    HugeIcons.Copy01,
                    stringResource(R.string.link_action_copy_link),
                    {
                        context.writeClipboardText(url)
                        onDismiss()
                    },
                ),
                Triple(
                    HugeIcons.Share03,
                    stringResource(R.string.link_action_share_link),
                    {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(intent, null))
                        }
                        onDismiss()
                    },
                ),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                modifier = Modifier.fillMaxWidth(),
            ) {
                actions.fastForEachIndexed { index, action ->
                    SegmentedListItem(
                        onClick = action.third,
                        shapes = ListItemDefaults.segmentedShapes(index = index, count = actions.size),
                        colors = ListItemDefaults.segmentedColors(),
                        leadingContent = {
                            Icon(
                                imageVector = action.first,
                                contentDescription = null,
                            )
                        },
                    ) {
                        Text(action.second)
                    }
                }
            }
        }
    }
}