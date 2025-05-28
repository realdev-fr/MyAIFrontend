package cloud.realdev.myai.views.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.realdev.myai.models.discuss.DiscussionRequest
import cloud.realdev.myai.models.discuss.DiscussionResult
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

class DiscussionViewModel: ViewModel() {

    private val _discussionRequest = MutableStateFlow(DiscussionRequest())
    val discussionRequest: StateFlow<DiscussionRequest> = _discussionRequest.asStateFlow()

    private val _discussionResult = MutableStateFlow<DiscussionResult?>(null)
    val discussionResult: StateFlow<DiscussionResult?> = _discussionResult.asStateFlow()

    private val _sendingRequest = MutableStateFlow(false)
    val sendingRequest: StateFlow<Boolean> = _sendingRequest.asStateFlow()

    val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    fun setText(text: String) {
        _discussionRequest.value = _discussionRequest.value.copy(text = text)
    }

    fun clear() {
        _discussionRequest.value = _discussionRequest.value.copy(text = "")
        _discussionResult.value = null
    }


    @OptIn(InternalAPI::class)
    fun discuss(onError: () -> Unit) {
        if(_discussionRequest.value.text.isEmpty()) {
            return
        }

        _sendingRequest.value = true

        try {
            viewModelScope.launch {
                try {
                    val response = client.post("http://192.168.1.25:8000/discuss") {
                        contentType(ContentType.Application.Json)
                        setBody(_discussionRequest.value)
                        timeout {
                            requestTimeoutMillis = 60000
                            socketTimeoutMillis = 60000
                        }
                    }

                    val translationResult = response.body<DiscussionResult>()
                    _discussionResult.value = translationResult
                    _sendingRequest.value = false
                } catch (e: Exception) {
                    print(e)
                    onError()
                    _sendingRequest.value = false
                    return@launch
                }

            }
        } catch (e: Exception) {
            print(e)
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