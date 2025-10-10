package cloud.realdev.myai.views.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.realdev.myai.models.BASE_URL
import cloud.realdev.myai.models.translate.Languages
import cloud.realdev.myai.models.translate.TranslationResult
import cloud.realdev.myai.models.translate.TranslationRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpStatement
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class TranslateViewModel: ViewModel() {

    private val _translationRequest = MutableStateFlow(TranslationRequest())
    val translationRequest: StateFlow<TranslationRequest> = _translationRequest.asStateFlow()

    private val _translationResultResult = MutableStateFlow<TranslationResult?>(null)
    val translationResult: StateFlow<TranslationResult?> = _translationResultResult.asStateFlow()

    private val _sendingRequest = MutableStateFlow(false)
    val sendingRequest: StateFlow<Boolean> = _sendingRequest.asStateFlow()

    val client = HttpClient(CIO) {
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

        _translationResultResult.value = null

        _sendingRequest.value = true

        try {
            viewModelScope.launch {
                try {
                    val httpStatement: HttpStatement = client.preparePost("$BASE_URL/translate") {
                        contentType(ContentType.Application.Json)
                        accept(ContentType.Text.EventStream)
                        setBody(_translationRequest.value)
                        timeout {
                            requestTimeoutMillis = 60000
                            socketTimeoutMillis = 60000
                        }
                    }

                    httpStatement.execute { response ->
                        val channel: ByteReadChannel = response.body()
                        while (!channel.isClosedForRead) {
                            val line = channel.readUTF8Line()
                            if (line != null && line.isNotEmpty()) {
                                try {
                                    val json = Json.decodeFromString<TranslationResult>(line)
                                    if(_translationResultResult.value == null) {
                                        _translationResultResult.value = json
                                        continue
                                    }
                                    if(json.translation.isNotEmpty()) {
                                        _translationResultResult.value = _translationResultResult.value!!.copy(translation = _translationResultResult.value!!.translation + json.translation)
                                    }

                                    if(json.explanation != null && json.explanation.isNotEmpty()) {
                                        _translationResultResult.value = _translationResultResult.value!!.copy(explanation = (_translationResultResult.value!!.explanation
                                            ?: "") + (json.explanation)
                                        )
                                    }

                                    if(json.language != null && json.language.isNotEmpty()) {
                                        _translationResultResult.value = _translationResultResult.value!!.copy(language = (_translationResultResult.value!!.language
                                            ?: "") + (json.language)
                                        )
                                    }

                                    if(json.correction != null && json.correction.isNotEmpty()) {
                                        _translationResultResult.value = _translationResultResult.value!!.copy(correction = (_translationResultResult.value!!.correction
                                            ?: "") + (json.correction)
                                        )
                                    }
                                } catch (e: Exception) {
                                    println("Erreur de parsing: ${e.message}")
                                }
                            }
                        }

                        if(_translationResultResult.value != null) {
                            if(_translationResultResult.value!!.translation.endsWith(" ")) {
                                _translationResultResult.value = _translationResultResult.value!!.copy(translation = _translationResultResult.value!!.translation.dropLast(1))
                            }

                            if(_translationResultResult.value!!.explanation != null && _translationResultResult.value!!.explanation!!.endsWith(" ")) {
                                _translationResultResult.value = _translationResultResult.value!!.copy(explanation = _translationResultResult.value!!.explanation!!.dropLast(1))
                            }

                            if(_translationResultResult.value!!.language != null && _translationResultResult.value!!.language!!.endsWith(" ")) {
                                _translationResultResult.value = _translationResultResult.value!!.copy(language = _translationResultResult.value!!.language!!.dropLast(1))
                            }

                            if(_translationResultResult.value!!.correction != null && _translationResultResult.value!!.correction!!.endsWith(" ")) {
                                _translationResultResult.value = _translationResultResult.value!!.copy(correction = _translationResultResult.value!!.correction!!.dropLast(1))
                            }
                        }
                    }
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