package cloud.realdev.myai.models.discuss

import kotlinx.serialization.Serializable

@Serializable
data class DiscussionRequest(val text: String = "")