package cloud.realdev.myai.models.translate

import kotlinx.serialization.Serializable

@Serializable
data class TranslationResult(
    val translation: String = "",
    val language: String? = null,
    val explanation: String? = null,
    val correction: String? = null
)
