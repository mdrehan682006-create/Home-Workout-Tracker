package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {

    // --- Workout Sessions ---
    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC")
    fun getAllSessionsFlow(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY dateMillis DESC")
    suspend fun getAllSessions(): List<WorkoutSession>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    suspend fun getSessionById(sessionId: Long): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun getSessionByIdFlow(sessionId: Long): Flow<WorkoutSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSession): Long

    @Update
    suspend fun updateSession(session: WorkoutSession)

    @Delete
    suspend fun deleteSession(session: WorkoutSession)

    @Transaction
    @Query("DELETE FROM workout_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    // --- Workout Exercises ---
    @Query("SELECT * FROM workout_exercises WHERE sessionId = :sessionId")
    fun getExercisesForSessionFlow(sessionId: Long): Flow<List<WorkoutExercise>>

    @Query("SELECT * FROM workout_exercises WHERE sessionId = :sessionId")
    suspend fun getExercisesForSession(sessionId: Long): List<WorkoutExercise>

    @Query("SELECT * FROM workout_exercises WHERE name = :name ORDER BY id ASC")
    fun getExercisesByNameFlow(name: String): Flow<List<WorkoutExercise>>

    @Query("SELECT * FROM workout_exercises WHERE name = :name ORDER BY id ASC")
    suspend fun getExercisesByName(name: String): List<WorkoutExercise>

    @Query("SELECT * FROM workout_exercises ORDER BY id DESC")
    fun getAllExercisesFlow(): Flow<List<WorkoutExercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: WorkoutExercise): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<WorkoutExercise>)

    @Update
    suspend fun updateExercise(exercise: WorkoutExercise)

    @Delete
    suspend fun deleteExercise(exercise: WorkoutExercise)

    @Query("DELETE FROM workout_exercises WHERE sessionId = :sessionId")
    suspend fun deleteExercisesForSession(sessionId: Long)

    // --- Goals ---
    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun getAllGoalsFlow(): Flow<List<Goal>>

    @Query("SELECT * FROM goals ORDER BY id DESC")
    suspend fun getAllGoals(): List<Goal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    // --- Body Measurements ---
    @Query("SELECT * FROM body_measurements ORDER BY dateMillis DESC")
    fun getAllMeasurementsFlow(): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurements ORDER BY dateMillis DESC")
    suspend fun getAllMeasurements(): List<BodyMeasurement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeasurement(measurement: BodyMeasurement): Long

    @Update
    suspend fun updateMeasurement(measurement: BodyMeasurement)

    @Delete
    suspend fun deleteMeasurement(measurement: BodyMeasurement)

    // --- Achievements ---
    @Query("SELECT * FROM achievements ORDER BY unlockedDateMillis DESC")
    fun getAllAchievementsFlow(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements ORDER BY unlockedDateMillis DESC")
    suspend fun getAllAchievements(): List<Achievement>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: Achievement): Long
}
