package me.rerere.rikkahub.ui.pages.setting

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Search01
import me.rerere.hugeicons.stroke.Settings03
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.components.ui.CardGroup

/**
 * 设置搜索条目：精确到**具体设置项**（不是页面级）
 *
 * - titleRes：设置项标题（命中目标）
 * - pageRes：所属页面（结果分组头 + 面包屑）
 * - screen：点击后跳转的目标路由（尽量精确到子页面）
 * - keywords：额外搜索关键词（中英文，供同义词命中）
 */
data class SettingSearchItem(
    @StringRes val titleRes: Int,
    @StringRes val pageRes: Int,
    val screen: Screen,
    val keywords: String = "",
)

/**
 * 设置搜索索引（新增设置项时，在此登记一行即可被搜索到）
 *
 * 注意：新增的字符串资源必须同时存在于 values/ 与 values-zh/。
 */
val SETTING_SEARCH_ITEMS: List<SettingSearchItem> = listOf(
    // ─── 偏好 · 常规 ───
    SettingSearchItem(R.string.setting_display_page_create_new_conversation_on_start_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "新对话 启动 launch start new chat"),
    SettingSearchItem(R.string.setting_display_page_send_on_enter_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "enter 回车 发送 换行 send"),
    SettingSearchItem(R.string.setting_display_page_show_message_jumper_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "jumper 跳转 导航 按钮"),
    SettingSearchItem(R.string.setting_display_page_message_jumper_position_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "jumper 位置 position 左侧 左"),
    SettingSearchItem(R.string.setting_display_page_enable_auto_scroll_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "滚动 到底 底部 scroll bottom"),
    SettingSearchItem(R.string.setting_display_page_use_app_icon_style_loading_indicator_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "loading 加载 指示器 spinner"),
    SettingSearchItem(R.string.setting_display_page_background_effect_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "背景 模糊 玻璃 blur glass effect 毛玻璃"),
    SettingSearchItem(R.string.setting_display_page_background_effect_type, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "背景效果 类型 type"),
    SettingSearchItem(R.string.setting_display_page_enable_message_generation_haptic_effect_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "触觉 震动 反馈 haptic vibrate"),
    SettingSearchItem(R.string.setting_display_page_skip_crop_image_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "图片 裁剪 编辑 crop image"),
    SettingSearchItem(R.string.setting_display_page_paste_long_text_as_file_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "长文本 粘贴 文件 附件 paste file"),
    SettingSearchItem(R.string.setting_display_page_paste_long_text_threshold_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "阈值 长度 threshold"),
    SettingSearchItem(R.string.setting_display_page_volume_key_scroll_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "音量键 翻页 墨水屏 volume key"),
    SettingSearchItem(R.string.setting_display_page_volume_key_scroll_ratio, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "滚动幅度 比例 ratio"),
    SettingSearchItem(R.string.setting_tts_page_default_playback_speed, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "播放速度 倍速 语速 speed rate"),
    SettingSearchItem(R.string.setting_display_page_tts_only_read_quoted_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "tts 朗读 引用 引号 quoted"),
    SettingSearchItem(R.string.setting_display_page_tts_read_outside_brackets_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "tts 括号 朗读 brackets"),
    SettingSearchItem(R.string.setting_display_page_auto_play_tts_title, R.string.setting_page_preferences_general, Screen.SettingPreferencesGeneral, "tts 朗读 自动播放 语音"),
    // ─── 偏好 · 界面 ───
    SettingSearchItem(R.string.setting_display_page_show_user_avatar_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "头像 avatar"),
    SettingSearchItem(R.string.setting_display_page_show_assistant_bubble_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "气泡 bubble 背景 助手"),
    SettingSearchItem(R.string.setting_display_page_bubble_opacity_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "气泡 bubble 透明度 不透明 opacity"),
    SettingSearchItem(R.string.setting_display_page_chat_list_model_icon_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "列表 模型 图标 icon"),
    SettingSearchItem(R.string.setting_display_page_show_model_name_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "模型 名称 name"),
    SettingSearchItem(R.string.setting_display_page_show_datetime_in_message_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "日期 时间 datetime 消息"),
    SettingSearchItem(R.string.setting_display_page_show_token_usage_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "token 上下文 统计 用量 context"),
    SettingSearchItem(R.string.setting_display_page_show_thinking_content_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "思考 推理 thinking reasoning"),
    SettingSearchItem(R.string.setting_display_page_auto_collapse_thinking_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "思考 折叠 collapse"),
    SettingSearchItem(R.string.setting_display_page_enable_latex_rendering_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "latex 公式 数学 math 渲染"),
    SettingSearchItem(R.string.setting_display_page_chat_font_family_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "字体 font 字型"),
    SettingSearchItem(R.string.setting_display_page_custom_font_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "自定义字体 font"),
    SettingSearchItem(R.string.setting_display_page_font_size_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "字体 大小 字号 font size"),
    SettingSearchItem(R.string.setting_display_page_code_block_auto_wrap_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "代码块 换行 code wrap"),
    SettingSearchItem(R.string.setting_display_page_code_block_auto_collapse_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "代码块 折叠 code collapse"),
    SettingSearchItem(R.string.setting_display_page_show_line_numbers_title, R.string.setting_page_preferences_ui, Screen.SettingPreferencesUI, "行号 代码 line number"),
    // ─── 偏好 · 主题 / 通知 / 网络 ───
    SettingSearchItem(R.string.setting_page_dynamic_color, R.string.setting_page_preferences_theme, Screen.SettingPreferencesTheme, "动态颜色 壁纸 color"),
    SettingSearchItem(R.string.setting_display_page_amoled_dark_mode_title, R.string.setting_page_preferences_theme, Screen.SettingPreferencesTheme, "amoled 暗色 深色 纯黑 dark black"),
    SettingSearchItem(R.string.setting_display_page_show_updates_title, R.string.setting_page_preferences_notification, Screen.SettingPreferencesNotification, "更新 update 提醒"),
    SettingSearchItem(R.string.setting_display_page_notification_message_generated, R.string.setting_page_preferences_notification, Screen.SettingPreferencesNotification, "通知 消息 生成 notification"),
    SettingSearchItem(R.string.setting_display_page_live_update_notification, R.string.setting_page_preferences_notification, Screen.SettingPreferencesNotification, "实时 通知 live notification"),
    SettingSearchItem(R.string.setting_page_preferences_network_auto_retry, R.string.setting_page_preferences_network, Screen.SettingPreferencesNetwork, "重试 retry 网络错误 失败"),
    // ─── 模型 ───
    SettingSearchItem(R.string.setting_model_page_chat_model, R.string.setting_model_page_title, Screen.SettingModels, "对话模型 主模型 chat model"),
    SettingSearchItem(R.string.setting_model_page_fast_model, R.string.setting_model_page_title, Screen.SettingModels, "快速模型 fast model"),
    SettingSearchItem(R.string.setting_model_page_compress_model, R.string.setting_model_page_title, Screen.SettingModels, "压缩 总结 上下文 compress context"),
    SettingSearchItem(R.string.setting_model_page_ocr_model, R.string.setting_model_page_title, Screen.SettingModels, "ocr 图片 文字识别 视觉"),
    SettingSearchItem(R.string.setting_model_page_translate_model, R.string.setting_model_page_title, Screen.SettingModels, "翻译 translate"),
    SettingSearchItem(R.string.setting_model_page_enable_suggestion, R.string.setting_model_page_title, Screen.SettingModels, "聊天建议 联想 suggestion"),
    SettingSearchItem(R.string.assistant_page_thinking_budget, R.string.setting_model_page_title, Screen.SettingModels, "思考预算 推理 effort thinking budget"),
    // ─── 提供商 ───
    SettingSearchItem(R.string.setting_page_providers, R.string.setting_provider_page_title, Screen.SettingProvider, "提供商 provider api 接口 密钥 key"),
    SettingSearchItem(R.string.setting_provider_page_add_provider, R.string.setting_provider_page_title, Screen.SettingProvider, "添加 提供商 add provider"),
    // ─── 搜索 ───
    SettingSearchItem(R.string.setting_page_search_title, R.string.setting_page_search_title, Screen.SettingSearch, "搜索服务 联网 web search 引擎"),
    SettingSearchItem(R.string.setting_page_search_result_size, R.string.setting_page_search_title, Screen.SettingSearch, "结果数量 条数 result size"),
    SettingSearchItem(R.string.search_detail_api_key, R.string.setting_page_search_title, Screen.SettingSearch, "api key 密钥"),
    SettingSearchItem(R.string.search_detail_api_url, R.string.setting_page_search_title, Screen.SettingSearch, "api url 接口地址"),
    SettingSearchItem(R.string.search_detail_engines, R.string.setting_page_search_title, Screen.SettingSearch, "引擎 engine"),
    SettingSearchItem(R.string.search_detail_max_tokens, R.string.setting_page_search_title, Screen.SettingSearch, "最大令牌 max tokens"),
    SettingSearchItem(R.string.search_detail_search_url, R.string.setting_page_search_title, Screen.SettingSearch, "搜索网址 链接 search url"),
    SettingSearchItem(R.string.search_detail_scrape_url, R.string.setting_page_search_title, Screen.SettingSearch, "抓取网址 scrape url"),
    // ─── 语音 ───
    SettingSearchItem(R.string.speech_page_title, R.string.speech_page_title, Screen.SettingSpeech, "语音 朗读 识别 tts asr 配音"),
    // ─── MCP ───
    SettingSearchItem(R.string.setting_mcp_page_title, R.string.setting_mcp_page_title, Screen.SettingMcp, "mcp 服务器 server 工具"),
    SettingSearchItem(R.string.setting_mcp_page_enable, R.string.setting_mcp_page_title, Screen.SettingMcp, "启用 开启 enable 服务器"),
    SettingSearchItem(R.string.setting_mcp_page_name, R.string.setting_mcp_page_title, Screen.SettingMcp, "名称 name 服务器"),
    SettingSearchItem(R.string.setting_mcp_page_transport_type, R.string.setting_mcp_page_title, Screen.SettingMcp, "传输类型 sse http transport"),
    SettingSearchItem(R.string.setting_mcp_page_server_url, R.string.setting_mcp_page_title, Screen.SettingMcp, "服务器地址 url 地址"),
    SettingSearchItem(R.string.setting_mcp_page_custom_headers, R.string.setting_mcp_page_title, Screen.SettingMcp, "请求头 headers 自定义"),
    // ─── Web ───
    SettingSearchItem(R.string.setting_page_web_server, R.string.setting_page_web_server, Screen.SettingWeb, "web 服务器 网页 局域网 lan"),
    SettingSearchItem(R.string.setting_page_web_server_port, R.string.setting_page_web_server, Screen.SettingWeb, "端口 port"),
    SettingSearchItem(R.string.setting_page_web_server_localhost_only, R.string.setting_page_web_server, Screen.SettingWeb, "localhost 本地主机 局域网 lan"),
    SettingSearchItem(R.string.setting_page_web_server_jwt_enable, R.string.setting_page_web_server, Screen.SettingWeb, "身份验证 认证 密码 auth jwt"),
    SettingSearchItem(R.string.setting_page_web_server_password, R.string.setting_page_web_server, Screen.SettingWeb, "访问密码 password"),
    // ─── 文件 / 关于 ───
    SettingSearchItem(R.string.setting_page_chat_storage, R.string.setting_files_page_title, Screen.SettingFiles, "聊天存储 文件 清理 缓存 files storage"),
    SettingSearchItem(R.string.about_page_version, R.string.about_page_title, Screen.SettingAbout, "版本 version 号"),
    SettingSearchItem(R.string.about_page_license, R.string.about_page_title, Screen.SettingAbout, "license 许可 开源"),
)

/** 按关键词搜索设置项（标题本地化文本 + 关键词，忽略大小写） */
fun searchSettingItems(context: Context, query: String): List<SettingSearchItem> {
    val q = query.trim().lowercase()
    if (q.isBlank()) return emptyList()
    return SETTING_SEARCH_ITEMS.filter { item ->
        context.getString(item.titleRes).lowercase().contains(q) ||
            item.keywords.lowercase().contains(q)
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

/**
 * 设置搜索结果
 *
 * 按页面分组展示：组头 = 页面名 + 命中数；每一项标题中**高亮关键词**。
 */
@Composable
fun SettingSearchResults(
    query: String,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val results = remember(query) { searchSettingItems(context, query) }

    if (results.isEmpty()) {
        // 空状态 / 无结果
        CardGroup(modifier = modifier.padding(horizontal = 8.dp)) {
            item(
                headlineContent = { Text(stringResource(R.string.setting_search_no_result)) },
                supportingContent = { Text(stringResource(R.string.setting_search_suggestion)) },
            )
        }
        return
    }

    // 按目标页面分组（groupBy 保持索引顺序）
    results.groupBy { it.screen }.forEach { (screen, items) ->
        val pageTitle = stringResource(items.first().pageRes)
        CardGroup(
            modifier = modifier.padding(horizontal = 8.dp),
            title = {
                Text("$pageTitle · ${items.size}")
            },
        ) {
            items.forEach { item ->
                item(
                    onClick = { onNavigate(item.screen) },
                    leadingContent = {
                        Icon(
                            imageVector = HugeIcons.Settings03,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    headlineContent = {
                        HighlightedText(
                            text = stringResource(item.titleRes),
                            query = query,
                        )
                    },
                )
            }
        }
    }
}

/** 高亮命中的关键词 */
@Composable
private fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
) {
    val q = query.trim()
    if (q.isEmpty()) {
        Text(text = text, modifier = modifier)
        return
    }
    val index = text.indexOf(q, ignoreCase = true)
    if (index < 0) {
        Text(text = text, modifier = modifier)
        return
    }
    Text(
        modifier = modifier,
        text = buildAnnotatedString {
            append(text.substring(0, index))
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            ) {
                append(text.substring(index, index + q.length))
            }
            append(text.substring(index + q.length))
        },
        color = Color.Unspecified,
    )
}
