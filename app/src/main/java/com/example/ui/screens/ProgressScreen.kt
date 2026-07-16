package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Achievement
import com.example.data.database.BodyMeasurement
import com.example.data.database.Goal
import com.example.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    viewModel: WorkoutViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val goals by viewModel.allGoals.collectAsState()
    val measurements by viewModel.allMeasurements.collectAsState()
    val achievements by viewModel.allAchievements.collectAsState()

    var activeSubTab by remember { mutableStateOf(0) } // 0: Goals, 1: Body Progress, 2: Badges

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showLogBodyDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- Header Section ---
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PROGRESS & BADGES",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- Sub-Tabs Navigation ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, Color(0x0DFFFFFF)), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Goals", "Body Stats", "Badges").forEachIndexed { index, label ->
                    val isActive = activeSubTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                            .clickable { activeSubTab = index }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                                color = if (isActive) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Active Sub-Tab Panel Render ---
        when (activeSubTab) {
            0 -> GoalsSubPanel(
                goals = goals,
                onAddGoalClick = { showAddGoalDialog = true },
                onDeleteGoal = { viewModel.deleteGoal(it) }
            )
            1 -> BodyProgressSubPanel(
                measurements = measurements,
                onLogBodyClick = { showLogBodyDialog = true },
                onDeleteMeasurement = { viewModel.deleteBodyMeasurement(it) }
            )
            2 -> BadgesSubPanel(
                achievements = achievements
            )
        }
    }

    // --- Add Goal Dialog ---
    if (showAddGoalDialog) {
        var goalTitle by remember { mutableStateOf("") }
        var goalTarget by remember { mutableStateOf("") }
        var goalCategory by remember { mutableStateOf("Exercise") } // Exercise, Frequency, Duration
        var goalUnit by remember { mutableStateOf("reps") } // reps, sessions, hours

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Create Personal Goal", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = goalTitle,
                        onValueChange = { goalTitle = it },
                        label = { Text("Goal Title (e.g. 100 Diamond Push-ups)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = goalTarget,
                            onValueChange = { goalTarget = it.filter { it.isDigit() || it == '.' } },
                            label = { Text("Target Value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = goalUnit,
                            onValueChange = { goalUnit = it },
                            label = { Text("Unit") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text("Goal Category", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Exercise", "Frequency", "Duration").forEach { cat ->
                            FilterChip(
                                selected = goalCategory == cat,
                                onClick = {
                                    goalCategory = cat
                                    goalUnit = when (cat) {
                                        "Frequency" -> "sessions"
                                        "Duration" -> "hours"
                                        else -> "reps"
                                    }
                                },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = goalTarget.toFloatOrNull() ?: 1f
                        if (goalTitle.isNotEmpty()) {
                            viewModel.addCustomGoal(goalTitle, target, goalUnit, goalCategory)
                            showAddGoalDialog = false
                        }
                    }
                ) {
                    Text("CREATE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }

    // --- Log Body Measurement Dialog ---
    if (showLogBodyDialog) {
        var weight by remember { mutableStateOf("") }
        var bodyFat by remember { mutableStateOf("") }
        var chest by remember { mutableStateOf("") }
        var waist by remember { mutableStateOf("") }
        var arms by remember { mutableStateOf("") }
        var shoulders by remember { mutableStateOf("") }
        var thigh by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showLogBodyDialog = false },
            title = { Text("Log Body Measurements", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = weight,
                                onValueChange = { weight = it },
                                label = { Text("Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = bodyFat,
                                onValueChange = { bodyFat = it },
                                label = { Text("Body Fat %") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = chest,
                                onValueChange = { chest = it },
                                label = { Text("Chest (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = waist,
                                onValueChange = { waist = it },
                                label = { Text("Waist (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = arms,
                                onValueChange = { arms = it },
                                label = { Text("Arms (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = shoulders,
                                onValueChange = { shoulders = it },
                                label = { Text("Shoulders (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = thigh,
                            onValueChange = { thigh = it },
                            label = { Text("Thigh circumference (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("General Notes") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val wVal = weight.toFloatOrNull() ?: 0f
                        if (wVal > 0) {
                            viewModel.logBodyMeasurement(
                                weight = wVal,
                                chest = chest.toFloatOrNull() ?: 0f,
                                waist = waist.toFloatOrNull() ?: 0f,
                                arms = arms.toFloatOrNull() ?: 0f,
                                shoulders = shoulders.toFloatOrNull() ?: 0f,
                                thigh = thigh.toFloatOrNull() ?: 0f,
                                bodyFat = bodyFat.toFloatOrNull() ?: 0f,
                                notes = notes
                            )
                            showLogBodyDialog = false
                        }
                    }
                ) {
                    Text("LOG")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogBodyDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

// ==========================================
// --- SUB PANEL COMPOSABLES ---
// ==========================================

@Composable
fun GoalsSubPanel(
    goals: List<Goal>,
    onAddGoalClick: () -> Unit,
    onDeleteGoal: (Goal) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onAddGoalClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CREATE CUSTOM GOAL", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, fontSize = 12.sp)
            }
        }

        if (goals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No active goals logged. Click above to set your targets!",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(goals) { goal ->
                val progressFraction = (goal.currentValue / goal.targetValue).coerceIn(0f, 1f)
                val percentText = "${(progressFraction * 100).toInt()}%"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = goal.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "CATEGORY: ${goal.category.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                )
                            }
                            IconButton(onClick = { onDeleteGoal(goal) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${goal.currentValue.toInt()} / ${goal.targetValue.toInt()} ${goal.unit.uppercase()}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )
                            Text(
                                text = percentText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = if (goal.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            trackColor = Color(0x1AFFFFFF)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BodyProgressSubPanel(
    measurements: List<BodyMeasurement>,
    onLogBodyClick: () -> Unit,
    onDeleteMeasurement: (BodyMeasurement) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Button(
                onClick = onLogBodyClick,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOG BODY MEASUREMENTS", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, fontSize = 12.sp)
            }
        }

        if (measurements.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No body measurements tracked yet. Keep notes of your weight, arms size, and fat progress!",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(measurements) { log ->
                val dateLabel = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(log.dateMillis))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateLabel.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, letterSpacing = 0.5.sp)
                            )

                            IconButton(onClick = { onDeleteMeasurement(log) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Grid Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Weight: ${log.weight} kg", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface))
                                if (log.bodyFat > 0) Text("Fat: ${log.bodyFat}%", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                if (log.arms > 0) Text("Arms: ${log.arms} cm", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (log.chest > 0) Text("Chest: ${log.chest} cm", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                if (log.waist > 0) Text("Waist: ${log.waist} cm", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                                if (log.shoulders > 0) Text("Shoulders: ${log.shoulders} cm", style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                            }
                        }

                        if (log.notes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Notes: ${log.notes}",
                                style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BadgesSubPanel(
    achievements: List<Achievement>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "MY WORKOUT ACHIEVEMENTS",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp, color = MaterialTheme.colorScheme.onBackground)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Using beautiful custom rows to present badges. This works perfectly inside LazyColumn.
        items(achievements) { badge ->
            val isUnlocked = badge.unlockedDateMillis > 0
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val dateLabel = if (isUnlocked) "Unlocked: ${sdf.format(Date(badge.unlockedDateMillis))}" else "Locked"

            // Choose beautiful vector icons based on the stored name
            val badgeIcon = when (badge.iconName) {
                "local_fire_department" -> Icons.Default.LocalFireDepartment
                "fitness_center" -> Icons.Default.FitnessCenter
                "military_tech" -> Icons.Default.MilitaryTech
                "star" -> Icons.Default.Star
                "workspace_premium" -> Icons.Default.WorkspacePremium
                "emoji_events" -> Icons.Default.EmojiEvents
                else -> Icons.Default.EmojiEvents
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUnlocked) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, if (isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0x0DFFFFFF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                if (isUnlocked) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                } else {
                                    Color(0x0DFFFFFF)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = if (isUnlocked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = badge.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = badge.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isUnlocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dateLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
                            color = if (isUnlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}
