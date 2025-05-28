package cloud.realdev.myai.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import cloud.realdev.myai.models.navigation.Screen

@Composable
fun HomeView(backStack: NavBackStack) {
    Scaffold {
        Column(modifier = Modifier.padding(it)) {
            IconMenuItem(backStack, "Translate", Screen.Translate)
            IconMenuItem(backStack, "Chat", Screen.Chat)
        }
    }
}

@Composable
fun IconMenuItem(backStack: NavBackStack, text: String, screen: Screen) {
    ElevatedCard (
        modifier = Modifier.padding(16.dp),
        onClick = {
            backStack.add(screen)
        },
    ) {
        Row(
            modifier = Modifier.padding(PaddingValues(start = 16.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text)
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                backStack.add(screen)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
            }
        }
    }
}