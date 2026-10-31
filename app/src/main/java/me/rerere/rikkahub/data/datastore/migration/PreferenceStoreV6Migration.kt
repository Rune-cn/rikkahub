package me.rerere.rikkahub.data.datastore.migration

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.rikkahub.utils.jsonPrimitiveOrNull

/**
 * V5 → V6：让已有助手默认拥有「工作流管理」工具。
 *
 * 背景：工作流管理工具（create_workflow 等）默认只随 `Assistant.localTools` 启用，
 * 而旧版本该字段默认只有 TimeInfo，AI 拿不到工作流工具（表现为"AI 不知道自己有工作流工具"）。
 * 这里给所有已有助手的 localTools 补上 `workflow_manage`（没有才加，保留用户已有选择）。
 * 迁移只跑一次，之后用户仍可在「助手 → 本地工具」里手动关闭。
 */
class PreferenceStoreV6Migration : DataMigration<Preferences> {
    override suspend fun shouldMigrate(currentData: Preferences): Boolean {
        val version = currentData[SettingsStore.VERSION]
        return version == null || version < 6
    }

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()

        prefs[SettingsStore.ASSISTANTS]?.let { json ->
            prefs[SettingsStore.ASSISTANTS] = migrateAssistantsAddWorkflowTool(json)
        }

        prefs[SettingsStore.VERSION] = 6
        return prefs.toPreferences()
    }

    override suspend fun cleanUp() {}
}

internal fun migrateAssistantsAddWorkflowTool(assistantsJson: String): String {
    return runCatching {
        val root = JsonInstant.parseToJsonElement(assistantsJson) as? JsonArray
            ?: return@runCatching assistantsJson
        JsonInstant.encodeToString(JsonArray(root.map(::migrateAssistantAddWorkflowTool)))
    }.getOrDefault(assistantsJson)
}

/** 给单个助手的 localTools 补上 workflow_manage（不存在才加）。 */
internal fun migrateAssistantAddWorkflowTool(assistant: JsonElement): JsonElement {
    val obj = assistant as? JsonObject ?: return assistant
    val migrated = obj.toMutableMap()
    val localTools = obj["localTools"] as? JsonArray
    if (localTools != null) {
        val hasWorkflow = localTools.any { it.jsonPrimitiveOrNull?.contentOrNull == "workflow_manage" }
        if (!hasWorkflow) {
            migrated["localTools"] = buildJsonArray {
                localTools.forEach { add(it) }
                add("workflow_manage")
            }
        }
    }
    return JsonObject(migrated)
}