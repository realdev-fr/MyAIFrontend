package cloud.realdev.myai.models.discuss

import kotlinx.serialization.Serializable

@Serializable
data class DiscussionResult(val type: String = "", val content: String? = null)