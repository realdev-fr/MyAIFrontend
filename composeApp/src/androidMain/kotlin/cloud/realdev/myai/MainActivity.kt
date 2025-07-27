package cloud.realdev.myai

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            println("Permission RECORD_AUDIO accordée.")
            // Lancer l'enregistrement après l'octroi de la permission
            // Vous pouvez ajouter un événement ou un état pour le ViewModel ici
        } else {
            println("Permission RECORD_AUDIO refusée.")
            // Informer l'utilisateur que la fonction ne peut pas être utilisée
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(this) {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
}