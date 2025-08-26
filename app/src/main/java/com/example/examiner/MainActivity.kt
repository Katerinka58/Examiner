package com.example.examiner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.examiner.ui.screens.QuestionsScreen
import com.example.examiner.ui.screens.AnalyticsScreen
import com.example.examiner.ui.screens.ExamScreen
import com.example.examiner.ui.screens.IntegrationsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ExaminerApp() }
    }
}

private enum class ExaminerDest(val route: String, val label: String) {
    QUESTIONS("questions", "Вопросы"),
    ANALYTICS("analytics", "Аналитика"),
    EXAM("exam", "Экзамен"),
    INTEGRATIONS("integrations", "Интеграции")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExaminerApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    ExaminerDest.values().forEach { dest ->
                        val selected = currentRoute == dest.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            label = { Text(dest.label) },
                            icon = { Text(dest.label.take(1)) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = ExaminerDest.QUESTIONS.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(ExaminerDest.QUESTIONS.route) { QuestionsScreen() }
                composable(ExaminerDest.ANALYTICS.route) { AnalyticsScreen() }
                composable(ExaminerDest.EXAM.route) { ExamScreen() }
                composable(ExaminerDest.INTEGRATIONS.route) { IntegrationsScreen() }
            }
        }
    }
} 