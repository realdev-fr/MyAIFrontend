package cloud.realdev.myai.views

import android.app.Application
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import cloud.realdev.myai.models.discuss.DiscussionResult
import cloud.realdev.myai.models.navigation.Screen
import cloud.realdev.myai.views.utils.clipboard.copyToClipboard
import cloud.realdev.myai.views.viewmodels.DiscussionViewModel
import cloud.realdev.myai.views.viewmodels.factory.DiscussionViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalChatView(backStack: NavBackStack) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    val application = LocalContext.current.applicationContext as Application
    val viewModelFactory = remember { DiscussionViewModelFactory(application, isLocal = true) }
    val viewModel: DiscussionViewModel = viewModel(factory = viewModelFactory)

    val discussionRequest by viewModel.discussionRequest.collectAsState()
    val discussionResult by viewModel.discussionResult.collectAsState()
    val sendingRequest by viewModel.sendingRequest.collectAsState()
    val isReflexion by viewModel.reflexion.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun hideKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    fun discuss() {
        hideKeyboard()
        viewModel.discussLocal(true)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Discuter")
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
            Box(modifier = Modifier.weight(0.5f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Rapide", modifier = Modifier.padding(8.dp), style = TextStyle(color = Color.Black))
                    Switch(
                        checked = isReflexion,
                        onCheckedChange = {
                            viewModel.toggleReflexion()
                        },
                        modifier = Modifier.padding(8.dp)
                    )
                    Text("Réflexion", modifier = Modifier.padding(8.dp), style = TextStyle(color = Color.Black))
                }
            }
            Box(modifier = Modifier.weight(5f).fillMaxWidth().padding(16.dp).border(1.dp, color = Color.Black, shape = RoundedCornerShape(8.dp)).padding(16.dp)) {
                SentenceView(discussionRequest.text, sendingRequest, clipboardManager, onTextChanged = {
                    viewModel.setText(it)
                }) {
                    discuss()
                }
            }
            Row(modifier = Modifier.weight(0.5f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if(!sendingRequest && discussionRequest.text.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            viewModel.clear()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
            }
            Box(modifier = Modifier.weight(5f).fillMaxWidth().padding(16.dp).border(1.dp, color = Color.Black, shape = RoundedCornerShape(8.dp)).padding(16.dp)) {
                if(discussionResult == null) {
                    return@Box
                }
                DiscussionResult(discussionResult!!, clipboardManager)
            }
        }
    }
}