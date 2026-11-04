
package com.example.tomatocare.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tomatocare.TomatoViewModel
import com.example.tomatocare.ml.AnalysisOutcome
import com.example.tomatocare.ui.screens.AboutScreen
import com.example.tomatocare.ui.screens.AnalysisScreen
import com.example.tomatocare.ui.screens.CameraScreen
import com.example.tomatocare.ui.screens.DiseaseDetailsScreen
import com.example.tomatocare.ui.screens.HistoryScreen
import com.example.tomatocare.ui.screens.HomeScreen
import com.example.tomatocare.ui.screens.NoLeafResultScreen
import com.example.tomatocare.ui.screens.ResultScreen
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.collectAsState
import com.example.tomatocare.data.AnalysisEntity

private val TomatoRed = Color(0xFFB3261D)
private val TextSecondary = Color(0xFF6B716B)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Общий ViewModel для экранов камеры, анализа, результата и истории.
    val viewModel: TomatoViewModel = viewModel()

    val bottomItems = listOf(
        BottomNavItem(
            route = "home",
            label = "Главная",
            icon = Icons.Default.Home
        ),
        BottomNavItem(
            route = "history",
            label = "История",
            icon = Icons.Default.List
        ),
        BottomNavItem(
            route = "about",
            label = "О приложении",
            icon = Icons.Default.Info
        )
    )

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        "home",
        "history",
        "about"
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 0.dp
                ) {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo("home") {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(item.label)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TomatoRed,
                                selectedTextColor = TomatoRed,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Color(0xFFFFE5E5)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                val analyses by viewModel.analyses.collectAsState(initial = emptyList())
                val latestAnalysis = analyses.firstOrNull()

                HomeScreen(
                    latestAnalysis = latestAnalysis,
                    onCameraClick = {
                        navController.navigate("camera")
                    },
                    onImagePicked = { uri ->
                        viewModel.imageUri = uri
                        navController.navigate("analysis")
                    },
                    onLastAnalysisClick = { id ->
                        navController.navigate("result/$id")
                    }
                )
            }

            composable("camera") {
                CameraScreen(
                    onBack = {
                        navController.popBackStack()
                    },
                    onPhotoCaptured = { uri ->
                        viewModel.imageUri = uri
                        navController.navigate("analysis")
                    },
                    onImagePicked = { uri ->
                        viewModel.imageUri = uri
                        navController.navigate("analysis")
                    }
                )
            }

            composable("analysis") {
                val uri = viewModel.imageUri

                if (uri != null) {
                    AnalysisScreen(
                        imageUri = uri,
                        pipelineProvider = { viewModel.pipeline },
                        onResult = { outcome ->
                            when (outcome) {
                                is AnalysisOutcome.Leaf -> {
                                    viewModel.saveAnalysis(
                                        uri = uri,
                                        outcome = outcome,
                                        onSaved = { id ->
                                            navController.navigate("result/$id") {
                                                popUpTo("analysis") {
                                                    inclusive = true
                                                }
                                            }
                                        },
                                        onError = {
                                            Toast.makeText(
                                                context,
                                                "Не удалось сохранить результат анализа",
                                                Toast.LENGTH_LONG
                                            ).show()

                                            navController.popBackStack()
                                        }
                                    )
                                }

                                AnalysisOutcome.NoLeaf -> {
                                    viewModel.result = outcome

                                    navController.navigate("result_no_leaf") {
                                        popUpTo("analysis") {
                                            inclusive = true
                                        }
                                    }
                                }
                            }
                        },
                        onError = {
                            Toast.makeText(
                                context,
                                "Не удалось проанализировать фото. Попробуйте ещё раз",
                                Toast.LENGTH_LONG
                            ).show()

                            navController.popBackStack()
                        },
                        onCancel = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }

            // Результат сохранённого анализа.
            composable(
                route = "result/{analysisId}",
                arguments = listOf(
                    navArgument("analysisId") {
                        type = NavType.LongType
                    }
                )
            ) { backStackEntry ->

                val analysisId = backStackEntry.arguments
                    ?.getLong("analysisId")

                if (analysisId != null) {
                    ResultScreen(
                        analysisId = analysisId,
                        viewModel = viewModel,
                        onDetailsClick = {
                            navController.navigate("disease_details")
                        },
                        onNewPhoto = {
                            navController.navigate("camera") {
                                popUpTo("home")
                            }
                        },
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }

            // Результат, когда YOLO не обнаружила лист.
            // В Room такой результат не сохраняется.
            composable("result_no_leaf") {
                NoLeafResultScreen(
                    imageUri = viewModel.imageUri,
                    onNewPhoto = {
                        navController.navigate("camera") {
                            popUpTo("home")
                        }
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable("disease_details") {
                DiseaseDetailsScreen()
            }

            composable("history") {
                HistoryScreen(
                    viewModel = viewModel,
                    onAnalysisClick = { id ->
                        navController.navigate("result/$id")
                    }
                )
            }

            composable("about") {
                AboutScreen()
            }
        }
    }
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
