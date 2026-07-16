package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnimatedBarChart
import com.example.ui.components.AnimatedDonutChart
import com.example.ui.components.AnimatedLineChart
import com.example.viewmodel.WorkoutViewModel
import java.util.*

@Composable
fun AnalyticsScreen(
    viewModel: WorkoutViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val sessions by viewModel.allSessions.collectAsState()
    val exercises by viewModel.allExercises.collectAsState()

    // --- Compute Muscle Group Distribution ---
    val muscleGroupSlices = remember(exercises) {
        val groups = exercises.filter { it.isCompleted }.groupBy { it.muscleGroup }
        if (groups.isEmpty()) {
            listOf(
                Pair("Chest", 1f),
                Pair("Back", 1f),
                Pair("Shoulders", 1f),
                Pair("Legs", 1f),
                Pair("Core", 1f)
            )
        } else {
            groups.map { (muscle, list) ->
                Pair(muscle, list.size.toFloat())
            }
        }
    }
    
    val donutColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFFFF9100), // Custom Orange
        Color(0xFFE040FB)  // Custom Purple
    )

    // --- Compute Weekly Workout Frequency (Sun-Sat) ---
    val weeklyFrequencyData = remember(sessions) {
        val counts = FloatArray(7) { 0f }
        val calendar = Calendar.getInstance()
        
        sessions.forEach { session ->
            calendar.timeInMillis = session.dateMillis
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 7 = Sat
            counts[dayOfWeek - 1] += 1f
        }
        
        counts.toList()
    }
    val weeklyLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    // --- Compute Training Volume Progression over last 7 workouts ---
    val trainingVolumeData = remember(sessions, exercises) {
        val nonRestSessions = sessions.filter { !it.isRestDay }.sortedBy { it.dateMillis }
        val lastSessions = nonRestSessions.takeLast(7)
        
        lastSessions.map { s ->
            val sessionExs = exercises.filter { it.sessionId == s.id && it.isCompleted }
            // volume = sum of (completedSets * targetReps) or actual logged reps
            sessionExs.sumOf {
                val repSum = if (it.completedReps.isEmpty()) 0 else it.completedReps.split(",").sumOf { r -> r.toIntOrNull() ?: 0 }
                if (repSum > 0) repSum else (it.completedSets * it.targetReps)
            }.toFloat()
        }
    }

    val volumeLabels = remember(trainingVolumeData) {
        List(trainingVolumeData.size) { "#${it + 1}" }
    }

    // --- Compute general stats ---
    val totalCalories = remember(sessions) { sessions.sumOf { it.caloriesBurned } }
    val avgDuration = remember(sessions) {
        val completed = sessions.filter { !it.isRestDay }
        if (completed.isEmpty()) 0 else (completed.map { it.durationSeconds }.sum() / completed.size) / 60
    }
    val totalDurationHours = remember(sessions) {
        (sessions.map { it.durationSeconds }.sum() / 3600f)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- Header ---
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PERFORMANCE ANALYTICS",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
        }

        if (sessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxSize(0.8f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Analytics Awaiting Fuel",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Complete workout sessions in Day 1, 2, or 3 to feed active metrics, trends, and charts.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        } else {
            // --- Core KPI Cards Summary ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Total Calories".uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$totalCalories kcal", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Avg Duration".uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$avgDuration mins", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary))
                        }
                    }
                }
            }

            // --- Muscle Group Target Distribution (Pie/Donut Chart) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MUSCLE WORKLOAD",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        AnimatedDonutChart(
                            slices = muscleGroupSlices,
                            colors = donutColors
                        )
                    }
                }
            }

            // --- Weekly Frequency (Bar Chart) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "WEEKLY FREQUENCY",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        AnimatedBarChart(
                            data = weeklyFrequencyData,
                            labels = weeklyLabels,
                            barColor = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // --- Training Volume Growth (Line Chart) ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TRAINING VOLUME TREND",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        if (trainingVolumeData.size < 2) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Log at least 2 workouts to plot volume trends",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                )
                            }
                        } else {
                            AnimatedLineChart(
                                data = trainingVolumeData,
                                labels = volumeLabels,
                                lineColor = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // --- Additional Stats details card ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "PERFORMANCE SUMMARY",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Hours Trained", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(String.format("%.2f hrs", totalDurationHours), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0x0DFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Consistency Score", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            val consistencyPercent = ((sessions.size / 30f) * 100).coerceAtMost(100f).toInt()
                            Text("$consistencyPercent%", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0x0DFFFFFF))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recovery Score", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            val hasRestDays = sessions.count { it.isRestDay }
                            val restRatio = if (sessions.isEmpty()) 0 else (hasRestDays * 100 / sessions.size)
                            Text("$restRatio/100", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary))
                        }
                    }
                }
            }
        }
    }
}
