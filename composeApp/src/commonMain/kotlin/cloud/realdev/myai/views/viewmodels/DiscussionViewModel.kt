package cloud.realdev.myai.views.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.realdev.myai.models.discuss.DiscussionRequest
import cloud.realdev.myai.models.discuss.DiscussionResult
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class DiscussionViewModel(application: Application, val isLocal: Boolean = false): AndroidViewModel(application) {

    private val _discussionRequest = MutableStateFlow(DiscussionRequest())
    val discussionRequest: StateFlow<DiscussionRequest> = _discussionRequest.asStateFlow()

    private val _discussionResult = MutableStateFlow<DiscussionResult?>(null)
    val discussionResult: StateFlow<DiscussionResult?> = _discussionResult.asStateFlow()

    private val _stream = MutableStateFlow(true)
    val stream: StateFlow<Boolean> = _stream.asStateFlow()

    private val _reflexion = MutableStateFlow(true)
    val reflexion: StateFlow<Boolean> = _reflexion.asStateFlow()

    private val _sendingRequest = MutableStateFlow(false)
    val sendingRequest: StateFlow<Boolean> = _sendingRequest.asStateFlow()

    private var _llmInference: LlmInference? = null


    init {
        setupModel()
    }

    fun setupModel() {
        if(isLocal) {
            viewModelScope.launch(Dispatchers.IO) {
                val context = getApplication<Application>().applicationContext

                val taskOptions = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(if(_reflexion.value) "/data/local/tmp/llm/model_version.task" else "/data/local/tmp/llm/model_version.bin")
                    .setMaxTopK(64)
                    .build()

                _llmInference = LlmInference.createFromOptions(context, taskOptions)
            }
        }
    }

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

    fun toggleStream() {
        _stream.value = !_stream.value
    }

    fun toggleReflexion() {
        _reflexion.value = !_reflexion.value
        setupModel()
    }


    fun discussLocal(stream : Boolean) {
        if(_llmInference == null) {
            return
        }

        _discussionResult.value = null

        if(stream) {
            _llmInference?.generateResponseAsync(_discussionRequest.value.text
            ) { partialResult, done ->
                viewModelScope.launch {
                    _discussionResult.value = _discussionResult.value?.copy(content = ((_discussionResult.value?.content ?: "") + partialResult)) ?: DiscussionResult(content = partialResult)
                }
            }
        }

        else {
            viewModelScope.launch {
                val result = withContext(Dispatchers.IO) {
                    _llmInference!!.generateResponse(_discussionRequest.value.text)
                }
                _discussionResult.value = _discussionResult.value?.copy(content = result) ?: DiscussionResult(content = result)
            }
        }
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
                    val httpStatement: HttpStatement = client.preparePost("http://192.168.1.25:9000/" + if(stream.value) "discuss" else "ask") {
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
                            val line = channel.readUTF8Line()?.replace("JSON input:", "")
                            println("Reçu du serveur (texte): $line")
                            if (line != null && line.isNotEmpty()) {
                                try {
                                    val jsonDecoder = Json {
                                        ignoreUnknownKeys = true
                                    }

                                    val json = jsonDecoder.decodeFromString<DiscussionResult>(line)

                                    if(_discussionResult.value == null) {
                                        _discussionResult.value = json
                                        continue
                                    }

                                    if(json.type == "final_response") {
                                        _discussionResult.value = _discussionResult.value?.copy(content = (_discussionResult.value?.content?:"") + json.content)
                                    } else {
                                        print("Not final response")
                                    }
                                } catch (e: Exception) {
                                    print("Exception: " + e)
                                }
                            }
                        }
                    }
                    _sendingRequest.value = false
                } catch (e: Exception) {
                    print("Exception: " + e)
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