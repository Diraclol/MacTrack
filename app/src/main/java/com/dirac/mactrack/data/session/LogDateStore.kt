package com.dirac.mactrack.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

// In-memory session state: which day the food log is currently viewing / logging to. Not
// persisted (defaults to today on each app launch). The food log sets it via the day navigator;
// the log actions (cart, quick add, food detail) read it so new entries land on the viewed day.
class LogDateStore {
    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    fun current(): LocalDate = _date.value
    fun set(d: LocalDate) { _date.value = d }
    fun today() { _date.value = LocalDate.now() }
}
