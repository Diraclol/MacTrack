package com.dirac.mactrack.ui.feature.food

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun FoodLogScreen(modifier: Modifier = Modifier) {
    val viewModel: FoodViewModel = viewModel(factory = FoodViewModel.Factory)
    val foods by viewModel.foods.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { viewModel.addSampleFood() }) {
            Text("Add sample food")
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(foods) { food ->
                Text("${food.name} — ${food.calories} cal, ${food.proteinG}g protein")
            }
        }
    }
}