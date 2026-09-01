package com.dirac.mactrack.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dirac.mactrack.ui.feature.dashboard.DashboardScreen
import com.dirac.mactrack.ui.feature.library.LibraryScreen
import com.dirac.mactrack.ui.feature.goals.GoalsScreen
import com.dirac.mactrack.ui.feature.goals.ReassessGoalsScreen
import com.dirac.mactrack.ui.feature.more.MoreScreen
import com.dirac.mactrack.ui.feature.today.TodayScreen
import com.dirac.mactrack.ui.feature.food.CreateFoodScreen
import com.dirac.mactrack.ui.feature.meals.MealsScreen
import com.dirac.mactrack.ui.feature.recipes.RecipesScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.dirac.mactrack.ui.feature.foodsearch.UnifiedSearchScreen
import com.dirac.mactrack.ui.feature.foodsearch.FoodDetailScreen

@Composable
fun MacTrackApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabRoutes = Destination.entries.map { it.route }
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Transparent,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Destination.entries.forEach { destination ->
                            val selected = currentRoute == destination.route
                            val tint = if (selected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                                    )
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Icon(destination.icon, contentDescription = destination.label, tint = tint)
                                Text(destination.label, style = MaterialTheme.typography.labelSmall, color = tint)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.DASHBOARD.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.DASHBOARD.route) { DashboardScreen() }
            composable(Destination.FOOD_LOG.route) {
                TodayScreen(
                    onOpenSearch = { navController.navigate("food_search") },
                    onOpenEntry = { entryId -> navController.navigate("food_detail/entry/$entryId") }
                )
            }
            composable(Destination.MORE.route) {
                MoreScreen(
                    onOpenLibrary = { navController.navigate("library") },
                    onOpenGoals = { navController.navigate("goals") },
                    onReassessGoals = { navController.navigate("reassess_goals") },
                )
            }
            composable("library") {
                LibraryScreen(
                    onBack = { navController.popBackStack() },
                    onCreateFood = { navController.navigate("create_food") },
                    onCreateMeal = { navController.navigate("create_meal") },
                    onCreateRecipe = { navController.navigate("create_recipe") },
                    onOpenFood = { id -> navController.navigate("food_detail/custom/$id") }
                )
            }
            composable("create_food") {
                CreateFoodScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("create_meal") { MealsScreen(onBack = { navController.popBackStack() }, showBar = true) }
            composable("create_recipe") { RecipesScreen(onBack = { navController.popBackStack() }, showBar = true) }
            composable("goals") { GoalsScreen(onBack = { navController.popBackStack() }) }
            composable("reassess_goals") { ReassessGoalsScreen(onBack = { navController.popBackStack() }) }
            composable("food_search") {
                UnifiedSearchScreen(
                    onOpenFood = { source, id -> navController.navigate("food_detail/$source/$id") },
                    onLoggedCart = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                "food_detail/{source}/{id}",
                arguments = listOf(
                    navArgument("source") { type = NavType.StringType },
                    navArgument("id") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val source = backStackEntry.arguments?.getString("source") ?: ""
                val id = backStackEntry.arguments?.getString("id") ?: ""
                FoodDetailScreen(
                    source = source,
                    id = id,
                    onLogged = {
                        navController.navigate(Destination.FOOD_LOG.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    onAdded = { navController.popBackStack() },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}