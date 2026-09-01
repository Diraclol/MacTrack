package com.dirac.mactrack.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Filled.Home),
    FOOD_LOG("food_log", "Food Log", Icons.Filled.Restaurant),
    AI("ai", "AI", Icons.Filled.AutoAwesome),
    MORE("more", "More", Icons.Filled.Menu)
}