package me.rerere.rikkahub.ui.pages.setting

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Search01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.components.ui.CardGroup

/**
 * 设置搜索条目：标题 + 额外关键词（中英）+ 目标页面
 */
data class SettingSearchEntry(
    @StringRes val titleRes: Int,
    val keywords: String,
    val screen: Screen,
)

/**
 * 设置搜索索引
 *
 * 新增设置页面时，在此登记一行即可被搜索到。
 * keywords 里放中英文关键词，方便中英文输入都能命中。
 */
val SETTING_SEARCH_ENTRIES: List<SettingSearchEntry> = listOf(
    // 通用
    SettingSearchEntry(R.string.setting_page_general_settings, "设置 settings 通用 general 基础 basic", Screen.Setting),
    SettingSearchEntry(R.string.setting_page_preferences, "偏好 preferences 界面 ui 外观 appearance 主题 theme 颜色 color 深色 dark 浅色 light 语言 language", Screen.SettingPreferences),
    SettingSearchEntry(R.string.setting_page_assistant, "助手 assistant 角色 role 提示词 prompt", Screen.Assistant),
    // 扩展管理
    SettingSearchEntry(R.string.setting_page_extensions, "扩展管理 extensions 快捷消息 quick messages 提示词 prompts 技能 skills 工作区 workspace 工具调用 tool calls 工作流 workflows", Screen.Extensions),
    // 模型与服务
    SettingSearchEntry(R.string.setting_page_default_model, "默认模型 default model 对话模型 chat model 快速模型 fast model", Screen.SettingModels),
    SettingSearchEntry(R.string.setting_page_providers, "提供商 providers api 密钥 key 接口 endpoint 余额 balance", Screen.SettingProvider),
    SettingSearchEntry(R.string.setting_page_search_service, "搜索服务 search service 联网 web search 引擎 engine", Screen.SettingSearch),
    SettingSearchEntry(R.string.setting_page_tts_service, "语音服务 tts speech 朗读 read aloud 语音识别 asr", Screen.SettingSpeech),
    SettingSearchEntry(R.string.setting_page_media, "媒体生成 media generation 图片 image 绘图", Screen.SettingMedia),
    SettingSearchEntry(R.string.setting_page_mcp, "mcp 服务器 server 工具 tools", Screen.SettingMcp),
    SettingSearchEntry(R.string.setting_page_web_server, "web 服务器 web server 局域网 lan 端口 port", Screen.SettingWeb),
    // 数据
    SettingSearchEntry(R.string.setting_page_data_backup, "备份 backup 恢复 restore webdav s3 同步 sync", Screen.Backup),
    SettingSearchEntry(R.string.setting_page_chat_storage, "聊天存储 chat storage 文件 files 清理 clean 缓存 cache", Screen.SettingFiles),
    // 关于
    SettingSearchEntry(R.string.setting_page_about, "关于 about 版本 version 许可证 license github", Screen.SettingAbout),
)

/** 按关键词搜索设置项（标题本地化文本 + 关键词，忽略大小写） */
fun searchSettingEntries(context: Context, query: String): List<SettingSearchEntry> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return emptyList()
    return SETTING_SEARCH_ENTRIES.filter { entry ->
        context.getString(entry.titleRes).lowercase().contains(q) ||
            entry.keywords.lowercase().contains(q)
    }
}

/** 设置页顶部的搜索框 */
@Composable
fun SettingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        placeholder = { Text(stringResource(R.string.setting_search_placeholder)) },
        leadingIcon = {
            Icon(HugeIcons.Search01, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = HugeIcons.Cancel01,
                        contentDescription = stringResource(R.string.setting_search_clear),
                    )
                }
            }
        },
        singleLine = true,
    )
}

/** 设置搜索结果列表 */
@Composable
fun SettingSearchResults(
    query: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val results = remember(query) { searchSettingEntries(context, query) }
    CardGroup(
        modifier = modifier.padding(horizontal = 8.dp),
        title = { Text(stringResource(R.string.setting_search_results)) },
    ) {
        if (results.isEmpty()) {
            item(
                headlineContent = { Text(stringResource(R.string.setting_search_no_result)) },
            )
        }
        results.forEach { entry ->
            item(
                onClick = { onNavigate(entry.screen) },
                headlineContent = { Text(stringResource(entry.titleRes)) },
            )
        }
    }
}
