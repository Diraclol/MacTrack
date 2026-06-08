package com.dirac.mactrack.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dirac.mactrack.ui.feature.food.FoodLogScreen
import com.dirac.mactrack.ui.feature.goals.GoalsScreen
import com.dirac.mactrack.ui.feature.more.MoreScreen
import com.dirac.mactrack.ui.feature.today.TodayScreen

@Composable
fun MacTrackApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.DASHBOARD.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.DASHBOARD.route) { TodayScreen() }
            composable(Destination.FOOD_LOG.route) { FoodLogScreen() }
            composable(Destination.GOALS.route) { GoalsScreen() }
            composable(Destination.MORE.route) { MoreScreen() }
        }
    }
}