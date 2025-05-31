package cloud.realdev.myai.views.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.realdev.myai.models.discuss.DiscussionRequest
import cloud.realdev.myai.models.discuss.DiscussionResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.post
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

class DiscussionViewModel: ViewModel() {

    private val _discussionRequest = MutableStateFlow(DiscussionRequest())
    val discussionRequest: StateFlow<DiscussionRequest> = _discussionRequest.asStateFlow()

    private val _discussionResult = MutableStateFlow<DiscussionResult?>(null)
    val discussionResult: StateFlow<DiscussionResult?> = _discussionResult.asStateFlow()

    private val _sendingRequest = MutableStateFlow(false)
    val sendingRequest: StateFlow<Boolean> = _sendingRequest.asStateFlow()

    val client = HttpClient(CIO) {
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

        _discussionResult.value = null

        _sendingRequest.value = true

        try {
            viewModelScope.launch {
                try {
                    val httpStatement: HttpStatement = client.preparePost("http://192.168.1.25:8000/discuss") {
                        contentType(ContentType.Application.Json)
                        accept(ContentType.Text.EventStream)
                        setBody(_discussionRequest.value)
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
                                    val json = Json.decodeFromString<DiscussionResult>(line)
                                    if(_discussionResult.value == null) {
                                        _discussionResult.value = json
                                        continue
                                    }
                                    _discussionResult.value = _discussionResult.value?.copy(response = _discussionResult.value?.response + json.response)
                                } catch (e: Exception) {
                                    print(e)
                                }
                            }
                        }
                    }
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