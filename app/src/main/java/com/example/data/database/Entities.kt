package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val dayNumber: Int, // 1, 2, 3, or 4
    val durationSeconds: Long,
    val caloriesBurned: Int,
    val difficulty: String, // "Easy", "Medium", "Hard"
    val notes: String,
    val isRestDay: Boolean = false
)

@Entity(tableName = "workout_exercises")
data class WorkoutExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long, // Foreign key linking to WorkoutSession.id
    val name: String,
    val muscleGroup: String,
    val targetSets: Int = 3,
    val targetReps: Int = 10,
    val completedSets: Int = 0,
    val completedReps: String = "", // Comma-separated reps, e.g., "10,10,10"
    val completedWeight: String = "", // Comma-separated weights, e.g., "0,0,0" (bodyweight)
    val difficulty: String = "Medium",
    val notes: String = "",
    val isCompleted: Boolean = false,
    val isPersonalRecord: Boolean = false
)

@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String, // e.g. "100 Push-ups", "500 Squats", "Workout 5 Days Weekly"
    val targetValue: Float,
    val currentValue: Float,
    val unit: String, // "reps", "sessions", "hours"
    val category: String, // "Exercise", "Frequency", "Duration"
    val isCompleted: Boolean = false
)

@Entity(tableName = "body_measurements")
data class BodyMeasurement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateMillis: Long,
    val weight: Float, // kg
    val chest: Float = 0f, // cm
    val waist: Float = 0f, // cm
    val arms: Float = 0f, // cm
    val shoulders: Float = 0f, // cm
    val thigh: Float = 0f, // cm
    val bodyFat: Float = 0f, // %
    val notes: String = ""
)

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // e.g. "STREAK_5", "WORKOUT_10", "PUSHUP_100"
    val title: String,
    val description: String,
    val iconName: String,
    val unlockedDateMillis: Long,
    val targetValue: Float = 0f
)
