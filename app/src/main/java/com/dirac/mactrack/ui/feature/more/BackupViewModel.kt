package com.dirac.mactrack.ui.feature.more

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
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
                    val size = fileSize(uri, resolver)
                    if (size > MAX_IMPORT_BYTES) error("File too large (max 25 MB).")
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

    // Best-effort size of the content URI in bytes; -1 (unknown) is allowed through.
    private fun fileSize(uri: Uri, resolver: ContentResolver): Long = try {
        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            val idx = c.getColumnIndex(OpenableColumns.SIZE)
            if (c.moveToFirst() && idx >= 0 && !c.isNull(idx)) c.getLong(idx) else -1L
        } ?: -1L
    } catch (e: Exception) {
        -1L
    }

    companion object {
        // Cap import files at 25 MB before reading them whole (SECURITY.md 2.3).
        private const val MAX_IMPORT_BYTES = 25L * 1024 * 1024

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                BackupViewModel(app.backupManager)
            }
        }
    }
}
