package cloud.realdev.myai.models.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class Screen: NavKey {
    @Serializable
    data object Home : Screen()
    @Serializable
    data object Translate : Screen()
    @Serializable
    data object Chat : Screen()
}