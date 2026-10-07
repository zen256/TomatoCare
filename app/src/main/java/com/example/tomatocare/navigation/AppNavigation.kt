package com.example.tomatocare.navigation

import android.widget.Toast
import androidx.compose.foundation.layout.height
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

import com.example.tomatocare.TomatoViewModel
import com.example.tomatocare.ui.screens.AboutScreen
import com.example.tomatocare.ui.screens.AnalysisScreen
import com.example.tomatocare.ui.screens.CameraScreen
import com.example.tomatocare.ui.screens.DiseaseDetailsScreen
import com.example.tomatocare.ui.screens.HistoryScreen
import com.example.tomatocare.ui.screens.HomeScreen
import com.example.tomatocare.ui.screens.ResultScreen

private val TomatoRed = Color(0xFFB3261D)
private val TextSecondary = Color(0xFF6B716B)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Общий ViewModel: хранит снимок, результат и нейросеть для экранов camera/analysis/result.
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
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.height(80.dp),
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

        // innerPadding применяется один раз здесь, поэтому экраны его повторно не получают.
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    onCameraClick = {
                        navController.navigate("camera")
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
                        classifierProvider = { viewModel.classifier },
                        onResult = { result ->
                            viewModel.result = result
                            navController.navigate("result") {
                                popUpTo("analysis") { inclusive = true }
                            }
                        },
                        onError = {
                            // Подробности ошибки пишутся в Logcat (тег AnalysisScreen).
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
                    // Снимка нет (например, процесс был перезапущен) — возвращаемся назад.
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }

            composable("result") {
                val result = viewModel.result

                if (result != null) {
                    ResultScreen(
                        result = result,
                        imageUri = viewModel.imageUri,
                        onDetailsClick = {
                            navController.navigate("disease_details")
                        },
                        onNewPhoto = {
                            // Сразу на камеру; экран результата убираем из стека.
                            navController.navigate("camera") {
                                popUpTo("home")
                            }
                        },
                        onBack = {
                            navController.popBackStack("home", inclusive = false)
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }

            composable("disease_details") {
                DiseaseDetailsScreen()
            }

            composable("history") {
                HistoryScreen()
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