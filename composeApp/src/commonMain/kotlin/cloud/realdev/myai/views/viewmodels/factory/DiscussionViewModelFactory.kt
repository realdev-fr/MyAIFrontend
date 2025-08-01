package cloud.realdev.myai.views.viewmodels.factory

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cloud.realdev.myai.views.viewmodels.DiscussionViewModel

class DiscussionViewModelFactory(private val application: Application, private val isLocal: Boolean = false) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DiscussionViewModel(application, isLocal) as T
    }
}