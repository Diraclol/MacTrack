package com.dirac.mactrack.ui.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dirac.mactrack.MacTrackApplication
import com.dirac.mactrack.data.entity.UserProfile
import com.dirac.mactrack.data.repository.UserProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(private val repository: UserProfileRepository) : ViewModel() {

    val profile: StateFlow<UserProfile?> = repository.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun setBodyFat(pct: Double?) {
        viewModelScope.launch { repository.setBodyFat(pct) }
    }

    fun saveProfile(
        sex: String,
        age: Int,
        weightKg: Double,
        heightCm: Double,
        activityLevel: String,
        goalType: String,
        proteinLevel: String,
        fatLevel: String
    ) {
        viewModelScope.launch {
            repository.saveProfile(
                UserProfile(
                    sex = sex,
                    age = age,
                    weightKg = weightKg,
                    heightCm = heightCm,
                    activityLevel = activityLevel,
                    goalType = goalType,
                    proteinLevel = proteinLevel,
                    fatLevel = fatLevel
                )
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as MacTrackApplication
                ProfileViewModel(app.userProfileRepository)
            }
        }
    }
}