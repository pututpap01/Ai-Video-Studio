package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aistudio.aivideogenerator.ui.screens.PromptStudioScreen
import com.aistudio.aivideogenerator.ui.screens.SettingsScreen
import com.aistudio.aivideogenerator.ui.screens.StudioHomeScreen
import com.aistudio.aivideogenerator.ui.screens.VideoPlayerStudioScreen
import com.aistudio.aivideogenerator.ui.viewmodel.VideoStudioViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AiVideoStudioApp()
                }
            }
        }
    }
}

@Composable
fun AiVideoStudioApp() {
    val navController = rememberNavController()
    val viewModel: VideoStudioViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            StudioHomeScreen(
                viewModel = viewModel,
                onNavigateToCreate = { prompt ->
                    val encodedPrompt = java.net.URLEncoder.encode(prompt, "UTF-8")
                    navController.navigate("create?prompt=$encodedPrompt")
                },
                onNavigateToPlayer = { projectId ->
                    navController.navigate("player/$projectId")
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable(
            route = "create?prompt={prompt}",
            arguments = listOf(
                navArgument("prompt") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val initialPrompt = backStackEntry.arguments?.getString("prompt") ?: ""
            val decodedPrompt = try {
                java.net.URLDecoder.decode(initialPrompt, "UTF-8")
            } catch (e: Exception) {
                initialPrompt
            }

            PromptStudioScreen(
                viewModel = viewModel,
                initialPrompt = decodedPrompt,
                onNavigateBack = { navController.popBackStack() },
                onGenerationComplete = { newProjectId ->
                    navController.navigate("player/$newProjectId") {
                        popUpTo("home")
                    }
                }
            )
        }

        composable(
            route = "player/{projectId}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            VideoPlayerStudioScreen(
                viewModel = viewModel,
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
