package cloud.realdev.myai.views

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import cloud.realdev.myai.models.navigation.Screen
import cloud.realdev.myai.models.translate.Languages
import cloud.realdev.myai.models.translate.TranslationResult
import cloud.realdev.myai.views.utils.clipboard.copyToClipboard
import cloud.realdev.myai.views.viewmodels.TranslateViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslationsView(backStack: NavBackStack) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    val viewModel = viewModel<TranslateViewModel>()
    val translationRequest by viewModel.translationRequest.collectAsState()
    val translationResult by viewModel.translationResult.collectAsState()
    val sendingRequest by viewModel.sendingRequest.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun hideKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    fun translate() {
        hideKeyboard()
        viewModel.translate {
            scope.launch {
                snackBarHostState
                    .showSnackbar(
                        message = "Translation failed",
                        // Defaults to SnackbarDuration.Short
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Translate")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        backStack.remove(Screen.Translate)
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
                TranslateView(translationRequest.text, sendingRequest, clipboardManager, translationResult, onTextChanged = {
                    viewModel.setText(it)
                }) {
                    translate()
                }
            }
            Row(modifier = Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                LanguageSelector(translationRequest.source_lang) {
                    viewModel.setSourceLanguage(it)
                }

                IconButton(
                    onClick = {
                        viewModel.swapTranslationRequest()
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Swap")
                }

                LanguageSelector(translationRequest.target_lang) {
                    viewModel.setTargetLanguage(it)
                }

                if(!sendingRequest && translationRequest.text.isNotEmpty()) {
                    IconButton(
                        modifier = Modifier.padding(start = 20.dp),
                        onClick = {
                            viewModel.clear()
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear")
                    }
                }
            }
            Box(modifier = Modifier.weight(5f).fillMaxWidth().padding(16.dp).border(1.dp, color = Color.Black, shape = RoundedCornerShape(8.dp)).padding(16.dp)) {
                if(translationResult == null) {
                    return@Box
                }
                TranslateResult(translationResult!!, clipboardManager)
            }
        }
    }
}

@Composable
fun TranslateView(text: String, sendingRequest: Boolean, clipboardManager: ClipboardManager, translationResult: TranslationResult?, onTextChanged: (String) -> Unit, onTranslate: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        if(translationResult != null && translationResult.correction != null && translationResult.correction.isNotEmpty() && translationResult.correction != text && translationResult.correction != "---") {
            Box(modifier = Modifier.padding(5.dp)) {
                Text("Correction: " + translationResult.correction)
            }
        }
        Column(modifier = Modifier.weight(4f).fillMaxSize()) {
            OutlinedTextField(
                modifier = Modifier.fillMaxSize(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                ),
                value = text,
                onValueChange = {
                    onTextChanged(it)
                },
                placeholder = {
                    Text("Enter text")
                },
            )
        }

        if(!text.isEmpty()) {
            Row(modifier = Modifier.weight(1f).fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    copyToClipboard(clipboardManager, text)
                }) {
                    Text("Copy to clipboard")
                }
                if(sendingRequest) {
                    return@Row
                }
                TextButton(onClick = {
                    onTranslate()
                }) {
                    Text("Translate")
                }
            }
        }
    }
}

@Composable
fun TranslateResult(translationResult: TranslationResult, clipboardManager: ClipboardManager) {
    var showExplanation by remember { mutableStateOf(false) }

    if(showExplanation) {
        AlertDialog(
            icon = {
                Icon(Icons.Default.Info, contentDescription = "Example Icon")
            },
            title = {
                Text(text = "Explication")
            },
            text = {
                Text(text = translationResult.explanation?:"")
            },
            onDismissRequest = {
                showExplanation = false
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExplanation = false
                    }
                ) {
                    Text("Ok")
                }
            },
        )
    }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.weight(4f).fillMaxSize().border(1.dp, color = Color.Black, shape = RoundedCornerShape(8.dp)).padding(16.dp)) {
            Text(translationResult.translation)
        }
        Box(modifier = Modifier.weight(1f).wrapContentHeight(align = Alignment.CenterVertically)) {
            Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                   showExplanation = true
                }) {
                    Icon(Icons.Default.Info, contentDescription = "Info")
                }
                TextButton(onClick = {
                    copyToClipboard(clipboardManager, translationResult.translation)
                }) {
                    Text("Copy to clipboard")
                }
            }
        }
    }
}

@Composable
fun LanguageSelector(selectedLanguage: String, onClick: (Languages) -> Unit) {
    var langExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.border(1.dp, color = Color.Black, shape = RoundedCornerShape(8.dp)).padding(16.dp).clickable {
        langExpanded = true
    }) {
        Text(selectedLanguage)
    }

    DropdownMenu(
        expanded = langExpanded,
        onDismissRequest = { langExpanded = false }
    ) {
        // First section
        for(language in Languages.entries) {
            // First section
            DropdownMenuItem(
                text = { Text(language.translation) },
                leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null) },
                onClick = {
                    onClick(language)
                    langExpanded = false
                }
            )
        }
    }
}