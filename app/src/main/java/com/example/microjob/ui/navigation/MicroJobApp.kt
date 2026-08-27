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
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.filled.Favorite
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
import com.example.microjob.ui.screens.profile.MyJobDetailScreen
import com.example.microjob.ui.screens.profile.MyJobsScreen
import com.example.microjob.ui.screens.profile.ProfileScreen
import com.example.microjob.ui.screens.profile.JobListScreen
import com.example.microjob.ui.screens.profile.SettingsScreen
import com.example.microjob.ui.screens.profile.SocialImpactScreen
import com.example.microjob.ui.screens.profile.AllDonationsScreen
import com.example.microjob.ui.screens.profile.AllVouchersScreen
import com.example.microjob.ui.screens.profile.PointsHistoryScreen
import com.example.microjob.ui.screens.profile.MiniGameMenuScreen
import com.example.microjob.ui.screens.profile.TicTacToeScreen
import com.example.microjob.ui.screens.profile.NumberGuessScreen
import com.example.microjob.ui.screens.profile.MemoryFlipScreen
import com.example.microjob.ui.screens.profile.UserDetailsScreen
import com.example.microjob.ui.screens.reviews.ReviewFormScreen
import com.example.microjob.ui.screens.reviews.ReviewJobDetailScreen
import com.example.microjob.ui.screens.reviews.ReviewsListScreen
import com.example.microjob.ui.screens.translation.VoiceTranslationScreen
import com.example.microjob.viewmodel.AuthViewModel
import com.example.microjob.viewmodel.ChatViewModel
import com.example.microjob.viewmodel.HomeViewModel
import com.example.microjob.viewmodel.PostJobViewModel
import com.example.microjob.viewmodel.ProfileViewModel
import com.example.microjob.viewmodel.ReviewViewModel
import com.example.microjob.viewmodel.SocialImpactViewModel
import com.example.microjob.viewmodel.postJobViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Root composable: Scaffold (bottom bar + center FAB) hosting the NavHost. */
@Composable
fun MicroJobApp() {
    val navController = rememberNavController()
    val authVm: AuthViewModel = viewModel()
    val postJobVm: PostJobViewModel = viewModel(factory = postJobViewModelFactory())
    val chatVm: ChatViewModel = viewModel()
    val homeVm: HomeViewModel = viewModel()
    val profileVm: ProfileViewModel = viewModel()
    val reviewVm: ReviewViewModel = viewModel()
    val socialImpactVm: SocialImpactViewModel = viewModel()
    val currentUser by authVm.currentUser.collectAsStateWithLifecycle()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val unreadCount by chatVm.unreadCount.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Refresh unread count on startup and when navigating to messages
    LaunchedEffect(Unit) {
        chatVm.refreshUnreadCount()
    }
    LaunchedEffect(currentRoute) {
        if (currentRoute == MicroJobRoutes.MESSAGES) {
            chatVm.refreshUnreadCount()
            chatVm.loadConversations()
        }
    }

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
            currentRoute == MicroJobRoutes.CHAT_DETAIL ||
            currentRoute?.startsWith("user_profile") == true ||
            currentRoute == MicroJobRoutes.SETTINGS ||
            currentRoute?.startsWith("user_details") == true ||
            currentRoute?.startsWith("review_form") == true ||
            currentRoute?.startsWith("reviews") == true ||
            currentRoute?.startsWith("posted_jobs") == true ||
            currentRoute?.startsWith("accepted_jobs") == true ||
            currentRoute?.startsWith("my_jobs") == true ||
            currentRoute?.startsWith("my_job_detail") == true ||
            currentRoute?.startsWith("review_job") == true ||
            currentRoute == MicroJobRoutes.SOCIAL_IMPACT ||
            currentRoute == MicroJobRoutes.DONATION_HISTORY ||
            currentRoute == MicroJobRoutes.VOUCHER_REDEEM ||
            currentRoute == MicroJobRoutes.POINTS_HISTORY ||
            currentRoute == MicroJobRoutes.MINI_GAME_MENU ||
            currentRoute == MicroJobRoutes.TIC_TAC_TOE ||
            currentRoute == MicroJobRoutes.NUMBER_GUESS ||
            currentRoute == MicroJobRoutes.MEMORY_FLIP
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
                            if (currentUser != null) {
                                navController.navigateToTab(route)
                            } else {
                                navController.navigate(MicroJobRoutes.LOGIN)
                            }
                        } else {
                            navController.navigateToTab(route)
                        }
                    },
                    unreadCount = unreadCount
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
            // Keep top inset stable across tab ↔ full-screen to avoid the
            // "everything moving up when innerPadding disappears" jump.
            // Only the bottom inset depends on showChrome (bottom bar).
            modifier = Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .padding(bottom = if (showChrome) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            composable(
                route = MicroJobRoutes.HOME) {
                HomeScreen(
                    vm = homeVm,
                    onJobClick = { job -> navController.navigate(MicroJobRoutes.jobDetail(job.id)) }
                )
            }
            composable(
                route = MicroJobRoutes.COURSE) {
                PlaceholderScreen("Course & Certification")
            }
            composable(
                route = MicroJobRoutes.MESSAGES) {
                ChatListScreen(
                    vm = chatVm,
                    onChatClick = { otherUserId ->
                        navController.navigate(MicroJobRoutes.chatDetail(otherUserId))
                    }
                )
            }
            composable(
                route = MicroJobRoutes.PROFILE) {
                val myId = currentUser?.id ?: return@composable
                // Reload profile every time we land on it (picks up new reviews, etc.)
                LaunchedEffect(currentRoute) {
                    profileVm.loadProfile(myId)
                }
                ProfileScreen(
                    userId = myId,
                    vm = profileVm,
                     onNavigateToSettings = { navController.navigate(MicroJobRoutes.SETTINGS) },
                     onNavigateToUserDetails = { navController.navigate(MicroJobRoutes.userDetails(myId)) },
                    onNavigateToPostedJobs = { navController.navigate(MicroJobRoutes.postedJobs(myId)) },
                    onNavigateToAcceptedJobs = { navController.navigate(MicroJobRoutes.acceptedJobs(myId)) },
                    onNavigateToMyJobs = { navController.navigate(MicroJobRoutes.myJobs(myId)) },
                    onNavigateToReviews = {
                        navController.navigate(MicroJobRoutes.reviewsList(myId))
                    },
                    onNavigateToCertificates = { /* TODO */ },
                    onNavigateToSocialImpact = { navController.navigate(MicroJobRoutes.SOCIAL_IMPACT) },
                    onNavigateToMiniGames = { navController.navigate(MicroJobRoutes.MINI_GAME_MENU) },
                    onNavigateToPointsHistory = { navController.navigate(MicroJobRoutes.POINTS_HISTORY) },
                    onNavigateToChat = { otherUserId ->
                        navController.navigate(MicroJobRoutes.chatDetail(otherUserId))
                    },
                    onLogout = {
                        authVm.logout()
                        navController.popBackStack(MicroJobRoutes.HOME, inclusive = false)
                    }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.CHAT_DETAIL,
                arguments = listOf(navArgument("otherUserId") { type = NavType.LongType })
            ) { entry ->
                val otherUserId = entry.arguments?.getLong("otherUserId") ?: return@composable
                ChatDetailScreen(
                    otherUserId = otherUserId,
                    onBack = { navController.popBackStack() },
                    onOtherUserClick = { userId ->
                        navController.navigate(MicroJobRoutes.userProfile(userId))
                    },
                    onJobCompleted = { jobTitle, jobPrice ->
                        val points = (jobPrice * 0.5).toInt().coerceAtLeast(200)
                        socialImpactVm.earnPoints("Completed: $jobTitle", points)
                    },
                    onOpenReview = { jobId ->
                        val myId = authVm.currentUser.value?.id
                        if (myId != null) {
                            val localRepo = com.example.microjob.data.LocalJobRepository(context.applicationContext)
                            scope.launch {
                                val job = withContext(Dispatchers.IO) { localRepo.getJob(jobId) }
                                if (job != null) {
                                    val reviewedUserId = if (myId == job.posterId) {
                                        job.workerId ?: 0L
                                    } else {
                                        job.posterId
                                    }
                                    if (reviewedUserId > 0) {
                                        navController.navigate(MicroJobRoutes.reviewForm(reviewedUserId, jobId))
                                    }
                                }
                            }
                        } else {
                            navController.navigate(MicroJobRoutes.LOGIN)
                        }
                    },
                    vm = chatVm
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.USER_PROFILE,
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { entry ->
                val userId = entry.arguments?.getLong("userId") ?: return@composable
                ProfileScreen(
                    userId = userId,
                    vm = profileVm,
                    onBack = { navController.popBackStack() },
                     onNavigateToSettings = { navController.navigate(MicroJobRoutes.SETTINGS) },
                     onNavigateToUserDetails = { navController.navigate(MicroJobRoutes.userDetails(userId)) },
                    onNavigateToPostedJobs = { navController.navigate(MicroJobRoutes.postedJobs(userId)) },
                    onNavigateToAcceptedJobs = { navController.navigate(MicroJobRoutes.acceptedJobs(userId)) },
                    onNavigateToMyJobs = { navController.navigate(MicroJobRoutes.myJobs(userId)) },
                    onNavigateToReviews = {
                        navController.navigate(MicroJobRoutes.reviewsList(userId))
                    },
                    onNavigateToCertificates = { /* TODO */ },
                    onNavigateToSocialImpact = { navController.navigate(MicroJobRoutes.SOCIAL_IMPACT) },
                    onNavigateToMiniGames = { navController.navigate(MicroJobRoutes.MINI_GAME_MENU) },
                    onNavigateToPointsHistory = { navController.navigate(MicroJobRoutes.POINTS_HISTORY) },
                    onNavigateToChat = { otherUserId ->
                        if (currentUser != null) {
                            navController.navigate(MicroJobRoutes.chatDetail(otherUserId))
                        } else {
                            navController.navigate(MicroJobRoutes.LOGIN)
                        }
                    },
                    onLogout = {
                        authVm.logout()
                        navController.popBackStack(MicroJobRoutes.HOME, inclusive = false)
                    }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.SETTINGS) {
                SettingsScreen(
                    vm = profileVm,
                    onBack = { navController.popBackStack() },
                    onNavigateToUserDetails = {
                        currentUser?.id?.let { navController.navigate(MicroJobRoutes.userDetails(it)) }
                    }
                )
            }
            composable(
                route = MicroJobRoutes.USER_DETAILS,
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { entry ->
                val userId = entry.arguments?.getLong("userId") ?: return@composable
                LaunchedEffect(userId) { profileVm.loadProfile(userId) }
                UserDetailsScreen(
                    vm = profileVm,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.REVIEW_FORM,
                arguments = listOf(
                    navArgument("reviewedUserId") { type = NavType.LongType },
                    navArgument("jobId") { type = NavType.IntType }
                )
            ) { entry ->
                val reviewedUserId = entry.arguments?.getLong("reviewedUserId") ?: return@composable
                val jobId = entry.arguments?.getInt("jobId") ?: 0
                val existingReviewId = entry.arguments
                    ?.takeIf { it.containsKey("reviewId") }
                    ?.getLong("reviewId")
                LaunchedEffect(reviewedUserId, jobId, existingReviewId) {
                    reviewVm.resetForm()
                    if (existingReviewId != null) {
                        reviewVm.loadReviewForEditById(reviewedUserId, existingReviewId)
                    }
                }
                ReviewFormScreen(
                    reviewedUserId = reviewedUserId,
                    jobId = if (jobId > 0) jobId.toLong() else null,
                    existingReviewId = existingReviewId,
                    vm = reviewVm,
                    onBack = { navController.popBackStack() },
                    onSubmitted = { navController.popBackStack() }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.REVIEW_FORM_EDIT,
                arguments = listOf(
                    navArgument("reviewedUserId") { type = NavType.LongType },
                    navArgument("jobId") { type = NavType.IntType },
                    navArgument("reviewId") { type = NavType.LongType }
                )
            ) { entry ->
                val reviewedUserId = entry.arguments?.getLong("reviewedUserId") ?: return@composable
                val jobId = entry.arguments?.getInt("jobId") ?: 0
                val existingReviewId = entry.arguments?.getLong("reviewId")
                LaunchedEffect(reviewedUserId, jobId, existingReviewId) {
                    reviewVm.resetForm()
                    if (existingReviewId != null) {
                        reviewVm.loadReviewForEditById(reviewedUserId, existingReviewId)
                    }
                }
                ReviewFormScreen(
                    reviewedUserId = reviewedUserId,
                    jobId = if (jobId > 0) jobId.toLong() else null,
                    existingReviewId = existingReviewId,
                    vm = reviewVm,
                    onBack = { navController.popBackStack() },
                    onSubmitted = { navController.popBackStack() }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.REVIEWS_LIST,
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { entry ->
                val userId = entry.arguments?.getLong("userId") ?: return@composable
                val profileState by profileVm.uiState.collectAsStateWithLifecycle()
                ReviewsListScreen(
                    userId = userId,
                    userName = profileState.user?.name ?: "User",
                    vm = reviewVm,
                    onBack = { navController.popBackStack() },
                    onWriteReview = { /* handled via chat review prompt */ },
                    onEditReview = { reviewId ->
                        navController.navigate(
                            MicroJobRoutes.reviewFormEdit(userId, null, reviewId)
                        )
                    },
                    onReviewClick = { jobId ->
                        navController.navigate(MicroJobRoutes.reviewJobDetail(jobId))
                    }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.REVIEW_JOB_DETAIL,
                arguments = listOf(navArgument("jobId") { type = NavType.IntType })
            ) { entry ->
                val jobId = entry.arguments?.getInt("jobId") ?: return@composable
                ReviewJobDetailScreen(jobId = jobId, onBack = { navController.popBackStack() })
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.MY_JOBS,
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { entry ->
                val userId = entry.arguments?.getLong("userId") ?: return@composable
                MyJobsScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onJobClick = { jobId -> navController.navigate(MicroJobRoutes.myJobDetail(jobId)) }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.MY_JOB_DETAIL,
                arguments = listOf(navArgument("jobId") { type = NavType.IntType })
            ) { entry ->
                val jobId = entry.arguments?.getInt("jobId") ?: return@composable
                MyJobDetailScreen(jobId = jobId, onBack = { navController.popBackStack() })
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.POSTED_JOBS,
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { entry ->
                val userId = entry.arguments?.getLong("userId") ?: return@composable
                var postedJobs by remember { mutableStateOf(emptyList<com.example.microjob.model.Job>()) }
                val ctx = androidx.compose.ui.platform.LocalContext.current
                LaunchedEffect(userId) {
                    val localRepo = com.example.microjob.data.LocalJobRepository(ctx.applicationContext)
                    postedJobs = withContext(Dispatchers.IO) { localRepo.getPostedJobs(userId) }
                }
                JobListScreen(
                    title = "Posted Jobs",
                    jobs = postedJobs,
                    onBack = { navController.popBackStack() },
                    onJobClick = { jobId -> navController.navigate(MicroJobRoutes.jobDetail(jobId)) }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.ACCEPTED_JOBS,
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { entry ->
                val userId = entry.arguments?.getLong("userId") ?: return@composable
                var acceptedJobs by remember { mutableStateOf(emptyList<com.example.microjob.model.Job>()) }
                val ctx = androidx.compose.ui.platform.LocalContext.current
                LaunchedEffect(userId) {
                    val localRepo = com.example.microjob.data.LocalJobRepository(ctx.applicationContext)
                    acceptedJobs = withContext(Dispatchers.IO) { localRepo.getAcceptedJobs(userId) }
                }
                JobListScreen(
                    title = "Accepted Jobs",
                    jobs = acceptedJobs,
                    onBack = { navController.popBackStack() },
                    onJobClick = { jobId -> navController.navigate(MicroJobRoutes.jobDetail(jobId)) }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.POST_JOB) {
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
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
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
                    },
                    onPosterClick = { posterId ->
                        navController.navigate(MicroJobRoutes.userProfile(posterId))
                    }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.LOGIN) {
                LoginScreen(
                    vm = authVm,
                    onBack = { navController.popBackStack() },
                    // Navigation back to Home is handled by the welcome snackbar
                    // LaunchedEffect above (it also pops the back stack).
                    onLoggedIn = {}
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.VOICE_TRANSLATION) {
                VoiceTranslationScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.SOCIAL_IMPACT
            ) {
                val socialImpactState by socialImpactVm.uiState.collectAsStateWithLifecycle()
                SocialImpactScreen(
                    uiState = socialImpactState,
                    onBackClick = { navController.popBackStack() },
                    onViewAllDonations = { navController.navigate(MicroJobRoutes.DONATION_HISTORY) },
                    onViewAllVouchers = { navController.navigate(MicroJobRoutes.VOUCHER_REDEEM) },
                    onRedeemVoucher = { socialImpactVm.redeemVoucher(it) }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.DONATION_HISTORY
            ) {
                val socialImpactState by socialImpactVm.uiState.collectAsStateWithLifecycle()
                AllDonationsScreen(
                    donations = socialImpactState.donationHistory,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.VOUCHER_REDEEM
            ) {
                val socialImpactState by socialImpactVm.uiState.collectAsStateWithLifecycle()
                AllVouchersScreen(
                    vouchers = socialImpactState.voucherList,
                    onBack = { navController.popBackStack() },
                    onRedeemVoucher = { socialImpactVm.redeemVoucher(it) }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.POINTS_HISTORY
            ) {
                val socialImpactState by socialImpactVm.uiState.collectAsStateWithLifecycle()
                PointsHistoryScreen(
                    userPoints = socialImpactState.userPoints,
                    pointsHistory = socialImpactState.pointsHistory,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.MINI_GAME_MENU
            ) {
                MiniGameMenuScreen(
                    onBack = { navController.popBackStack() },
                    onPlayTicTacToe = { navController.navigate(MicroJobRoutes.TIC_TAC_TOE) },
                    onPlayNumberGuess = { navController.navigate(MicroJobRoutes.NUMBER_GUESS) },
                    onPlayMemoryFlip = { navController.navigate(MicroJobRoutes.MEMORY_FLIP) }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.TIC_TAC_TOE
            ) {
                TicTacToeScreen(
                    onBack = { navController.popBackStack() },
                    onWin = { socialImpactVm.earnPoints("Won Tic Tac Toe", 200) }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.NUMBER_GUESS
            ) {
                NumberGuessScreen(
                    onBack = { navController.popBackStack() },
                    onWin = { socialImpactVm.earnPoints("Won Number Guess", 200) }
                )
            }
            composable(
                enterTransition = { fadeIn(animationSpec = tween(120)) },
                exitTransition = { fadeOut(animationSpec = tween(120)) },
                popEnterTransition = { fadeIn(animationSpec = tween(120)) },
                popExitTransition = { fadeOut(animationSpec = tween(120)) },
                route = MicroJobRoutes.MEMORY_FLIP
            ) {
                MemoryFlipScreen(
                    onBack = { navController.popBackStack() },
                    onWin = { socialImpactVm.earnPoints("Won Memory Flip", 200) }
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
    BottomTab(MicroJobRoutes.SOCIAL_IMPACT, "Social Impact", Icons.Filled.Favorite),
    BottomTab(MicroJobRoutes.MESSAGES, "Messages", Icons.Filled.Email),
    BottomTab(MicroJobRoutes.PROFILE, "Profile", Icons.Filled.Person),
)

@Composable
private fun MicroJobBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    unreadCount: Int = 0,
) {
    NavigationBar {
        bottomTabs.forEach { tab ->
            val showBadge = tab.route == MicroJobRoutes.MESSAGES && unreadCount > 0
            NavigationBarItem(
                selected = isRouteForTab(currentRoute, tab.route),
                onClick = { onNavigate(tab.route) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (showBadge) {
                                Badge {
                                    Text("$unreadCount")
                                }
                            }
                        }
                    ) {
                        Icon(imageVector = tab.icon, contentDescription = tab.label)
                    }
                },
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

/** Checks if the current route belongs to a given tab. */
private fun isRouteForTab(currentRoute: String?, tabRoute: String): Boolean {
    return currentRoute == tabRoute
}
