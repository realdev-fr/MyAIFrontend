package cloud.realdev.myai.views.viewmodels

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.realdev.myai.models.BASE_IP
import cloud.realdev.myai.models.discuss.DiscussionRequest
import cloud.realdev.myai.models.discuss.DiscussionResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class SpeakViewModel: ViewModel() {

    private val _discussionRequest = MutableStateFlow(DiscussionRequest())
    val discussionRequest: StateFlow<DiscussionRequest> = _discussionRequest.asStateFlow()

    private val _discussionResult = MutableStateFlow<DiscussionResult?>(null)
    val discussionResult: StateFlow<DiscussionResult?> = _discussionResult.asStateFlow()

    private val _sendingRequest = MutableStateFlow(false)
    val sendingRequest: StateFlow<Boolean> = _sendingRequest.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    val client = HttpClient(CIO) {
        install(WebSockets) // Important: installer le plugin WebSockets
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        // Pas besoin de configurer le moteur ici spécifiquement pour le WebSocket,
        // CIO le gère bien par défaut avec le plugin WebSockets.
    }

    val speakSocketUrl = "ws://$BASE_IP/ws/speak" // L'URL de votre endpoint FastAPI


    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    // Paramètres audio pour AudioRecord
    private val sampleRate = 16000 // Fréquence d'échantillonnage (Hz) - courant pour la voix
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO // Mono
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT // 16-bit PCM (brut)
    private var bufferSizeInBytes: Int = 0

    init {
        // Calcul de la taille du buffer minimum
        bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (bufferSizeInBytes == AudioRecord.ERROR || bufferSizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
            println("Erreur lors du calcul de la taille du buffer minimum.")
            // Gérer l'erreur, utiliser une taille par défaut ou quitter
            bufferSizeInBytes = sampleRate * 2 // Exemple: 1 seconde de mono 16-bit PCM
        }
        println("Taille du buffer audio minimum: $bufferSizeInBytes octets")
    }

    fun getIsRecording(): Boolean = isRecording.value

     // AudioRecord.STATE_INITIALIZED est pour API 23+
    fun startRecording(context: android.content.Context) {
        if (isRecording.value) {
            println("L'enregistrement est déjà en cours.")
            return
        }

        // Vérifier les permissions
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            println("Permission RECORD_AUDIO non accordée.")
            // La demande de permission doit être faite depuis l'activité/composable
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSizeInBytes
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            println("Impossible d'initialiser AudioRecord.")
            audioRecord?.release()
            audioRecord = null
            return
        }

        _isRecording.value = true

        audioRecord?.startRecording()
        println("Enregistrement audio démarré.")

        recordingJob = viewModelScope.launch(Dispatchers.IO) { // Utiliser un Dispatcher.IO pour les opérations de lecture
            client.webSocket(speakSocketUrl) {
                println("Connecté au serveur WebSocket pour le streaming audio.")

                val receiveJob = launch {
                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val message = frame.readText()
                                    println("Reçu du serveur (texte): $message")
                                    if (message.isNotEmpty()) {
                                        try {
                                            val json = Json.decodeFromString<DiscussionResult>(message)
                                            if(_discussionResult.value == null) {
                                                _discussionResult.value = json
                                                continue
                                            }
                                            _discussionResult.value = _discussionResult.value?.copy(content = _discussionResult.value?.content + json.content)
                                        } catch (e: Exception) {
                                            print(e)
                                        }
                                    }
                                    // Tu peux parser le JSON ici si nécessaire
                                }
                                is Frame.Binary -> {
                                    println("Reçu du serveur (binaire): ${frame.readBytes().size} octets")
                                }
                                else -> {}
                            }
                        }
                    } catch (e: Exception) {
                        println("Erreur de réception WebSocket: ${e.message}")
                    }
                }


                val audioBuffer = ByteArray(bufferSizeInBytes)
                var bytesRead: Int

                while (isRecording.value && currentCoroutineContext().isActive) { // S'assurer que la coroutine n'est pas annulée
                    bytesRead = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0

                    if (bytesRead > 0) {
                        val chunk = audioBuffer.copyOf(bytesRead) // Copier seulement les octets lus
                        try {
                            outgoing.send(Frame.Binary(true, chunk))
                            println("Envoyé un chunk audio de ${chunk.size} octets.")
                        } catch (e: Exception) {
                            println("Erreur lors de l'envoi du chunk via WebSocket: ${e.message}")
                            // Arrêter l'enregistrement en cas d'erreur d'envoi
                            stopRecording()
                            break // Sortir de la boucle d'envoi
                        }
                    } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE || bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        println("Erreur de lecture AudioRecord: $bytesRead")
                        stopRecording()
                        break
                    }
                    // Pas besoin d'un delay explicite ici, la lecture est bloquante et AudioRecord gère le flux
                }
                println("Boucle d'enregistrement terminée.")
                receiveJob.cancelAndJoin()  // 🧹 Nettoyer proprement
            }
        }
    }

    fun stopRecording() {
        if (!isRecording.value) return

        _isRecording.value = false
        recordingJob?.cancel() // Annuler la coroutine d'enregistrement
        recordingJob = null

        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        println("Enregistrement audio arrêté et ressources libérées.")

        // Fermer le client Ktor WebSocket si nécessaire (il est fermé dans le bloc `webSocket`)
        // client.close() // Cette ligne n'est pas nécessaire ici car le bloc `webSocket` la gère.
    }

    fun clear() {
        _discussionResult.value = null
    }

    override fun onCleared() {
        client.close()
        stopRecording()
        super.onCleared()
    }
}