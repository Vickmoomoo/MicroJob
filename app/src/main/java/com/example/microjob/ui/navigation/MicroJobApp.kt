package com.example.microjob.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.microjob.ui.screens.PlaceholderScreen
import com.example.microjob.ui.screens.home.HomeScreen

/** Root composable: Scaffold (bottom bar + center FAB) hosting the NavHost. */
@Composable
fun MicroJobApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MicroJobBottomBar(
                currentRoute = currentRoute,
                onNavigate = navController::navigateToTab
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(MicroJobRoutes.POST_JOB) }) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Post Job")
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MicroJobRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MicroJobRoutes.HOME) {
                HomeScreen(
                    onJobClick = { /* TODO: navigate to Job Details */ }
                )
            }
            composable(MicroJobRoutes.COURSE) {
                PlaceholderScreen("Course & Certification")
            }
            composable(MicroJobRoutes.MESSAGES) {
                PlaceholderScreen("Messages")
            }
            composable(MicroJobRoutes.PROFILE) {
                PlaceholderScreen("My Profile")
            }
            composable(MicroJobRoutes.POST_JOB) {
                PlaceholderScreen("Post Job")
            }
        }
    }
}

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val bottomTabs = listOf(
    BottomTab(MicroJobRoutes.HOME, "Home", Icons.Filled.Home),
    BottomTab(MicroJobRoutes.COURSE, "Course", Icons.Filled.Star),
    BottomTab(MicroJobRoutes.MESSAGES, "Messages", Icons.Filled.Email),
    BottomTab(MicroJobRoutes.PROFILE, "Profile", Icons.Filled.Person),
)

@Composable
private fun MicroJobBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationBar {
        bottomTabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) {
        popUpTo(MicroJobRoutes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
