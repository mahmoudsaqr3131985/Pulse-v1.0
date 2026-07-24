package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.Screen
import com.example.screens.event.EventDetailsScreen
import com.example.screens.event.EventFormScreen
import com.example.screens.event.EventListScreen
import com.example.screens.event.GeneratedContentScreen
import com.example.screens.event.EventViewModel
import com.example.screens.event.UploadProgressScreen
import com.example.screens.event.MediaCenterScreen
import com.example.screens.event.IdeasScreen
import com.example.screens.event.SearchScreen
import com.example.screens.event.AIContentDetailsScreen
import com.example.screens.home.HomeScreen
import com.example.screens.settings.SettingsScreen
import com.example.screens.splash.SplashScreen
import com.example.screens.workspace.StorageSetupWizardScreen
import com.example.screens.workspace.WorkspaceFormScreen
import com.example.screens.workspace.WorkspaceListScreen
import com.example.screens.workspace.WorkspaceViewModel
import com.example.ui.theme.PulseTheme
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import com.example.utils.PreferencesManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Must be called before super.onCreate() per androidx.core.splashscreen contract.
        // Handles the Android 12+ system splash screen and its pre-31 backport in one call.
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // PulseApplication already calls this, but guard again here defensively: if a
        // developer ever runs MainActivity in a test harness that bypasses the Application
        // class, this ensures themeFlow/languageFlow are still populated instead of throwing
        // UninitializedPropertyAccessException the first time a preference is read below.
        if (!PreferencesManager.isInitialized()) {
            runCatching { PreferencesManager.init(this) }
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by PreferencesManager.themeFlow.collectAsState()
            val languageMode by PreferencesManager.languageFlow.collectAsState()

            val isDarkTheme = when (themeMode) {
                PreferencesManager.THEME_LIGHT -> false
                PreferencesManager.THEME_DARK -> true
                else -> isSystemInDarkTheme()
            }

            val layoutDirection = PreferencesManager.getLayoutDirection()

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                PulseTheme(darkTheme = isDarkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        PulseAppNavigation()
                    }
                }
            }
        }
    }
}

@Composable
fun PulseAppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val workspaceViewModel: WorkspaceViewModel = viewModel()
    val eventViewModel: EventViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToWorkspaces = {
                    navController.navigate(Screen.WorkspaceList.route)
                },
                onNavigateToCreateEvent = {
                    navController.navigate(Screen.EventForm.createRoute(null))
                },
                onNavigateToEventList = {
                    navController.navigate(Screen.EventList.route)
                },
                onNavigateToMediaCenter = {
                    navController.navigate(Screen.MediaCenter.route)
                },
                onNavigateToIdeas = {
                    navController.navigate(Screen.Ideas.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToEventDetail = { eventId ->
                    navController.navigate(Screen.EventDetails.createRoute(eventId))
                },
                onNavigateToContentDetail = { contentId ->
                    navController.navigate(Screen.ContentDetail.createRoute(contentId))
                }
            )
        }

        composable(Screen.EventList.route) {
            EventListScreen(
                viewModel = eventViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCreateEvent = {
                    navController.navigate(Screen.EventForm.createRoute(null))
                },
                onNavigateToEditEvent = { eventId ->
                    navController.navigate(Screen.EventForm.createRoute(eventId))
                },
                onNavigateToEventDetails = { eventId ->
                    navController.navigate(Screen.EventDetails.createRoute(eventId))
                }
            )
        }

        composable(
            route = Screen.EventForm.route,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId")
            EventFormScreen(
                viewModel = eventViewModel,
                eventId = eventId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSavedSuccessfully = {
                    Toast.makeText(context, "Event saved successfully.", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EventDetails.route,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EventDetailsScreen(
                viewModel = eventViewModel,
                eventId = eventId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = { editId ->
                    navController.navigate(Screen.EventForm.createRoute(editId))
                },
                onNavigateToStorageWizard = { wsId ->
                    navController.navigate(Screen.StorageSetupWizard.createRoute(wsId))
                },
                onNavigateToWorkspaces = {
                    navController.navigate(Screen.WorkspaceList.route)
                },
                onNavigateToUploadProgress = { id ->
                    navController.navigate(Screen.UploadProgress.createRoute(id))
                },
                onNavigateToGeneratedContent = { id ->
                    navController.navigate(Screen.GeneratedContent.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.GeneratedContent.route,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            GeneratedContentScreen(
                eventId = eventId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.UploadProgress.route,
            arguments = listOf(
                navArgument("eventId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            UploadProgressScreen(
                viewModel = eventViewModel,
                eventId = eventId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = workspaceViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToStorageWizard = { workspaceId ->
                    navController.navigate(Screen.StorageSetupWizard.createRoute(workspaceId))
                }
            )
        }

        composable(Screen.WorkspaceList.route) {
            WorkspaceListScreen(
                viewModel = workspaceViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCreate = {
                    navController.navigate(Screen.WorkspaceForm.createRoute(null))
                },
                onNavigateToEdit = { workspaceId ->
                    navController.navigate(Screen.WorkspaceForm.createRoute(workspaceId))
                },
                onNavigateToStorageWizard = { workspaceId ->
                    navController.navigate(Screen.StorageSetupWizard.createRoute(workspaceId))
                }
            )
        }

        composable(
            route = Screen.WorkspaceForm.route,
            arguments = listOf(
                navArgument("workspaceId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val workspaceId = backStackEntry.arguments?.getString("workspaceId")
            WorkspaceFormScreen(
                viewModel = workspaceViewModel,
                workspaceId = workspaceId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToStorageWizard = { savedId ->
                    navController.popBackStack()
                    navController.navigate(Screen.StorageSetupWizard.createRoute(savedId))
                }
            )
        }

        composable(
            route = Screen.StorageSetupWizard.route,
            arguments = listOf(
                navArgument("workspaceId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val workspaceId = backStackEntry.arguments?.getString("workspaceId") ?: ""
            StorageSetupWizardScreen(
                viewModel = workspaceViewModel,
                workspaceId = workspaceId,
                onNavigateComplete = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.MediaCenter.route) {
            MediaCenterScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToContentDetail = { contentId ->
                    navController.navigate(Screen.ContentDetail.createRoute(contentId))
                }
            )
        }

        composable(Screen.Ideas.route) {
            IdeasScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEvent = { eventId ->
                    navController.navigate(Screen.EventDetails.createRoute(eventId))
                },
                onNavigateToContent = { contentId ->
                    navController.navigate(Screen.ContentDetail.createRoute(contentId))
                },
                onNavigateToIdeas = {
                    navController.navigate(Screen.Ideas.route)
                }
            )
        }

        composable(
            route = Screen.ContentDetail.route,
            arguments = listOf(
                navArgument("contentId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val contentId = backStackEntry.arguments?.getString("contentId") ?: ""
            AIContentDetailsScreen(
                contentId = contentId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToNewContent = { newContentId ->
                    navController.navigate(Screen.ContentDetail.createRoute(newContentId)) {
                        popUpTo(Screen.ContentDetail.createRoute(contentId)) { inclusive = true }
                    }
                }
            )
        }
    }
}
