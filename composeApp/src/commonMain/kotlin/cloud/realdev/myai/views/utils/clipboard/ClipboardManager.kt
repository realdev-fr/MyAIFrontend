package cloud.realdev.myai.views.utils.clipboard

import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.buildAnnotatedString

fun copyToClipboard(clipboardManager: ClipboardManager, text: String) {
    if(text.isEmpty()) {
        return
    }
    clipboardManager.setText(
        annotatedString = buildAnnotatedString {
            append(text = text)
        }
    )
}