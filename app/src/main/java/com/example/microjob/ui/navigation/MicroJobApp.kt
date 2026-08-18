package com.example.microjob.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.microjob.ui.screens.PlaceholderScreen
import com.example.microjob.ui.screens.detail.JobDetailScreen
import com.example.microjob.ui.screens.home.HomeScreen
import com.example.microjob.ui.screens.login.LoginScreen
import com.example.microjob.ui.screens.messages.ChatDetailScreen
import com.example.microjob.ui.screens.messages.ChatListScreen
import com.example.microjob.ui.screens.post.PostJobScreen
import com.example.microjob.ui.screens.profile.ProfileScreen
import com.example.microjob.ui.screens.translation.VoiceTranslationScreen
import com.example.microjob.viewmodel.AuthViewModel
import com.example.microjob.viewmodel.ChatViewModel
import com.example.microjob.viewmodel.HomeViewModel
import com.example.microjob.viewmodel.PostJobViewModel
import com.example.microjob.viewmodel.postJobViewModelFactory
import kotlinx.coroutines.launch

/** Root composable: Scaffold (bottom bar + center FAB) hosting the NavHost. */
@Composable
fun MicroJobApp() {
    val navController = rememberNavController()
    val authVm: AuthViewModel = viewModel()
    val postJobVm: PostJobViewModel = viewModel(factory = postJobViewModelFactory())
    val chatVm: ChatViewModel = viewModel()
    val homeVm: HomeViewModel = viewModel()
    val currentUser by authVm.currentUser.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Restore the logged-in user from SharedPreferences on startup.
    LaunchedEffect(Unit) {
        authVm.loadCurrentUser()
    }

    // Show a welcome snackbar right after login, then go back to Home.
    val isLoginScreen = currentRoute == MicroJobRoutes.LOGIN
    LaunchedEffect(currentUser) {
        if (currentUser != null && isLoginScreen) {
            // Navigate to Home FIRST, then show the welcome snackbar on it.
            navController.popBackStack(MicroJobRoutes.HOME, inclusive = false)
            snackbarHostState.showSnackbar("Welcome, ${currentUser!!.name}!")
        }
    }

    // Refresh Home whenever we arrive back on it, so jobs that were accepted
    // (OPEN → IN_PROGRESS) stop showing on the feed.
    LaunchedEffect(currentRoute) {
        if (currentRoute == MicroJobRoutes.HOME) {
            homeVm.loadJobs()
        }
    }

    // Full-screen pages (job detail, post job, login) hide the bottom bar, the
    // FAB and the outer Scaffold padding — they have their own TopAppBar and
    // must not inherit the status-bar inset a second time.
    val isFullScreen =
        currentRoute == MicroJobRoutes.JOB_DETAIL ||
            currentRoute == MicroJobRoutes.POST_JOB ||
            currentRoute == MicroJobRoutes.VOICE_TRANSLATION ||
            currentRoute == MicroJobRoutes.LOGIN ||
            currentRoute == MicroJobRoutes.CHAT_DETAIL
    val showChrome = !isFullScreen

    /** Navigates to [route]; if the user is not logged in, goes to login first. */
    fun requireLogin(route: String) {
        if (currentUser != null) {
            navController.navigateToTab(route)
        } else {
            navController.navigate(MicroJobRoutes.LOGIN)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
            if (showChrome) {
                MicroJobBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        // Profile and Messages need a login; other tabs navigate normally.
                        if (route == MicroJobRoutes.PROFILE || route == MicroJobRoutes.MESSAGES) {
                            requireLogin(route)
                        } else {
                            navController.navigateToTab(route)
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (showChrome && currentRoute == MicroJobRoutes.HOME) {
                FloatingActionButton(onClick = { requireLogin(MicroJobRoutes.POST_JOB) }) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Post Job")
                }
            } else if (showChrome && currentRoute == MicroJobRoutes.MESSAGES) {
                FloatingActionButton(
                    onClick = { navController.navigate(MicroJobRoutes.VOICE_TRANSLATION) }
                ) {
                    Icon(imageVector = Icons.Filled.Translate, contentDescription = "Voice translation")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MicroJobRoutes.HOME,
            // Full-screen pages must not inherit the outer Scaffold's padding.
            modifier = if (showChrome) Modifier.padding(innerPadding) else Modifier
        ) {
            composable(MicroJobRoutes.HOME) {
                HomeScreen(
                    vm = homeVm,
                    onJobClick = { job -> navController.navigate(MicroJobRoutes.jobDetail(job.id)) }
                )
            }
            composable(MicroJobRoutes.COURSE) {
                PlaceholderScreen("Course & Certification")
            }
            composable(MicroJobRoutes.MESSAGES) {
                ChatListScreen(
                    vm = chatVm,
                    onChatClick = { otherUserId ->
                        navController.navigate(MicroJobRoutes.chatDetail(otherUserId))
                    }
                )
            }
            composable(
                route = MicroJobRoutes.CHAT_DETAIL,
                arguments = listOf(navArgument("otherUserId") { type = NavType.LongType })
            ) { entry ->
                val otherUserId = entry.arguments?.getLong("otherUserId") ?: return@composable
                ChatDetailScreen(
                    otherUserId = otherUserId,
                    onBack = { navController.popBackStack() },
                    vm = chatVm
                )
            }
            composable(MicroJobRoutes.PROFILE) {
                ProfileScreen(
                    vm = authVm,
                    onLoggedOut = {
                        authVm.logout()
                        // Go back to Home so the logged-out state is obvious.
                        navController.popBackStack(MicroJobRoutes.HOME, inclusive = false)
                    }
                )
            }
            composable(MicroJobRoutes.POST_JOB) {
                PostJobScreen(
                    vm = postJobVm,
                    onBack = { navController.popBackStack() },
                    onPublished = { jobId ->
                        // Back to Home, then show "Job posted" with an Undo action.
                        navController.popBackStack()
                        postJobVm.resetForm() // clear the Success state so + works again
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Job posted successfully",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short,
                                withDismissAction = true
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                // Remove the job and return to the form (fields kept).
                                postJobVm.undoPublish(jobId)
                                navController.navigate(MicroJobRoutes.POST_JOB)
                            }
                        }
                    }
                )
            }
            composable(
                route = MicroJobRoutes.JOB_DETAIL,
                arguments = listOf(navArgument("jobId") { type = NavType.IntType })
            ) { entry ->
                val jobId = entry.arguments?.getInt("jobId") ?: return@composable
                JobDetailScreen(
                    jobId = jobId,
                    onBack = { navController.popBackStack() },
                    onContactPoster = { poster ->
                        val posterId = poster?.id
                        val me = authVm.currentUser.value?.id
                        if (posterId == null) {
                            // Unknown poster — nothing to chat with.
                            scope.launch { snackbarHostState.showSnackbar("Poster is unavailable.") }
                        } else if (posterId == me) {
                            // Can't contact yourself.
                            scope.launch { snackbarHostState.showSnackbar("This is your own job.") }
                        } else {
                            if (currentUser == null) {
                                navController.navigate(MicroJobRoutes.LOGIN)
                            } else {
                                navController.navigate(MicroJobRoutes.chatDetail(posterId))
                            }
                        }
                    }
                )
            }
            composable(MicroJobRoutes.LOGIN) {
                LoginScreen(
                    vm = authVm,
                    onBack = { navController.popBackStack() },
                    // Navigation back to Home is handled by the welcome snackbar
                    // LaunchedEffect above (it also pops the back stack).
                    onLoggedIn = {}
                )
            }
            composable(MicroJobRoutes.VOICE_TRANSLATION) {
                VoiceTranslationScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
        }

        // Snackbar drawn on top of everything (including the FAB) — it must
        // be a sibling of the Scaffold inside this Box, not inside it, so it
        // does not get pushed above the floating action button.
        SnackbarHost(
            hostState = snackbarHostState,
            // Keep the snackbar above the bottom navigation (Home/Course/...
            // never covering it), while still drawing on top of the FAB.
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (showChrome) 96.dp else 16.dp
                )
        )
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
