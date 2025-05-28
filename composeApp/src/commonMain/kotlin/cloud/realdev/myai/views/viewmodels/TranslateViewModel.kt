package cloud.realdev.myai.views.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.realdev.myai.models.translate.Languages
import cloud.realdev.myai.models.translate.TranslationResult
import cloud.realdev.myai.models.translate.TranslationRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TranslateViewModel: ViewModel() {

    private val _translationRequest = MutableStateFlow(TranslationRequest())
    val translationRequest: StateFlow<TranslationRequest> = _translationRequest.asStateFlow()

    private val _translationResultResult = MutableStateFlow<TranslationResult?>(null)
    val translationResult: StateFlow<TranslationResult?> = _translationResultResult.asStateFlow()

    private val _sendingRequest = MutableStateFlow(false)
    val sendingRequest: StateFlow<Boolean> = _sendingRequest.asStateFlow()

    val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    fun setText(text: String) {
        _translationRequest.value = _translationRequest.value.copy(text = text)
    }

    fun setSourceLanguage(languages: Languages) {
        _translationRequest.value = _translationRequest.value.copy(source_lang = languages.serverTranslation)
    }

    fun setTargetLanguage(languages: Languages) {
        _translationRequest.value = _translationRequest.value.copy(target_lang = languages.serverTranslation)
    }

    fun swapTranslationRequest() {
        val source = _translationRequest.value.source_lang
        val target = _translationRequest.value.target_lang
        _translationRequest.value = _translationRequest.value.copy(source_lang = target, target_lang = source)
    }

    fun clear() {
        _translationRequest.value = _translationRequest.value.copy(text = "")
        _translationResultResult.value = null
    }


    @OptIn(InternalAPI::class)
    fun translate(onError: () -> Unit) {
        if(_translationRequest.value.text.isEmpty()) {
            return
        }

        _sendingRequest.value = true

        try {
            viewModelScope.launch {
                try {
                    val response = client.post("http://192.168.1.25:8000/translate") {
                        contentType(ContentType.Application.Json)
                        setBody(_translationRequest.value)
                        timeout {
                            requestTimeoutMillis = 60000
                            socketTimeoutMillis = 60000
                        }
                    }

                    val translationResult = response.body<TranslationResult>()
                    _translationResultResult.value = translationResult
                    _sendingRequest.value = false
                } catch (e: Exception) {
                    onError()
                    _sendingRequest.value = false
                    return@launch
                }

            }
        } catch (e: Exception) {
            onError()
            _sendingRequest.value = false
            return
        }

    }

    override fun onCleared() {
        client.close()
        super.onCleared()
    }
}