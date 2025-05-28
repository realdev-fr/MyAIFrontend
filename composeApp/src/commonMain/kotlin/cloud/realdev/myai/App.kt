package cloud.realdev.myai

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import cloud.realdev.myai.models.navigation.Screen
import cloud.realdev.myai.views.ChatView
import cloud.realdev.myai.views.HomeView
import cloud.realdev.myai.views.TranslationsView
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val backStack = rememberNavBackStack<Screen>(Screen.Home)

    MaterialTheme {
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            entryProvider = entryProvider {
                entry<Screen.Home> {
                    HomeView(backStack = backStack)
                }
                entry<Screen.Translate> {
                    TranslationsView(backStack = backStack)
                }
                entry<Screen.Chat> {
                    ChatView(backStack = backStack)
                }
            }
        )
    }
}