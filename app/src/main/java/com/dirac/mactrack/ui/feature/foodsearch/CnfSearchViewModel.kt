package com.dirac.mactrack.ui.feature.foodsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.cnf.CnfFood
import com.dirac.mactrack.data.cnf.CnfRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CnfSearchViewModel(private val repo: CnfRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<CnfFood>>(emptyList())
    val results: StateFlow<List<CnfFood>> = _results.asStateFlow()

    fun onQueryChange(q: String) {
        _query.value = q
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { repo.search(q) }
            // ignore stale results if the query moved on
            if (_query.value == q) _results.value = r
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                CnfSearchViewModel(app.cnfRepository)
            }
        }
    }
}