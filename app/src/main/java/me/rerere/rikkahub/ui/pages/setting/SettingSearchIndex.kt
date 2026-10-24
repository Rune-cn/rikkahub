package me.rerere.rikkahub.ui.pages.setting

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowDown01
import me.rerere.hugeicons.stroke.ArrowRight01
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.LeftToRightListBullet
import me.rerere.hugeicons.stroke.PencilEdit01
import me.rerere.hugeicons.stroke.Search01
import me.rerere.hugeicons.stroke.SlidersHorizontal
import me.rerere.hugeicons.stroke.Zap
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.CardGroupIcon

/** 设置项类型（决定图标，也便于后续做"定位+高亮"） */
enum class SettingItemType {
    /** 开关 */
    SWITCH,

    /** 单选/选择器 */
    SELECT,

    /** 输入框 */
    INPUT,

    /** 跳转到子页面 */
    NAV,

    /** 操作按钮（启动/清空等） */
    ACTION,
}

/**
 * 设置搜索条目：精确到**具体设置项**
 *
 * - [pageRes]：所属页面标题（搜索结果按它分组，形成"页面"层级）
 * - [titleRes]：设置项标题（关键词匹配 + 高亮显示）
 * - [screen]：点击后跳转的页面
 */
data class SettingSearchItem(
    @StringRes val pageRes: Int,
    @StringRes val titleRes: Int,
    val type: SettingItemType,
    val keywords: String,
    val screen: Screen,
)

/** 页面标题资源（面包屑分组名） */
private val PAGE_PREFERENCES = R.string.setting_page_preferences
private val PAGE_UI = R.string.setting_page_preferences_ui
private val PAGE_GENERAL = R.string.setting_page_preferences_general
private val PAGE_THEME = R.string.setting_page_preferences_theme
private val PAGE_MODEL = R.string.setting_page_default_model
private val PAGE_SEARCH = R.string.setting_page_search_service
private val PAGE_SPEECH = R.string.setting_page_tts_service
private val PAGE_MEDIA = R.string.setting_page_media
private val PAGE_MCP = R.string.setting_page_mcp
private val PAGE_WEB = R.string.setting_page_web_server
private val PAGE_FILES = R.string.setting_page_chat_storage
private val PAGE_EXTENSIONS = R.string.setting_page_extensions

/**
 * 设置搜索索引（精确到项）
 *
 * 新增设置项时在此登记一行即可被搜到。
 * keywords 放中英文关键词，中英文输入都能命中。
 */
val SETTING_SEARCH_ITEMS: List<SettingSearchItem> = listOf(
    // ── 偏好设置入口 ──
    SettingSearchItem(PAGE_PREFERENCES, R.string.setting_page_preferences_ui, SettingItemType.NAV, "界面 显示 ui display 字体 气泡", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_PREFERENCES, R.string.setting_page_preferences_general, SettingItemType.NAV, "通用 发送 滚动 震动 键盘", Screen.SettingPreferencesGeneral),
    SettingSearchItem(PAGE_PREFERENCES, R.string.setting_page_preferences_theme, SettingItemType.NAV, "主题 外观 颜色 深色 纯黑", Screen.SettingPreferencesTheme),
    SettingSearchItem(PAGE_PREFERENCES, R.string.setting_page_preferences_network, SettingItemType.NAV, "网络 代理 超时 network proxy timeout", Screen.SettingPreferencesNetwork),
    SettingSearchItem(PAGE_PREFERENCES, R.string.setting_page_preferences_notification, SettingItemType.NAV, "通知 提醒 notification", Screen.SettingPreferencesNotification),

    // ── 界面设置 ──
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_bubble_opacity_title, SettingItemType.INPUT, "气泡 bubble 透明度 opacity 背景", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_font_size_title, SettingItemType.INPUT, "字体 字号 font size 文字", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_chat_font_family_title, SettingItemType.SELECT, "正文字体 字体 chat font family 等宽 serif", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_custom_font_title, SettingItemType.INPUT, "自定义字体 导入 custom font ttf", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_show_assistant_bubble_title, SettingItemType.SWITCH, "助手气泡 气泡样式 assistant bubble", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_show_datetime_in_message_title, SettingItemType.SWITCH, "时间戳 消息时间 datetime time", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_show_thinking_content_title, SettingItemType.SWITCH, "思考 推理 thinking reasoning", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_show_token_usage_title, SettingItemType.SWITCH, "token 用量 token usage 消耗", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_show_model_name_title, SettingItemType.SWITCH, "模型名 model name 显示模型", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_chat_list_model_icon_title, SettingItemType.SWITCH, "ai 头像 模型图标 model icon avatar", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_show_user_avatar_title, SettingItemType.SWITCH, "用户头像 user avatar 头像", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_show_line_numbers_title, SettingItemType.SWITCH, "行号 代码 line number code", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_code_block_auto_collapse_title, SettingItemType.SWITCH, "代码块 折叠 code collapse", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_code_block_auto_wrap_title, SettingItemType.SWITCH, "代码 换行 code wrap", Screen.SettingPreferencesUI),
    SettingSearchItem(PAGE_UI, R.string.setting_display_page_enable_latex_rendering_title, SettingItemType.SWITCH, "latex 数学公式 渲染 math formula", Screen.SettingPreferencesUI),

    // ── 通用设置 ──
    SettingSearchItem(PAGE_GENERAL, R.string.setting_display_page_send_on_enter_title, SettingItemType.SWITCH, "回车发送 发送键 enter send key", Screen.SettingPreferencesGeneral),
    SettingSearchItem(PAGE_GENERAL, R.string.setting_display_page_enable_auto_scroll_title, SettingItemType.SWITCH, "自动滚动 auto scroll 滚到底", Screen.SettingPreferencesGeneral),
    SettingSearchItem(PAGE_GENERAL, R.string.setting_display_page_show_message_jumper_title, SettingItemType.SWITCH, "消息跳转 跳转按钮 message jumper", Screen.SettingPreferencesGeneral),
    SettingSearchItem(PAGE_GENERAL, R.string.setting_display_page_enable_message_generation_haptic_effect_title, SettingItemType.SWITCH, "震动 振动 haptic 反馈", Screen.SettingPreferencesGeneral),
    SettingSearchItem(PAGE_GENERAL, R.string.setting_display_page_background_effect_title, SettingItemType.SELECT, "背景 毛玻璃 模糊 background glass blur", Screen.SettingPreferencesGeneral),
    SettingSearchItem(PAGE_GENERAL, R.string.setting_display_page_create_new_conversation_on_start_title, SettingItemType.SWITCH, "新会话 启动 new conversation", Screen.SettingPreferencesGeneral),
    SettingSearchItem(PAGE_GENERAL, R.string.setting_display_page_paste_long_text_as_file_title, SettingItemType.SWITCH, "粘贴 长文本 文件 paste long text file", Screen.SettingPreferencesGeneral),
    SettingSearchItem(PAGE_GENERAL, R.string.setting_display_page_volume_key_scroll_title, SettingItemType.SWITCH, "音量键 滚动 volume key scroll", Screen.SettingPreferencesGeneral),
    SettingSearchItem(PAGE_GENERAL, R.string.setting_display_page_auto_play_tts_title, SettingItemType.SWITCH, "自动播放 tts 朗读 auto play", Screen.SettingPreferencesGeneral),
    SettingSearchItem(PAGE_GENERAL, R.string.setting_display_page_tts_read_outside_brackets_title, SettingItemType.SWITCH, "朗读 括号外 tts brackets", Screen.SettingPreferencesGeneral),
    SettingSearchItem(PAGE_GENERAL, R.string.setting_display_page_tts_only_read_quoted_title, SettingItemType.SWITCH, "朗读 引用 quoted tts", Screen.SettingPreferencesGeneral),

    // ── 主题设置 ──
    SettingSearchItem(PAGE_THEME, R.string.setting_page_dynamic_color, SettingItemType.SWITCH, "动态取色 主题色 dynamic color material", Screen.SettingPreferencesTheme),
    SettingSearchItem(PAGE_THEME, R.string.setting_display_page_amoled_dark_mode_title, SettingItemType.SWITCH, "纯黑 深色 amoled dark oled", Screen.SettingPreferencesTheme),

    // ── 模型 ──
    SettingSearchItem(PAGE_MODEL, R.string.setting_model_page_chat_model, SettingItemType.SELECT, "对话模型 chat 主模型 默认", Screen.SettingModels),
    SettingSearchItem(PAGE_MODEL, R.string.setting_model_page_fast_model, SettingItemType.SELECT, "快速模型 fast 轻量 快", Screen.SettingModels),
    SettingSearchItem(PAGE_MODEL, R.string.setting_model_page_translate_model, SettingItemType.SELECT, "翻译模型 translate 翻译", Screen.SettingModels),
    SettingSearchItem(PAGE_MODEL, R.string.setting_model_page_ocr_model, SettingItemType.SELECT, "ocr 模型 图片识别", Screen.SettingModels),

    // ── 搜索服务 ──
    SettingSearchItem(PAGE_SEARCH, R.string.setting_page_search_add_provider, SettingItemType.NAV, "搜索提供商 provider 引擎 engine 添加", Screen.SettingSearch),
    SettingSearchItem(PAGE_SEARCH, R.string.setting_page_search_result_size, SettingItemType.SELECT, "结果数量 条数 result size 每页", Screen.SettingSearch),

    // ── 语音服务 ──
    SettingSearchItem(PAGE_SPEECH, R.string.setting_tts_page_providers_label, SettingItemType.NAV, "tts 朗读提供商 voice 配音 provider", Screen.SettingSpeech),
    SettingSearchItem(PAGE_SPEECH, R.string.setting_asr_page_providers_label, SettingItemType.NAV, "asr 语音识别 转写 provider", Screen.SettingSpeech),

    // ── 媒体生成 ──
    SettingSearchItem(PAGE_MEDIA, R.string.setting_media_page_providers, SettingItemType.NAV, "图片 绘图 生成 提供商 模型 media", Screen.SettingMedia),

    // ── MCP ──
    SettingSearchItem(PAGE_MCP, R.string.setting_mcp_page_add_header, SettingItemType.NAV, "mcp 服务器 工具 servers 添加", Screen.SettingMcp),
    SettingSearchItem(PAGE_MCP, R.string.setting_mcp_page_enable, SettingItemType.SWITCH, "mcp 启用 enable", Screen.SettingMcp),

    // ── Web 服务器 ──
    SettingSearchItem(PAGE_WEB, R.string.setting_page_web_server_start, SettingItemType.ACTION, "web 启动 开启 start server", Screen.SettingWeb),
    SettingSearchItem(PAGE_WEB, R.string.setting_page_web_server_port, SettingItemType.INPUT, "web 端口 port", Screen.SettingWeb),
    SettingSearchItem(PAGE_WEB, R.string.setting_page_web_server_password, SettingItemType.INPUT, "web 密码 password 认证", Screen.SettingWeb),
    SettingSearchItem(PAGE_WEB, R.string.setting_page_web_server_jwt_enable, SettingItemType.SWITCH, "web jwt 令牌 token", Screen.SettingWeb),
    SettingSearchItem(PAGE_WEB, R.string.setting_page_web_server_localhost_only, SettingItemType.SWITCH, "web 本机 localhost 仅本机", Screen.SettingWeb),

    // ── 数据与存储 ──
    SettingSearchItem(PAGE_FILES, R.string.setting_files_page_clean_title, SettingItemType.ACTION, "清理 文件 缓存 clean storage 清理空间", Screen.SettingFiles),


    // ── 通用设置 ──
    SettingSearchItem(PAGE_GENERAL, R.string.setting_page_color_mode, SettingItemType.SELECT, "颜色模式 主题 深浅色 深色 浅色 跟随系统 暗色 color mode theme dark light system", Screen.Setting),
    SettingSearchItem(PAGE_GENERAL, R.string.setting_page_preferences, SettingItemType.NAV, "偏好设置 界面 外观 preferences", Screen.SettingPreferences),
    // ── 扩展管理入口（设置搜索可直接直达功能页） ──
    SettingSearchItem(PAGE_EXTENSIONS, R.string.workflow_page_title, SettingItemType.NAV, "工作流 workflow 流水线 步骤 自动化 pipeline", Screen.Workflows),
    SettingSearchItem(PAGE_EXTENSIONS, R.string.extensions_page_sub_agent_section, SettingItemType.NAV, "子 agent 多 agent 协同 并行 分派 sub-agent delegation spawn", Screen.OtherSettings),
    SettingSearchItem(PAGE_EXTENSIONS, R.string.extensions_page_sub_agent_model, SettingItemType.SELECT, "子 agent 模型 默认模型 sub-agent model", Screen.OtherSettings),
    SettingSearchItem(PAGE_EXTENSIONS, R.string.extensions_page_auto_retry, SettingItemType.NAV, "自动重试 限流 重试 网络错误 状态码 retry rate limit tpm rpm", Screen.OtherSettings),
    SettingSearchItem(PAGE_EXTENSIONS, R.string.assistant_page_quick_messages, SettingItemType.NAV, "快捷消息 快捷指令 quick messages 一键发送", Screen.QuickMessages),
    SettingSearchItem(PAGE_EXTENSIONS, R.string.extensions_page_prompts, SettingItemType.NAV, "提示词 预设 模板 prompt templates", Screen.Prompts),
    SettingSearchItem(PAGE_EXTENSIONS, R.string.extensions_page_agent_skills, SettingItemType.NAV, "技能 能力 skill agent skills", Screen.Skills),
    SettingSearchItem(PAGE_EXTENSIONS, R.string.extensions_page_workspace, SettingItemType.NAV, "工作区 文件 命令行 终端 workspace files terminal", Screen.Workspaces),
    SettingSearchItem(PAGE_EXTENSIONS, R.string.tool_call_page_title, SettingItemType.NAV, "工具调用 自动允许 审批 tool calls auto allow approval", Screen.ToolCalls),
    SettingSearchItem(PAGE_EXTENSIONS, R.string.extensions_page_other_category, SettingItemType.NAV, "其他 更多 other 子 agent 自动重试", Screen.OtherSettings),

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

/**
 * 设置搜索输入框（圆角胶囊样式），供顶栏搜索态使用。
 *
 * 顶栏收起后完全恢复原样（不占观感）；展开时自动聚焦弹键盘，输入即搜；
 * 点向下箭头或键盘「搜索」由 [onCollapse] 收起（同时清空关键词与结果区）。
 */
@Composable
fun SettingSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (focusRequester != null) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(percent = 50),
        placeholder = { Text(stringResource(R.string.setting_search_placeholder)) },
        leadingIcon = { Icon(HugeIcons.Search01, contentDescription = null) },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = HugeIcons.Cancel01,
                        contentDescription = stringResource(R.string.setting_search_clear),
                    )
                }
            } else {
                IconButton(onClick = onCollapse) {
                    Icon(
                        imageVector = HugeIcons.ArrowDown01,
                        contentDescription = stringResource(R.string.setting_search_collapse),
                    )
                }
            }
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
        // 键盘右下角「搜索」= 完成输入（只收起键盘，结果区保留可点选）；彻底收起靠右侧 ↓
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
    )
}

/**
 * 设置搜索结果：按「页面」分组，每组渲染成与设置页一致的原生卡片组（CardGroup）。
 *
 * 组内每项：圆底类型图标 + 命中词高亮标题，点击直达对应设置页。
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
        Text(
            text = stringResource(R.string.setting_search_no_result),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(horizontal = 16.dp, vertical = 24.dp),
        )
        return
    }
    // 按页面分组（保留索引顺序），每组就是一张原生的整卡片
    val grouped = LinkedHashMap<Int, MutableList<SettingSearchItem>>()
    results.forEach { item -> grouped.getOrPut(item.pageRes) { mutableListOf() }.add(item) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
    ) {
        grouped.forEach { (pageRes, items) ->
            CardGroup(
                title = { Text(stringResource(pageRes)) },
            ) {
                items.forEach { item ->
                    item(
                        onClick = { onNavigate(item.screen) },
                        leadingContent = {
                            CardGroupIcon(
                                icon = settingItemIcon(item.type),
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        headlineContent = {
                            Text(
                                text = highlightKeyword(
                                    text = context.getString(item.titleRes),
                                    query = query,
                                )
                            )
                        },
                    )
                }
            }
        }
    }
}

/** 设置项类型 → 图标（结果卡片圆底用） */
private fun settingItemIcon(type: SettingItemType): androidx.compose.ui.graphics.vector.ImageVector = when (type) {
    SettingItemType.SWITCH -> HugeIcons.SlidersHorizontal
    SettingItemType.SELECT -> HugeIcons.LeftToRightListBullet
    SettingItemType.INPUT -> HugeIcons.PencilEdit01
    SettingItemType.NAV -> HugeIcons.ArrowRight01
    SettingItemType.ACTION -> HugeIcons.Zap
}

/** 关键词高亮：命中词加粗 + 主题色 */
@Composable
private fun highlightKeyword(text: String, query: String): androidx.compose.ui.text.AnnotatedString {
    val q = query.trim()
    if (q.isBlank()) return androidx.compose.ui.text.AnnotatedString(text)
    val highlightStyle = SpanStyle(
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
    return buildAnnotatedString {
        var start = 0
        val lower = text.lowercase()
        val lowerQuery = q.lowercase()
        while (true) {
            val index = lower.indexOf(lowerQuery, start)
            if (index < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, index))
            withStyle(highlightStyle) {
                append(text.substring(index, index + q.length))
            }
            start = index + q.length
        }
    }
}