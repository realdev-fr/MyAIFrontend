package cloud.realdev.myai.models.discuss

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class DiscussionResult(
    val type: String = "",
    val content: String? = null,
    val tool_name: String? = null,
    val tool_output: JsonElement? = null
)