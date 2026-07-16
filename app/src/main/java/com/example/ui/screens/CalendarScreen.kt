package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.WorkoutSession
import com.example.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: WorkoutViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val sessions by viewModel.allSessions.collectAsState()
    val exercises by viewModel.allExercises.collectAsState()

    val searchQuery by viewModel.searchQuery.collectAsState()
    val muscleFilter by viewModel.muscleFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    var selectedCalendarDate by remember { mutableStateOf(Calendar.getInstance()) }
    val currentMonthCalendar = remember { Calendar.getInstance() }

    var currentMonth by remember { mutableStateOf(currentMonthCalendar.get(Calendar.MONTH)) }
    var currentYear by remember { mutableStateOf(currentMonthCalendar.get(Calendar.YEAR)) }

    // State to toggle display between Calendar Grid view and standard List view
    var isListView by remember { mutableStateOf(false) }

    // Processed history list based on search, filters and sort order
    val filteredSessions = remember(sessions, exercises, searchQuery, muscleFilter, sortOrder) {
        var list = sessions.toMutableList()

        // Filter by Search Query
        if (searchQuery.isNotEmpty()) {
            list = list.filter { session ->
                val sessionExs = exercises.filter { it.sessionId == session.id }
                session.notes.contains(searchQuery, ignoreCase = true) ||
                sessionExs.any { it.name.contains(searchQuery, ignoreCase = true) }
            }.toMutableList()
        }

        // Filter by Muscle Group
        if (muscleFilter != "All") {
            list = list.filter { session ->
                val sessionExs = exercises.filter { it.sessionId == session.id }
                sessionExs.any { it.muscleGroup.equals(muscleFilter, ignoreCase = true) }
            }.toMutableList()
        }

        // Sort
        when (sortOrder) {
            "Newest" -> list.sortByDescending { it.dateMillis }
            "Oldest" -> list.sortBy { it.dateMillis }
            "Longest Workout" -> list.sortByDescending { it.durationSeconds }
            "Highest Volume" -> {
                list.sortByDescending { session ->
                    val sessionExs = exercises.filter { it.sessionId == session.id }
                    sessionExs.sumOf { it.completedSets * it.targetReps }
                }
            }
        }

        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // --- Header Switch Row ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calendar & History",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black)
                )

                Row {
                    IconButton(onClick = { isListView = false }) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Calendar View",
                            tint = if (!isListView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = { isListView = true }) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "List View",
                            tint = if (isListView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }

        if (!isListView) {
            // --- Custom Interactive Calendar Grid ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Month Selector Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.MONTH, currentMonth)
                                set(Calendar.YEAR, currentYear)
                            }
                            val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)

                            Text(
                                text = monthLabel.uppercase(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Row {
                                IconButton(
                                    onClick = {
                                        if (currentMonth == 0) {
                                            currentMonth = 11
                                            currentYear--
                                        } else {
                                            currentMonth--
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Prev Month", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(
                                    onClick = {
                                        if (currentMonth == 11) {
                                            currentMonth = 0
                                            currentYear++
                                        } else {
                                            currentMonth++
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Weekdays Headers
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Days grid
                        val tempCal = Calendar.getInstance().apply {
                            set(Calendar.MONTH, currentMonth)
                            set(Calendar.YEAR, currentYear)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        val firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1
                        val maxDays = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH)

                        var dayCounter = 1

                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (row in 0..5) {
                                if (dayCounter > maxDays) break
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    for (col in 0..6) {
                                        val cellIndex = row * 7 + col
                                        if (cellIndex < firstDayOfWeek || dayCounter > maxDays) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        } else {
                                            val thisDay = dayCounter
                                            val cellDate = Calendar.getInstance().apply {
                                                set(Calendar.MONTH, currentMonth)
                                                set(Calendar.YEAR, currentYear)
                                                set(Calendar.DAY_OF_MONTH, thisDay)
                                            }
                                            val cellDateStr = sdf.format(cellDate.time)

                                            // Determine if this day has sessions or is rest day
                                            val sessionsOnDay = sessions.filter {
                                                sdf.format(Date(it.dateMillis)) == cellDateStr
                                            }
                                            val isRestDay = sessionsOnDay.any { it.isRestDay }
                                            val isCompletedWorkout = sessionsOnDay.any { !it.isRestDay }

                                            val isSelected = sdf.format(selectedCalendarDate.time) == cellDateStr

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .aspectRatio(1f)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            isSelected -> MaterialTheme.colorScheme.primary
                                                            isCompletedWorkout -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                            isRestDay -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .clickable {
                                                        selectedCalendarDate = cellDate
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$thisDay",
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = if (isSelected || isCompletedWorkout || isRestDay) FontWeight.Bold else FontWeight.Normal,
                                                        color = when {
                                                            isSelected -> Color.Black
                                                            isCompletedWorkout -> MaterialTheme.colorScheme.primary
                                                            isRestDay -> MaterialTheme.colorScheme.secondary
                                                            else -> MaterialTheme.colorScheme.onSurface
                                                        }
                                                    )
                                                )
                                            }
                                            dayCounter++
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Sessions on the selected Date list
            item {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val targetDateStr = sdf.format(selectedCalendarDate.time)
                val targetDayName = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(selectedCalendarDate.time)

                val selectedSessions = sessions.filter {
                    sdf.format(Date(it.dateMillis)) == targetDateStr
                }

                Column {
                    Text(
                        text = "WORKOUTS FOR $targetDayName",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedSessions.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No workout logs for this date.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                )
                            }
                        }
                    } else {
                        selectedSessions.forEach { session ->
                            WorkoutDetailCard(session, exercises, viewModel)
                        }
                    }
                }
            }
        } else {
            // --- Standard History List View with Filters ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0x0DFFFFFF))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Search Outlined Box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Search by exercise or notes...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0x19FFFFFF)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Filters Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Muscle Group Filter Chip
                            var showMuscleMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedCard(
                                    onClick = { showMuscleMenu = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0x19FFFFFF)),
                                    colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Muscle: $muscleFilter", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                DropdownMenu(expanded = showMuscleMenu, onDismissRequest = { showMuscleMenu = false }) {
                                    listOf("All", "Chest", "Back", "Shoulders", "Legs", "Core").forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m) },
                                            onClick = {
                                                viewModel.setMuscleFilter(m)
                                                showMuscleMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Sort Order Filter Chip
                            var showSortMenu by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedCard(
                                    onClick = { showSortMenu = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0x19FFFFFF)),
                                    colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(sortOrder, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                    listOf("Newest", "Oldest", "Longest Workout", "Highest Volume").forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s) },
                                            onClick = {
                                                viewModel.setSortOrder(s)
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // List elements
            if (filteredSessions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No workouts match your filter criteria.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        )
                    }
                }
            } else {
                items(filteredSessions) { session ->
                    WorkoutDetailCard(session, exercises, viewModel)
                }
            }
        }
    }
}

@Composable
fun WorkoutDetailCard(
    session: WorkoutSession,
    allExercises: List<com.example.data.database.WorkoutExercise>,
    viewModel: WorkoutViewModel
) {
    val sessionExs = remember(session, allExercises) {
        allExercises.filter { it.sessionId == session.id }
    }
    var isExpanded by remember { mutableStateOf(false) }

    val formattedDate = remember(session.dateMillis) {
        val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        sdf.format(Date(session.dateMillis))
    }

    val title = when (session.dayNumber) {
        1 -> "Day 1: Chest, Triceps & Squats"
        2 -> "Day 2: Back, Biceps & Core"
        3 -> "Day 3: Shoulders & Squats"
        4 -> "Day 4: Rest & Recovery"
        else -> "Custom Workout"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0x0DFFFFFF))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand details",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Info summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!session.isRestDay) {
                    Text(
                        text = "⏱️ ${session.durationSeconds / 60} MINS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                    Text(
                        text = "🔥 ${session.caloriesBurned} KCAL",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    )
                }
                Text(
                    text = "💪 ${session.difficulty.uppercase()}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = when (session.difficulty) {
                            "Easy" -> MaterialTheme.colorScheme.secondary
                            "Hard" -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                    )
                )
            }

            if (session.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${session.notes}",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded details: full exercises logged
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    if (session.isRestDay) {
                        Text(
                            text = "Rest day completed! Your body appreciated the hydration, light stretches, and recovery sleep.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Text(
                            text = "LOGGED EXERCISES",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        sessionExs.forEach { ex ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "• ${ex.name} (${ex.muscleGroup})",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    )
                                    Text(
                                        text = "${ex.completedSets} sets",
                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    )
                                }

                                // If sets details are loaded, display them
                                if (ex.completedReps.isNotEmpty() && ex.completedReps != "0") {
                                    val reps = ex.completedReps.split(",")
                                    val weights = if (ex.completedWeight.isEmpty()) emptyList() else ex.completedWeight.split(",")
                                    
                                    val setStrings = reps.mapIndexed { index, r ->
                                        val w = weights.getOrNull(index) ?: "0"
                                        val weightStr = if (w == "0.0" || w == "0") "Bodyweight" else "${w}kg"
                                        "Set ${index + 1}: ${r} reps @ $weightStr"
                                    }.joinToString(", ")

                                    Text(
                                        text = setStrings,
                                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Session action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.deleteWorkoutSession(session) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.duplicateWorkoutSession(session) },
                            modifier = Modifier.weight(1f).height(40.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Duplicate Today", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
