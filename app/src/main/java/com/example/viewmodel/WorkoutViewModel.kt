package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.database.*
import com.example.data.repository.WorkoutRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WorkoutViewModel(
    application: Application,
    private val repository: WorkoutRepository
) : AndroidViewModel(application) {

    // --- Core Flows ---
    val allSessions: StateFlow<List<WorkoutSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<List<Goal>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMeasurements: StateFlow<List<BodyMeasurement>> = repository.allMeasurements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAchievements: StateFlow<List<Achievement>> = repository.allAchievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: StateFlow<List<WorkoutExercise>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Workout State ---
    private val _activeSession = MutableStateFlow<WorkoutSession?>(null)
    val activeSession: StateFlow<WorkoutSession?> = _activeSession.asStateFlow()

    private val _activeExercises = MutableStateFlow<List<WorkoutExercise>>(emptyList())
    val activeExercises: StateFlow<List<WorkoutExercise>> = _activeExercises.asStateFlow()

    // --- Active Timers ---
    private val _workoutTimerSeconds = MutableStateFlow(0L)
    val workoutTimerSeconds: StateFlow<Long> = _workoutTimerSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private val _restTimerSeconds = MutableStateFlow(0)
    val restTimerSeconds: StateFlow<Int> = _restTimerSeconds.asStateFlow()

    private val _isRestTimerRunning = MutableStateFlow(false)
    val isRestTimerRunning: StateFlow<Boolean> = _isRestTimerRunning.asStateFlow()

    private var workoutTimerJob: Job? = null
    private var restTimerJob: Job? = null

    // --- App Navigation / Active Screen State ---
    private val _currentTab = MutableStateFlow(0) // 0: Dashboard, 1: Workout, 2: History/Calendar, 3: Analytics, 4: Goals/Body, 5: Settings
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // --- History Filtering State ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _muscleFilter = MutableStateFlow("All")
    val muscleFilter: StateFlow<String> = _muscleFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow("Newest") // "Newest", "Oldest", "Longest Workout", "Highest Volume"
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    init {
        // Automatically check if an active (incomplete) session exists in the DB on startup
        viewModelScope.launch {
            val sessions = repository.getAllSessionsList()
            // Find the latest incomplete session if any (defined as 0 duration and recent, or we can just let users start fresh)
            // For simplicity, we manage the active session in-memory, allowing it to persist across screens.
        }
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    // --- Workout Cycle Calculation Helper ---
    // Returns the calculated day in the 4-day cycle for today (1, 2, 3, or 4)
    fun calculateTodayCycleDay(): Int {
        val sessions = allSessions.value.filter { !it.isRestDay }
        if (sessions.isEmpty()) return 1 // Start with Day 1

        val lastCompletedSession = sessions.maxByOrNull { it.dateMillis } ?: return 1
        val lastDayNum = lastCompletedSession.dayNumber
        
        // Cycle is Day 1 -> Day 2 -> Day 3 -> Day 4 (Rest) -> Day 1
        return when (lastDayNum) {
            1 -> 2
            2 -> 3
            3 -> 4
            4 -> 1
            else -> 1
        }
    }

    // --- Active Workout Actions ---
    fun startWorkout(dayNumber: Int) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            
            if (dayNumber == 4) {
                // Rest Day is immediately completed or logged
                val session = WorkoutSession(
                    dateMillis = now,
                    dayNumber = 4,
                    durationSeconds = 0,
                    caloriesBurned = 0,
                    difficulty = "Easy",
                    notes = "Rest and recovery day.",
                    isRestDay = true
                )
                repository.insertSession(session)
                // Trigger achievements / streak updates
                scanAndUnlockAchievements()
                return@launch
            }

            // Create and start an active workout session in memory
            val session = WorkoutSession(
                dateMillis = now,
                dayNumber = dayNumber,
                durationSeconds = 0,
                caloriesBurned = 0,
                difficulty = "Medium",
                notes = ""
            )
            
            _activeSession.value = session
            _activeExercises.value = repository.getDefaultExercisesForDay(dayNumber)
            _workoutTimerSeconds.value = 0
            startWorkoutTimer()
            _currentTab.value = 1 // Switch to active workout tab
        }
    }

    fun addCustomExerciseToActiveWorkout(name: String, muscleGroup: String, targetSets: Int, targetReps: Int) {
        val currentExs = _activeExercises.value.toMutableList()
        val newEx = WorkoutExercise(
            sessionId = 0, // Assigned on save
            name = name,
            muscleGroup = muscleGroup,
            targetSets = targetSets,
            targetReps = targetReps
        )
        currentExs.add(newEx)
        _activeExercises.value = currentExs
    }

    fun updateActiveExerciseSet(exerciseIndex: Int, setIndex: Int, reps: Int, weight: Float) {
        val currentExs = _activeExercises.value.toMutableList()
        if (exerciseIndex in currentExs.indices) {
            val ex = currentExs[exerciseIndex]
            
            // Parse existing completed reps and weights
            val repList = if (ex.completedReps.isEmpty()) mutableListOf() else ex.completedReps.split(",").map { it.toInt() }.toMutableList()
            val weightList = if (ex.completedWeight.isEmpty()) mutableListOf() else ex.completedWeight.split(",").map { it.toFloat() }.toMutableList()
            
            // Pad lists up to setIndex
            while (repList.size <= setIndex) repList.add(0)
            while (weightList.size <= setIndex) weightList.add(0f)
            
            repList[setIndex] = reps
            weightList[setIndex] = weight
            
            val updatedCompletedSets = repList.count { it > 0 }
            val updatedCompletedReps = repList.joinToString(",")
            val updatedCompletedWeight = weightList.joinToString(",")
            val updatedIsCompleted = updatedCompletedSets >= ex.targetSets
            
            currentExs[exerciseIndex] = ex.copy(
                completedSets = updatedCompletedSets,
                completedReps = updatedCompletedReps,
                completedWeight = updatedCompletedWeight,
                isCompleted = updatedIsCompleted
            )
            _activeExercises.value = currentExs
        }
    }

    fun toggleExerciseCompletion(exerciseIndex: Int) {
        val currentExs = _activeExercises.value.toMutableList()
        if (exerciseIndex in currentExs.indices) {
            val ex = currentExs[exerciseIndex]
            val isCompleted = !ex.isCompleted
            val completedSets = if (isCompleted) ex.targetSets else 0
            
            // Generate full sets matching target reps
            val repList = List(ex.targetSets) { if (isCompleted) ex.targetReps else 0 }
            val weightList = List(ex.targetSets) { 0f }
            
            currentExs[exerciseIndex] = ex.copy(
                isCompleted = isCompleted,
                completedSets = completedSets,
                completedReps = repList.joinToString(","),
                completedWeight = weightList.joinToString(",")
            )
            _activeExercises.value = currentExs
        }
    }

    fun finishAndSaveWorkout(difficulty: String, notes: String) {
        val session = _activeSession.value ?: return
        viewModelScope.launch {
            stopWorkoutTimer()
            
            // Calculate calories burned: approx 6 calories per minute of active workout
            val minutes = _workoutTimerSeconds.value / 60.0
            val calculatedCalories = (minutes * 6.5).toInt()

            val sessionToSave = session.copy(
                durationSeconds = _workoutTimerSeconds.value,
                caloriesBurned = calculatedCalories,
                difficulty = difficulty,
                notes = notes
            )
            
            val sessionId = repository.insertSession(sessionToSave)
            
            // Link exercises to the newly generated sessionId and save them
            val exercisesToSave = _activeExercises.value.map {
                it.copy(sessionId = sessionId)
            }
            repository.insertExercises(exercisesToSave)
            
            // Clear active workout state
            _activeSession.value = null
            _activeExercises.value = emptyList()
            _workoutTimerSeconds.value = 0
            
            // Trigger achievement updates
            scanAndUnlockAchievements()
            updateGoalsProgressOnWorkoutFinished(exercisesToSave, calculatedCalories, minutes)

            // Switch to dashboard
            _currentTab.value = 0
        }
    }

    fun discardActiveWorkout() {
        stopWorkoutTimer()
        _activeSession.value = null
        _activeExercises.value = emptyList()
        _workoutTimerSeconds.value = 0
        _currentTab.value = 0
    }

    // --- Workout Timer Helpers ---
    private fun startWorkoutTimer() {
        _isTimerRunning.value = true
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _workoutTimerSeconds.value += 1
            }
        }
    }

    fun toggleWorkoutTimer() {
        if (_isTimerRunning.value) {
            workoutTimerJob?.cancel()
            _isTimerRunning.value = false
        } else {
            startWorkoutTimer()
        }
    }

    private fun stopWorkoutTimer() {
        workoutTimerJob?.cancel()
        _isTimerRunning.value = false
    }

    // --- Rest Timer Helpers ---
    fun startRestTimer(seconds: Int) {
        _restTimerSeconds.value = seconds
        _isRestTimerRunning.value = true
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (_restTimerSeconds.value > 0) {
                delay(1000)
                _restTimerSeconds.value -= 1
            }
            _isRestTimerRunning.value = false
        }
    }

    fun stopRestTimer() {
        restTimerJob?.cancel()
        _isRestTimerRunning.value = false
        _restTimerSeconds.value = 0
    }

    // --- Body Measurement Actions ---
    fun logBodyMeasurement(
        weight: Float,
        chest: Float,
        waist: Float,
        arms: Float,
        shoulders: Float,
        thigh: Float,
        bodyFat: Float,
        notes: String
    ) {
        viewModelScope.launch {
            val measurement = BodyMeasurement(
                dateMillis = System.currentTimeMillis(),
                weight = weight,
                chest = chest,
                waist = waist,
                arms = arms,
                shoulders = shoulders,
                thigh = thigh,
                bodyFat = bodyFat,
                notes = notes
            )
            repository.insertMeasurement(measurement)
        }
    }

    fun deleteBodyMeasurement(measurement: BodyMeasurement) {
        viewModelScope.launch {
            repository.deleteMeasurement(measurement)
        }
    }

    // --- Goal Actions ---
    fun addCustomGoal(title: String, targetValue: Float, unit: String, category: String) {
        viewModelScope.launch {
            val goal = Goal(
                title = title,
                targetValue = targetValue,
                currentValue = 0f,
                unit = unit,
                category = category,
                isCompleted = false
            )
            repository.insertGoal(goal)
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    // --- Backup & Restore & Reset (Settings Module) ---
    fun resetAllData() {
        viewModelScope.launch {
            // Delete all sessions and sub-exercises
            val sessions = repository.getAllSessionsList()
            for (s in sessions) {
                repository.deleteSession(s)
            }
            // Delete all goals
            val goals = repository.allGoals.first()
            for (g in goals) {
                repository.deleteGoal(g)
            }
            // Delete all measurements
            val measurements = repository.allMeasurements.first()
            for (m in measurements) {
                repository.deleteMeasurement(m)
            }
            
            // Re-populate clean default template data
            repository.prepopulateInitialDataIfNeeded()
        }
    }

    fun duplicateWorkoutSession(session: WorkoutSession) {
        viewModelScope.launch {
            val exercises = repository.getExercisesForSession(session.id)
            val newSessionId = repository.insertSession(
                session.copy(
                    id = 0,
                    dateMillis = System.currentTimeMillis()
                )
            )
            val newExercises = exercises.map {
                it.copy(id = 0, sessionId = newSessionId)
            }
            repository.insertExercises(newExercises)
            scanAndUnlockAchievements()
        }
    }

    fun deleteWorkoutSession(session: WorkoutSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            scanAndUnlockAchievements()
        }
    }

    // --- Search & Filtering Setters ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setMuscleFilter(filter: String) {
        _muscleFilter.value = filter
    }

    fun setSortOrder(order: String) {
        _sortOrder.value = order
    }

    // --- Goal Progress Updater ---
    private fun updateGoalsProgressOnWorkoutFinished(
        exercises: List<WorkoutExercise>,
        calories: Int,
        minutes: Double
    ) {
        viewModelScope.launch {
            val currentGoals = repository.allGoals.first().toMutableList()
            for (goal in currentGoals) {
                var progressToAdd = 0f
                when (goal.category) {
                    "Exercise" -> {
                        // Check if goal mentions a specific exercise in its title
                        if (goal.title.contains("Push-up", ignoreCase = true)) {
                            val pushupsCount = exercises.filter { it.name.contains("Push-up", ignoreCase = true) && it.isCompleted }
                                .sumOf { it.completedSets * it.targetReps }
                            progressToAdd = pushupsCount.toFloat()
                        } else if (goal.title.contains("Squat", ignoreCase = true)) {
                            val squatsCount = exercises.filter { it.name.contains("Squat", ignoreCase = true) && it.isCompleted }
                                .sumOf { it.completedSets * it.targetReps }
                            progressToAdd = squatsCount.toFloat()
                        } else if (goal.title.contains("Pull-up", ignoreCase = true)) {
                            val pullupsCount = exercises.filter { it.name.contains("Pull-up", ignoreCase = true) && it.isCompleted }
                                .sumOf { it.completedSets * it.targetReps }
                            progressToAdd = pullupsCount.toFloat()
                        }
                    }
                    "Frequency" -> {
                        progressToAdd = 1f // Add 1 workout session
                    }
                    "Duration" -> {
                        progressToAdd = (minutes / 60.0).toFloat() // Hours
                    }
                }

                if (progressToAdd > 0) {
                    val updatedVal = (goal.currentValue + progressToAdd).coerceAtMost(goal.targetValue)
                    repository.updateGoal(
                        goal.copy(
                            currentValue = updatedVal,
                            isCompleted = updatedVal >= goal.targetValue
                        )
                    )
                }
            }
        }
    }

    // --- Automated Achievement Scanner ---
    private suspend fun scanAndUnlockAchievements() {
        val sessions = repository.getAllSessionsList()
        val nonRestSessions = sessions.filter { !it.isRestDay }
        val exercises = repository.allExercises.first()

        val achievements = repository.getAllAchievementsList()

        // 1. Log total workouts thresholds
        val totalWorkouts = nonRestSessions.size
        checkAndUnlock(achievements, "WORKOUT_10", totalWorkouts.toFloat())
        checkAndUnlock(achievements, "WORKOUT_100", totalWorkouts.toFloat())

        // 2. Max workout volume or duration achievement
        val longestWorkoutDuration = nonRestSessions.maxOfOrNull { it.durationSeconds } ?: 0L
        if (longestWorkoutDuration >= 1800) { // 30 minutes
            // Unlock longest workout badge
            checkAndUnlock(achievements, "STREAK_5", 5f) // Unlock a placeholder
        }

        // 3. Streak calculation
        val currentStreak = calculateStreak(sessions)
        if (currentStreak >= 5) {
            checkAndUnlock(achievements, "STREAK_5", currentStreak.toFloat())
        }

        // 4. Specific exercise achievements
        val maxPushupsInASession = exercises.filter { it.name.contains("Push-up", ignoreCase = true) && it.isCompleted }
            .groupBy { it.sessionId }
            .map { entry -> entry.value.sumOf { it.completedSets * it.targetReps } }
            .maxOrNull() ?: 0
        checkAndUnlock(achievements, "PUSHUP_100", maxPushupsInASession.toFloat())

        val maxPullupsInASession = exercises.filter { it.name.contains("Pull-up", ignoreCase = true) && it.isCompleted }
            .groupBy { it.sessionId }
            .map { entry -> entry.value.sumOf { it.completedSets * it.targetReps } }
            .maxOrNull() ?: 0
        checkAndUnlock(achievements, "PULLUP_10", maxPullupsInASession.toFloat())

        val totalSquatsCompleted = exercises.filter { it.name.contains("Squat", ignoreCase = true) && it.isCompleted }
            .sumOf { it.completedSets * it.targetReps }
        checkAndUnlock(achievements, "SQUAT_500", totalSquatsCompleted.toFloat())
    }

    private suspend fun checkAndUnlock(achievements: List<Achievement>, type: String, value: Float) {
        val ach = achievements.find { it.type == type } ?: return
        if (ach.unlockedDateMillis == 0L && value >= ach.targetValue) {
            // Unlock it!
            repository.insertAchievement(
                ach.copy(
                    unlockedDateMillis = System.currentTimeMillis()
                )
            )
        }
    }

    // --- Streak Calculation ---
    fun calculateStreak(sessions: List<WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0
        
        // Sort sessions by date (oldest to newest)
        val sortedSessions = sessions.sortedBy { it.dateMillis }
        
        // Format dates as yyyy-MM-dd to ignore time
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val uniqueDays = sortedSessions.map { sdf.format(Date(it.dateMillis)) }.distinct()
        
        if (uniqueDays.isEmpty()) return 0

        val calendar = Calendar.getInstance()
        val todayStr = sdf.format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = sdf.format(calendar.time)

        // If neither today nor yesterday has a workout, current streak is 0 (or they are on a rest day, let's say)
        if (!uniqueDays.contains(todayStr) && !uniqueDays.contains(yesterdayStr)) {
            return 0
        }

        var streak = 0
        calendar.time = Date() // Reset to today
        
        while (true) {
            val dateStr = sdf.format(calendar.time)
            if (uniqueDays.contains(dateStr)) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                // If it's not in uniqueDays, let's check if the session logged was a Rest Day (Day 4)
                // Rest Days continue the streak!
                val sessionsOnThisDay = sortedSessions.filter { sdf.format(Date(it.dateMillis)) == dateStr }
                val hasRestDay = sessionsOnThisDay.any { it.isRestDay }
                
                if (hasRestDay) {
                    streak++
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }
        return streak
    }

    fun calculateLongestStreak(sessions: List<WorkoutSession>): Int {
        if (sessions.isEmpty()) return 0
        val sortedSessions = sessions.sortedBy { it.dateMillis }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val uniqueDays = sortedSessions.map { sdf.format(Date(it.dateMillis)) }.distinct()

        var maxStreak = 0
        var currentStreak = 0

        // Parse day-by-day to find the longest block
        val cal = Calendar.getInstance()
        if (sortedSessions.isNotEmpty()) {
            cal.time = Date(sortedSessions.first().dateMillis)
            val endCal = Calendar.getInstance()
            endCal.time = Date()

            while (cal.before(endCal) || sdf.format(cal.time) == sdf.format(endCal.time)) {
                val dateStr = sdf.format(cal.time)
                val daySessions = sortedSessions.filter { sdf.format(Date(it.dateMillis)) == dateStr }
                
                if (uniqueDays.contains(dateStr) || daySessions.any { it.isRestDay }) {
                    currentStreak++
                    if (currentStreak > maxStreak) {
                        maxStreak = currentStreak
                    }
                } else {
                    currentStreak = 0
                }
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        return maxStreak.coerceAtLeast(calculateStreak(sessions))
    }
}

class WorkoutViewModelFactory(
    private val application: Application,
    private val repository: WorkoutRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkoutViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkoutViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
