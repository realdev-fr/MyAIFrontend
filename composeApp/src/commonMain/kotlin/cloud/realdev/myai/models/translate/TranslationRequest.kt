package cloud.realdev.myai.models.translate

import kotlinx.serialization.Serializable

@Serializable
data class TranslationRequest(val source_lang: String = Languages.FRENCH.serverTranslation, val target_lang: String = Languages.ENGLISH.serverTranslation, val text: String = "") {
}