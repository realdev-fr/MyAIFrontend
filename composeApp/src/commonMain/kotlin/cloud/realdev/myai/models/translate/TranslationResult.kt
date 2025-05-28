package cloud.realdev.myai.models.translate

import kotlinx.serialization.Serializable

@Serializable
data class TranslationResult(
    val translation: String,
    val language: Languages,
    val explanation: String,
    val correction: String
)
