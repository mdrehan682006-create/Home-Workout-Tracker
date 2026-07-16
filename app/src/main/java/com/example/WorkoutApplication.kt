package com.example

import android.app.Application
import com.example.data.database.WorkoutDatabase
import com.example.data.repository.WorkoutRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WorkoutApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { WorkoutDatabase.getDatabase(this) }
    val repository by lazy { WorkoutRepository(database.workoutDao()) }

    override fun onCreate() {
        super.onCreate()
        // Prepopulate database with initial 4-day custom workouts and goals in background
        applicationScope.launch {
            repository.prepopulateInitialDataIfNeeded()
        }
    }
}
