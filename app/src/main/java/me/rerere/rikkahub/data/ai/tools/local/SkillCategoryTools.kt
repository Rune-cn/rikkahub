package me.rerere.rikkahub.data.ai.tools.local

import androidx.compose.ui.text.input.InputSchema
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.files.SkillFrontmatterParser
import me.rerere.rikkahub.data.files.SkillManager

internal const val LIST_SKILLS_TOOL_NAME = "list_skills"
internal const val SET_SKILL_CATEGORY_TOOL_NAME = "set_skill_category"

/**
 * Skill 分类管理工具：让 AI 查看 skill 列表及其分类、修改分类
 *
 * - `list_skills`：查看已安装 skill（名称/描述/分类）
 * - `set_skill_category`：设置/修改 skill 的分类（持久化到 SKILL.md）
 */
internal fun buildSkillCategoryTools(skillManager: SkillManager): List<Tool> = listOf(
    listSkillsTool(skillManager),
    setSkillCategoryTool(skillManager),
)

private fun listSkillsTool(skillManager: SkillManager): Tool = Tool(
    name = LIST_SKILLS_TOOL_NAME,
    description = "List installed AI skills with their name, description and category.",
    parameters = {
        InputSchema.Obj(properties = buildJsonObject { })
    },
    needsApproval = { false },
    execute = {
        val skills = skillManager.listSkills()
        val payload = buildJsonObject {
            put("skills", buildJsonArray {
                skills.forEach { s ->
                    add(buildJsonObject {
                        put("name", s.name)
                        put("description", s.description)
                        put("category", s.category ?: "uncategorized")
                        put("builtin", s.builtin)
                    })
                }
            })
        }
        listOf(UIMessagePart.Text(payload.toString()))
    }
)

private fun setSkillCategoryTool(skillManager: SkillManager): Tool = Tool(
    name = SET_SKILL_CATEGORY_TOOL_NAME,
    description = "Set or change an installed skill's category label, persisted to SKILL.md. Built-in skills are read-only.",
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                put("name", buildJsonObject {
                    put("type", "string")
                    put("description", "Skill name.")
                })
                put("category", buildJsonObject {
                    put("type", "string")
                    put("description", "New category label, e.g. \\\"Search\\\" or any custom label.")
                })
            }
        )
    },
    needsApproval = { false },
    execute = { args ->
        val params = args.jsonObject
        val name = params["name"]?.jsonPrimitive?.contentOrNull
            ?: return@Tool listOf(UIMessagePart.Text("{\"error\":\"missing name\"}"))
        val category = params["category"]?.jsonPrimitive?.contentOrNull?.trim()
            ?: return@Tool listOf(UIMessagePart.Text("{\"error\":\"missing category\"}"))
        val skill = skillManager.listSkills().firstOrNull { it.name == name }
            ?: return@Tool listOf(UIMessagePart.Text("{\"error\":\"skill not found: $name\"}"))
        if (skill.builtin) {
            return@Tool listOf(UIMessagePart.Text("{\"error\":\"built-in skill is read-only\"}"))
        }
        runCatching {
            val file = skill.skillFile
            file.writeText(SkillFrontmatterParser.update(file.readText(), "category", category))
        }.onFailure {
            return@Tool listOf(UIMessagePart.Text("{\"error\":\"${it.message}\"}"))
        }
        listOf(UIMessagePart.Text("{\"ok\":true,\"name\":\"$name\",\"category\":\"$category\"}"))
    }
)
