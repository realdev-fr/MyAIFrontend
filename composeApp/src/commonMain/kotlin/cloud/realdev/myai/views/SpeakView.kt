package cloud.realdev.myai.views

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import cloud.realdev.myai.models.discuss.DiscussionResult
import cloud.realdev.myai.models.navigation.Screen
import cloud.realdev.myai.views.utils.clipboard.copyToClipboard
import cloud.realdev.myai.views.viewmodels.DiscussionViewModel
import cloud.realdev.myai.views.viewmodels.SpeakViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeakView(backStack: NavBackStack, activityContext: ComponentActivity, onRequestPermission: () -> Unit) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    val viewModel = viewModel<SpeakViewModel>()
    val discussionRequest by viewModel.discussionRequest.collectAsState()
    val discussionResult by viewModel.discussionResult.collectAsState()
    val sendingRequest by viewModel.sendingRequest.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isRecording by viewModel.isRecording.collectAsState() // Observe l'état d'enregistrement

    val context = LocalContext.current

    fun hideKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Parler")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        backStack.remove(Screen.Chat)
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackBarHostState)
        }
    ) {
        Column(modifier = Modifier.padding(it).clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
            hideKeyboard()
        }) {
            Box(modifier = Modifier.weight(5f).fillMaxWidth().padding(16.dp).border(1.dp, color = Color.Black, shape = RoundedCornerShape(8.dp)).padding(16.dp)) {
                UserSpeakView(isRecording, viewModel, activityContext, onRequestPermission)
            }
            Box(modifier = Modifier.weight(5f).fillMaxWidth().padding(16.dp).border(1.dp, color = Color.Black, shape = RoundedCornerShape(8.dp)).padding(16.dp)) {
                if(discussionResult == null) {
                    return@Box
                }
                SpeakResult(discussionResult!!, clipboardManager, viewModel)
            }
        }
    }
}

@Composable
fun UserSpeakView(isRecording: Boolean, viewModel: SpeakViewModel, applicationContext: ComponentActivity, onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRecording) "Enregistrement en cours..." else "Appuyez pour enregistrer",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (!isRecording) {
                    // Demander la permission si ce n'est pas déjà fait
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        applicationContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        onRequestPermission()
                    } else {
                        // Permission déjà accordée ou API < 23
                        viewModel.startRecording(applicationContext)
                    }
                } else {
                    viewModel.stopRecording()
                }
            },
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        ) {
            Text(if (isRecording) "STOP" else "START")
        }
    }
}

@Composable
fun SpeakResult(discussionResult: DiscussionResult, clipboardManager: ClipboardManager, viewModel: SpeakViewModel) {
    val scrollState = rememberScrollState()

    var autoScrollEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(scrollState.maxValue) {
        //Scroll automatiquement vers le bas
        if(autoScrollEnabled) {
            scrollState.scrollTo(scrollState.maxValue)
        }
    }

    // Observer la position de scroll pour savoir si l'utilisateur est en bas
    LaunchedEffect(scrollState.value) {
        val threshold = 20 // tolérance en pixels pour considérer qu'on est "en bas"
        autoScrollEnabled = scrollState.maxValue - scrollState.value <= threshold
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(modifier = Modifier.weight(3f).fillMaxSize().border(1.dp, color = Color.Black, shape = RoundedCornerShape(8.dp)).padding(16.dp).verticalScroll(scrollState)) {
            Text(discussionResult.response)
        }
        Box(modifier = Modifier.weight(1f).wrapContentHeight(align = Alignment.CenterVertically)) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    copyToClipboard(clipboardManager, discussionResult.response)
                }) {
                    Text("Copy to clipboard")
                }

                TextButton(onClick = {
                    viewModel.clear()
                }) {
                    Text("Clean text")
                }
            }
        }
    }
}