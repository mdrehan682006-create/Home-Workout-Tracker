package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.*
import com.example.viewmodel.WorkoutViewModel
import com.example.viewmodel.WorkoutViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: WorkoutViewModel by viewModels {
        WorkoutViewModelFactory(
            application,
            (application as WorkoutApplication).repository
        )
    }

    @OptIn(ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                val currentTab by viewModel.currentTab.collectAsState()
                val activeSession by viewModel.activeSession.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                            tonalElevation = 8.dp
                        ) {
                            // Tab 0: Dashboard
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { viewModel.setTab(0) },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )

                            // Tab 1: Active Workout
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { viewModel.setTab(1) },
                                icon = {
                                    Box {
                                        Icon(Icons.Default.FitnessCenter, contentDescription = "Workout")
                                        if (activeSession != null) {
                                            // Pulser active light indicator
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .align(Alignment.TopEnd)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )
                                        }
                                    }
                                },
                                label = { Text("Workout", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )

                            // Tab 2: Calendar & History
                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { viewModel.setTab(2) },
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar") },
                                label = { Text("Calendar", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )

                            // Tab 3: Analytics
                            NavigationBarItem(
                                selected = currentTab == 3,
                                onClick = { viewModel.setTab(3) },
                                icon = { Icon(Icons.Default.ShowChart, contentDescription = "Analytics") },
                                label = { Text("Charts", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )

                            // Tab 4: Goals & Badges
                            NavigationBarItem(
                                selected = currentTab == 4,
                                onClick = { viewModel.setTab(4) },
                                icon = { Icon(Icons.Default.MilitaryTech, contentDescription = "Progress") },
                                label = { Text("Progress", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )

                            // Tab 5: Settings
                            NavigationBarItem(
                                selected = currentTab == 5,
                                onClick = { viewModel.setTab(5) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        // Smooth transition content loading
                        when (currentTab) {
                            0 -> DashboardScreen(
                                viewModel = viewModel,
                                onNavigateToTab = { index -> viewModel.setTab(index) }
                            )
                            1 -> ActiveWorkoutScreen(
                                viewModel = viewModel,
                                onNavigateToTab = { index -> viewModel.setTab(index) }
                            )
                            2 -> CalendarScreen(
                                viewModel = viewModel,
                                onNavigateToTab = { index -> viewModel.setTab(index) }
                            )
                            3 -> AnalyticsScreen(
                                viewModel = viewModel,
                                onNavigateToTab = { index -> viewModel.setTab(index) }
                            )
                            4 -> ProgressScreen(
                                viewModel = viewModel,
                                onNavigateToTab = { index -> viewModel.setTab(index) }
                            )
                            5 -> SettingsScreen(
                                viewModel = viewModel,
                                onNavigateToTab = { index -> viewModel.setTab(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}
