package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.WorkoutExercise
import com.example.viewmodel.WorkoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    viewModel: WorkoutViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val activeSession by viewModel.activeSession.collectAsState()
    val activeExercises by viewModel.activeExercises.collectAsState()
    val workoutTimer by viewModel.workoutTimerSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    
    val restTimer by viewModel.restTimerSeconds.collectAsState()
    val isRestTimerRunning by viewModel.isRestTimerRunning.collectAsState()

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf("Medium") }
    var sessionNotes by remember { mutableStateOf("") }

    if (activeSession == null) {
        // Workout is idle, show quick selectors to start a workout manually
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "No Active Workout",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Select a workout day to begin tracking your reps, sets, and weight in real-time.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Selection Buttons - Styled beautifully as cards with borders
                listOf(
                    1 to "START DAY 1: CHEST & TRICEPS",
                    2 to "START DAY 2: BACK & BICEPS",
                    3 to "START DAY 3: SHOULDERS & SQUATS"
                ).forEach { (day, title) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { viewModel.startWorkout(day) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    } else {
        // Active Workout Logging
        val formattedTime = remember(workoutTimer) {
            val h = workoutTimer / 3600
            val m = (workoutTimer % 3600) / 60
            val s = workoutTimer % 60
            if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
        }

        Scaffold(
            bottomBar = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(20.dp)
                    ) {
                        // Difficulty Selection
                        Text(
                            text = "WORKOUT DIFFICULTY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Easy", "Medium", "Hard").forEach { diff ->
                                val isSelected = selectedDifficulty == diff
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedDifficulty = diff },
                                    label = { Text(diff) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.Black
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Workout Notes
                        OutlinedTextField(
                            value = sessionNotes,
                            onValueChange = { sessionNotes = it },
                            placeholder = { Text("Log any workout notes, feelings, or observations...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0x19FFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.discardActiveWorkout() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("DISCARD", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            Button(
                                onClick = { viewModel.finishAndSaveWorkout(selectedDifficulty, sessionNotes) },
                                modifier = Modifier.weight(1.5f).height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SAVE WORKOUT", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Timer / Clock Header
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ACTIVE SESSION",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formattedTime,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 38.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Pause/Play Timer
                                IconButton(
                                    onClick = { viewModel.toggleWorkoutTimer() },
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                                        .size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Timer toggle",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // Rest Timer Banner
                if (isRestTimerRunning) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "REST COUNTDOWN",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.secondary,
                                                letterSpacing = 0.5.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Take a breath... $restTimer seconds left",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                                Text(
                                    text = "${restTimer}s",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.secondary
                                    ),
                                    modifier = Modifier.clickable { viewModel.stopRestTimer() }
                                )
                            }
                        }
                    }
                }

                // Exercises List Section Title
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EXERCISES (${activeExercises.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                        )

                        TextButton(
                            onClick = { showAddExerciseDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ADD EXERCISE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                // Active Exercises rendering
                itemsIndexed(activeExercises) { exIdx, ex ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Header: Exercise Name & Complete checkbox
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${ex.muscleGroup.uppercase()} • TARGET: ${ex.targetSets} SETS x ${ex.targetReps} REPS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    )
                                }

                                Checkbox(
                                    checked = ex.isCompleted,
                                    onCheckedChange = { viewModel.toggleExerciseCompletion(exIdx) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        checkmarkColor = Color.Black
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Interactive log header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SET", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                                Text("REPS", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold), modifier = Modifier.weight(2f), textAlign = TextAlign.Center)
                                Text("WEIGHT (KG)", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold), modifier = Modifier.weight(2f), textAlign = TextAlign.Center)
                                Text("STATUS", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold), modifier = Modifier.weight(1.5f), textAlign = TextAlign.End)
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = Color(0x0DFFFFFF)
                            )

                            // Generate rows matching target sets
                            for (setIdx in 0 until ex.targetSets) {
                                // Parse previously stored set weights/reps
                                val completedRepsList = if (ex.completedReps.isEmpty()) emptyList() else ex.completedReps.split(",")
                                val completedWeightList = if (ex.completedWeight.isEmpty()) emptyList() else ex.completedWeight.split(",")

                                val initialRepsStr = completedRepsList.getOrNull(setIdx) ?: ""
                                val initialWeightStr = completedWeightList.getOrNull(setIdx) ?: ""

                                var repsText by remember(ex.completedReps) { mutableStateOf(if (initialRepsStr == "0") "" else initialRepsStr) }
                                var weightText by remember(ex.completedWeight) { mutableStateOf(if (initialWeightStr == "0.0" || initialWeightStr == "0") "" else initialWeightStr) }

                                val isSetChecked = initialRepsStr.isNotEmpty() && initialRepsStr != "0"

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Set Number Label
                                    Text(
                                        text = "${setIdx + 1}",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSetChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Reps Input Box
                                    OutlinedTextField(
                                        value = repsText,
                                        onValueChange = { input ->
                                            repsText = input.filter { it.isDigit() }
                                            val reps = repsText.toIntOrNull() ?: 0
                                            val weight = weightText.toFloatOrNull() ?: 0f
                                            viewModel.updateActiveExerciseSet(exIdx, setIdx, reps, weight)
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(2f)
                                            .height(44.dp)
                                            .padding(horizontal = 4.dp),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = Color(0x19FFFFFF),
                                            focusedContainerColor = Color(0x05FFFFFF),
                                            unfocusedContainerColor = Color.Transparent
                                        )
                                    )

                                    // Weight Input Box
                                    OutlinedTextField(
                                        value = weightText,
                                        onValueChange = { input ->
                                            weightText = input.filter { it.isDigit() || it == '.' }
                                            val reps = repsText.toIntOrNull() ?: 0
                                            val weight = weightText.toFloatOrNull() ?: 0f
                                            viewModel.updateActiveExerciseSet(exIdx, setIdx, reps, weight)
                                        },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(2f)
                                            .height(44.dp)
                                            .padding(horizontal = 4.dp),
                                        singleLine = true,
                                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = Color(0x19FFFFFF),
                                            focusedContainerColor = Color(0x05FFFFFF),
                                            unfocusedContainerColor = Color.Transparent
                                        )
                                    )

                                    // Check indicator
                                    Box(
                                        modifier = Modifier
                                            .weight(1.5f)
                                            .clickable {
                                                if (isSetChecked) {
                                                    // Clear it
                                                    repsText = ""
                                                    weightText = ""
                                                    viewModel.updateActiveExerciseSet(exIdx, setIdx, 0, 0f)
                                                } else {
                                                    // Complete set with target reps and bodyweight
                                                    repsText = "${ex.targetReps}"
                                                    weightText = "0"
                                                    viewModel.updateActiveExerciseSet(exIdx, setIdx, ex.targetReps, 0f)
                                                    
                                                    // Automatically start Rest Timer for 60 seconds!
                                                    viewModel.startRestTimer(60)
                                                }
                                            },
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = if (isSetChecked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSetChecked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Add Custom Exercise Dialog ---
    if (showAddExerciseDialog) {
        var newExName by remember { mutableStateOf("") }
        var newExMuscle by remember { mutableStateOf("Chest") }
        var newExSets by remember { mutableStateOf("3") }
        var newExReps by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("Add Custom Exercise", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newExName,
                        onValueChange = { newExName = it },
                        label = { Text("Exercise Name (e.g. Weighted Dips)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Muscle Group selection
                    Text("Muscle Group", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Chest", "Back", "Shoulders", "Legs", "Core").forEach { m ->
                            FilterChip(
                                selected = newExMuscle == m,
                                onClick = { newExMuscle = m },
                                label = { Text(m) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newExSets,
                            onValueChange = { newExSets = it.filter { it.isDigit() } },
                            label = { Text("Target Sets") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newExReps,
                            onValueChange = { newExReps = it.filter { it.isDigit() } },
                            label = { Text("Target Reps") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newExName.isNotEmpty()) {
                            val sets = newExSets.toIntOrNull() ?: 3
                            val reps = newExReps.toIntOrNull() ?: 10
                            viewModel.addCustomExerciseToActiveWorkout(newExName, newExMuscle, sets, reps)
                            showAddExerciseDialog = false
                        }
                    }
                ) {
                    Text("ADD")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
