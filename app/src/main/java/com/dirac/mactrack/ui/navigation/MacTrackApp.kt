package com.dirac.mactrack.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dirac.mactrack.ui.theme.StartScreen
import com.dirac.mactrack.ui.theme.ThemeViewModel
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
import com.dirac.mactrack.ui.feature.profile.ProfileScreen
import com.dirac.mactrack.ui.feature.more.MoreScreen
import com.dirac.mactrack.ui.feature.today.TodayScreen
import com.dirac.mactrack.ui.feature.food.CreateFoodScreen
import com.dirac.mactrack.ui.feature.meals.MealsScreen
import com.dirac.mactrack.ui.feature.recipes.RecipesScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.dirac.mactrack.ui.feature.foodsearch.UnifiedSearchScreen
import com.dirac.mactrack.ui.feature.foodsearch.FoodDetailScreen
import com.dirac.mactrack.ui.feature.weight.WeightScreen
import com.dirac.mactrack.ui.feature.trends.TrendsScreen
import com.dirac.mactrack.ui.feature.nutrient.NutrientDetailScreen

@Composable
fun MacTrackApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val tabRoutes = Destination.entries.map { it.route }
    val showBottomBar = currentRoute in tabRoutes

    // Which tab the app opens on (a setting). Read once so it only applies at launch.
    val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModel.Factory)
    val startRoute = remember {
        if (themeViewModel.startScreen.value == StartScreen.FOOD_LOG) Destination.FOOD_LOG.route
        else Destination.DASHBOARD.route
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Destination.entries.forEach { destination ->
                            val selected = currentRoute == destination.route
                            val tint = if (selected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            // Each tab gets an equal third and centers its content, so unequal label
                            // widths (Dashboard vs More) don't push the row off-center.
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
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
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Destination.DASHBOARD.route) {
                DashboardScreen(
                    onOpenProfile = { navController.navigate("profile") },
                    onOpenTrends = { navController.navigate("trends") }
                )
            }
            composable("trends") {
                TrendsScreen(onBack = { navController.popBackStack() })
            }
            composable(Destination.FOOD_LOG.route) {
                TodayScreen(
                    onOpenSearch = { navController.navigate("food_search") },
                    onOpenEntry = { entryId -> navController.navigate("food_detail/entry/$entryId") },
                    onOpenNutrient = { key -> navController.navigate("nutrient_detail/$key") }
                )
            }
            composable(
                "nutrient_detail/{nutrient}",
                arguments = listOf(navArgument("nutrient") { type = NavType.StringType })
            ) { backStackEntry ->
                val nutrient = backStackEntry.arguments?.getString("nutrient") ?: "sodium"
                NutrientDetailScreen(nutrientKey = nutrient, onBack = { navController.popBackStack() })
            }
            composable(Destination.MORE.route) {
                MoreScreen(
                    onOpenLibrary = { navController.navigate("library") },
                    onOpenGoals = { navController.navigate("goals") },
                    onOpenProfile = { navController.navigate("profile") },
                    onOpenWeight = { navController.navigate("weight") },
                )
            }
            composable("weight") {
                WeightScreen(onBack = { navController.popBackStack() })
            }
            composable("library") {
                LibraryScreen(
                    onBack = { navController.popBackStack() },
                    onCreateFood = { navController.navigate("create_food") },
                    onCreateMeal = { navController.navigate("create_meal") },
                    onCreateRecipe = { navController.navigate("create_recipe") },
                    onOpenFood = { id -> navController.navigate("food_detail/custom/$id") },
                    onOpenRecipe = { id -> navController.navigate("food_detail/recipe/$id") }
                )
            }
            composable("create_food") {
                CreateFoodScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("create_meal") {
                MealsScreen(
                    onBack = { navController.popBackStack() },
                    onAddFoods = { navController.navigate("food_search?picker=meal") },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("create_recipe") {
                RecipesScreen(
                    onBack = { navController.popBackStack() },
                    onAddIngredients = { navController.navigate("food_search?picker=recipe") },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable("goals") {
                GoalsScreen(
                    onBack = { navController.popBackStack() },
                    onReassess = { navController.navigate("reassess_goals") }
                )
            }
            composable("reassess_goals") { ReassessGoalsScreen(onBack = { navController.popBackStack() }) }
            composable("profile") {
                ProfileScreen(
                    onBack = { navController.popBackStack() },
                    onReassessGoals = { navController.navigate("reassess_goals") }
                )
            }
            composable(
                "food_search?picker={picker}",
                arguments = listOf(navArgument("picker") { type = NavType.StringType; defaultValue = "" })
            ) { backStackEntry ->
                val picker = backStackEntry.arguments?.getString("picker") ?: ""
                UnifiedSearchScreen(
                    onOpenFood = { source, id -> navController.navigate("food_detail/$source/$id") },
                    onLoggedCart = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                    picker = picker,
                    onDonePicking = { navController.popBackStack() },
                    onCreateFood = { navController.navigate("create_food") },
                    onCreateMeal = { navController.navigate("create_meal") },
                    onCreateRecipe = { navController.navigate("create_recipe") }
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