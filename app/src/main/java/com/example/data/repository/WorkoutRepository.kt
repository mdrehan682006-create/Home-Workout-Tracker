package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class WorkoutRepository(private val workoutDao: WorkoutDao) {

    val allSessions: Flow<List<WorkoutSession>> = workoutDao.getAllSessionsFlow()
    val allGoals: Flow<List<Goal>> = workoutDao.getAllGoalsFlow()
    val allMeasurements: Flow<List<BodyMeasurement>> = workoutDao.getAllMeasurementsFlow()
    val allAchievements: Flow<List<Achievement>> = workoutDao.getAllAchievementsFlow()
    val allExercises: Flow<List<WorkoutExercise>> = workoutDao.getAllExercisesFlow()

    // --- Workout Sessions & Exercises ---
    suspend fun getAllSessionsList() = workoutDao.getAllSessions()
    
    fun getSessionByIdFlow(sessionId: Long): Flow<WorkoutSession?> = workoutDao.getSessionByIdFlow(sessionId)
    
    suspend fun getSessionById(sessionId: Long): WorkoutSession? = workoutDao.getSessionById(sessionId)

    fun getExercisesForSessionFlow(sessionId: Long): Flow<List<WorkoutExercise>> = 
        workoutDao.getExercisesForSessionFlow(sessionId)

    suspend fun getExercisesForSession(sessionId: Long): List<WorkoutExercise> = 
        workoutDao.getExercisesForSession(sessionId)

    fun getExercisesByNameFlow(name: String): Flow<List<WorkoutExercise>> = 
        workoutDao.getExercisesByNameFlow(name)

    suspend fun insertSession(session: WorkoutSession): Long = workoutDao.insertSession(session)

    suspend fun updateSession(session: WorkoutSession) = workoutDao.updateSession(session)

    suspend fun deleteSession(session: WorkoutSession) {
        workoutDao.deleteExercisesForSession(session.id)
        workoutDao.deleteSession(session)
    }

    suspend fun insertExercise(exercise: WorkoutExercise): Long = workoutDao.insertExercise(exercise)

    suspend fun insertExercises(exercises: List<WorkoutExercise>) = workoutDao.insertExercises(exercises)

    suspend fun updateExercise(exercise: WorkoutExercise) = workoutDao.updateExercise(exercise)

    suspend fun deleteExercise(exercise: WorkoutExercise) = workoutDao.deleteExercise(exercise)

    // --- Goals ---
    suspend fun insertGoal(goal: Goal): Long = workoutDao.insertGoal(goal)
    suspend fun updateGoal(goal: Goal) = workoutDao.updateGoal(goal)
    suspend fun deleteGoal(goal: Goal) = workoutDao.deleteGoal(goal)

    // --- Measurements ---
    suspend fun insertMeasurement(measurement: BodyMeasurement): Long = workoutDao.insertMeasurement(measurement)
    suspend fun updateMeasurement(measurement: BodyMeasurement) = workoutDao.updateMeasurement(measurement)
    suspend fun deleteMeasurement(measurement: BodyMeasurement) = workoutDao.deleteMeasurement(measurement)

    // --- Achievements ---
    suspend fun insertAchievement(achievement: Achievement): Long = workoutDao.insertAchievement(achievement)
    suspend fun getAllAchievementsList() = workoutDao.getAllAchievements()

    // --- Prepopulate Initial Data Helper ---
    suspend fun prepopulateInitialDataIfNeeded() {
        val currentGoals = workoutDao.getAllGoals()
        if (currentGoals.isEmpty()) {
            // Prepopulate some default goals matching the user request
            workoutDao.insertGoal(Goal(title = "Complete 100 Push-ups in a Session", targetValue = 100f, currentValue = 0f, unit = "reps", category = "Exercise"))
            workoutDao.insertGoal(Goal(title = "Perform 10 consecutive Pull-ups", targetValue = 10f, currentValue = 0f, unit = "reps", category = "Exercise"))
            workoutDao.insertGoal(Goal(title = "Complete 500 Squats overall", targetValue = 500f, currentValue = 0f, unit = "reps", category = "Exercise"))
            workoutDao.insertGoal(Goal(title = "Workout 4 Days this Week", targetValue = 4f, currentValue = 0f, unit = "sessions", category = "Frequency"))
            workoutDao.insertGoal(Goal(title = "Workout 10 Hours this Month", targetValue = 10f, currentValue = 0f, unit = "hours", category = "Duration"))

            // Prepopulate default achievements (locked/ready to be unlocked)
            val now = System.currentTimeMillis()
            workoutDao.insertAchievement(Achievement(type = "STREAK_5", title = "5-Day Streak", description = "Workout consistently for 5 days in a row", iconName = "local_fire_department", unlockedDateMillis = 0, targetValue = 5f))
            workoutDao.insertAchievement(Achievement(type = "WORKOUT_10", title = "Consistent Athlete", description = "Log a total of 10 workouts", iconName = "fitness_center", unlockedDateMillis = 0, targetValue = 10f))
            workoutDao.insertAchievement(Achievement(type = "WORKOUT_100", title = "Elite Warrior", description = "Log a total of 100 workouts", iconName = "military_tech", unlockedDateMillis = 0, targetValue = 100f))
            workoutDao.insertAchievement(Achievement(type = "PUSHUP_100", title = "Push-up King", description = "Do 100 push-ups in a single session", iconName = "star", unlockedDateMillis = 0, targetValue = 100f))
            workoutDao.insertAchievement(Achievement(type = "SQUAT_500", title = "Leg Day Legend", description = "Complete 500 total squats", iconName = "workspace_premium", unlockedDateMillis = 0, targetValue = 500f))
            workoutDao.insertAchievement(Achievement(type = "PULLUP_10", title = "Bar Master", description = "Do 10 pull-ups in a single workout", iconName = "emoji_events", unlockedDateMillis = 0, targetValue = 10f))
        }

        // Add some mock past data if we want the charts and calendars to look premium right away!
        val existingSessions = workoutDao.getAllSessions()
        if (existingSessions.isEmpty()) {
            val now = System.currentTimeMillis()
            val dayInMillis = 86400000L

            // Let's seed 5 completed sessions over the last 10 days for immediate visual aesthetic in history, analytics & calendar
            val mockData = listOf(
                // 8 days ago: Day 1
                Triple(now - 8 * dayInMillis, 1, false),
                // 6 days ago: Day 2
                Triple(now - 6 * dayInMillis, 2, false),
                // 4 days ago: Day 3
                Triple(now - 4 * dayInMillis, 3, false),
                // 2 days ago: Day 4 (Rest)
                Triple(now - 2 * dayInMillis, 4, true),
                // Yesterday: Day 1
                Triple(now - 1 * dayInMillis, 1, false)
            )

            for ((time, dayNum, isRest) in mockData) {
                val sessionId = workoutDao.insertSession(
                    WorkoutSession(
                        dateMillis = time,
                        dayNumber = dayNum,
                        durationSeconds = if (isRest) 0 else (1200 + (Math.random() * 600).toLong()), // 20-30 mins
                        caloriesBurned = if (isRest) 0 else (150 + (Math.random() * 100).toInt()),
                        difficulty = if (isRest) "Easy" else listOf("Easy", "Medium", "Hard").random(),
                        notes = if (isRest) "Relaxed stretching, feeling great." else "Solid workout, feeling stronger!",
                        isRestDay = isRest
                    )
                )

                if (!isRest) {
                    val defaultExercises = getDefaultExercisesForDay(dayNum, sessionId)
                    // Mark some exercises complete and set sets/reps
                    val completedExercises = defaultExercises.map { exe ->
                        exe.copy(
                            completedSets = 3,
                            completedReps = "${exe.targetReps},${exe.targetReps},${exe.targetReps}",
                            completedWeight = "0,0,0",
                            isCompleted = true
                        )
                    }
                    workoutDao.insertExercises(completedExercises)
                }
            }

            // Also seed initial body measurements
            workoutDao.insertMeasurement(
                BodyMeasurement(
                    dateMillis = now - 15 * dayInMillis,
                    weight = 80.5f,
                    chest = 101.5f,
                    waist = 88.0f,
                    arms = 36.5f,
                    shoulders = 118.0f,
                    thigh = 59.0f,
                    bodyFat = 18.5f,
                    notes = "Starting tracking today."
                )
            )
            workoutDao.insertMeasurement(
                BodyMeasurement(
                    dateMillis = now,
                    weight = 79.2f,
                    chest = 102.0f,
                    waist = 86.5f,
                    arms = 37.0f,
                    shoulders = 119.5f,
                    thigh = 58.5f,
                    bodyFat = 17.8f,
                    notes = "Seeing nice core definition and strength gains!"
                )
            )
        }
    }

    // --- Helper to get default exercises per day ---
    fun getDefaultExercisesForDay(dayNumber: Int, sessionId: Long = 0): List<WorkoutExercise> {
        return when (dayNumber) {
            1 -> listOf(
                WorkoutExercise(sessionId = sessionId, name = "Push-ups", muscleGroup = "Chest", targetSets = 3, targetReps = 15),
                WorkoutExercise(sessionId = sessionId, name = "Diamond Push-ups", muscleGroup = "Triceps", targetSets = 3, targetReps = 10),
                WorkoutExercise(sessionId = sessionId, name = "Incline Push-ups", muscleGroup = "Chest", targetSets = 3, targetReps = 12),
                WorkoutExercise(sessionId = sessionId, name = "Chair Dips", muscleGroup = "Triceps", targetSets = 3, targetReps = 12),
                WorkoutExercise(sessionId = sessionId, name = "Bench Dips", muscleGroup = "Triceps", targetSets = 3, targetReps = 15),
                WorkoutExercise(sessionId = sessionId, name = "Bodyweight Squats", muscleGroup = "Legs", targetSets = 3, targetReps = 20),
                WorkoutExercise(sessionId = sessionId, name = "Close Grip Push-ups", muscleGroup = "Triceps", targetSets = 3, targetReps = 10)
            )
            2 -> listOf(
                WorkoutExercise(sessionId = sessionId, name = "Pull-ups", muscleGroup = "Back", targetSets = 3, targetReps = 8),
                WorkoutExercise(sessionId = sessionId, name = "Chin-ups", muscleGroup = "Biceps", targetSets = 3, targetReps = 8),
                WorkoutExercise(sessionId = sessionId, name = "Australian Rows", muscleGroup = "Back", targetSets = 3, targetReps = 10),
                WorkoutExercise(sessionId = sessionId, name = "Resistance Band Rows", muscleGroup = "Back", targetSets = 3, targetReps = 12),
                WorkoutExercise(sessionId = sessionId, name = "Superman Hold", muscleGroup = "Back", targetSets = 3, targetReps = 30), // 30s
                WorkoutExercise(sessionId = sessionId, name = "Biceps Curl", muscleGroup = "Biceps", targetSets = 3, targetReps = 12),
                WorkoutExercise(sessionId = sessionId, name = "Crunches", muscleGroup = "Core", targetSets = 3, targetReps = 20),
                WorkoutExercise(sessionId = sessionId, name = "Leg Raises", muscleGroup = "Core", targetSets = 3, targetReps = 12),
                WorkoutExercise(sessionId = sessionId, name = "Plank", muscleGroup = "Core", targetSets = 3, targetReps = 60), // 60s
                WorkoutExercise(sessionId = sessionId, name = "Side Plank", muscleGroup = "Core", targetSets = 2, targetReps = 30), // 30s per side
                WorkoutExercise(sessionId = sessionId, name = "Mountain Climbers", muscleGroup = "Core", targetSets = 3, targetReps = 20),
                WorkoutExercise(sessionId = sessionId, name = "Russian Twist", muscleGroup = "Core", targetSets = 3, targetReps = 20)
            )
            3 -> listOf(
                WorkoutExercise(sessionId = sessionId, name = "Pike Push-ups", muscleGroup = "Shoulders", targetSets = 3, targetReps = 10),
                WorkoutExercise(sessionId = sessionId, name = "Handstand Practice", muscleGroup = "Shoulders", targetSets = 3, targetReps = 45), // 45s
                WorkoutExercise(sessionId = sessionId, name = "Shoulder Taps", muscleGroup = "Shoulders", targetSets = 3, targetReps = 20),
                WorkoutExercise(sessionId = sessionId, name = "Front Raises", muscleGroup = "Shoulders", targetSets = 3, targetReps = 12),
                WorkoutExercise(sessionId = sessionId, name = "Lateral Raises", muscleGroup = "Shoulders", targetSets = 3, targetReps = 12),
                WorkoutExercise(sessionId = sessionId, name = "Wall Sit", muscleGroup = "Legs", targetSets = 3, targetReps = 45), // 45s
                WorkoutExercise(sessionId = sessionId, name = "Bodyweight Squats", muscleGroup = "Legs", targetSets = 3, targetReps = 20),
                WorkoutExercise(sessionId = sessionId, name = "Jump Squats", muscleGroup = "Legs", targetSets = 3, targetReps = 15)
            )
            else -> emptyList() // Day 4 (Rest) has no default exercises
        }
    }
}
