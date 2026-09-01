package com.dirac.mactrack.ui.feature.more

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.backup.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BackupViewModel(private val manager: BackupManager) : ViewModel() {

    // A one-shot result message for the UI to show as a toast, then clear.
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    fun clearMessage() { _message.value = null }

    fun export(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val json = manager.exportJson()
                    resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } ?: error("no stream")
                }.isSuccess
            }
            _message.value = if (ok) "Backup exported." else "Export failed."
        }
    }

    fun import(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = resolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("no stream")
                    manager.importJson(text)
                }
            }
            _message.value = result.fold(
                onSuccess = { s -> "Imported ${s.foods} foods, ${s.entries} log entries, ${s.weights} weigh-ins." },
                onFailure = { "Import failed: ${it.message ?: "invalid file"}" }
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                BackupViewModel(app.backupManager)
            }
        }
    }
}
