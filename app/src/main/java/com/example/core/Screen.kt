package com.example.core

sealed class Screen(val route: String) {
    object Splash : Screen("splash_screen")
    object Home : Screen("home_screen")
    object Settings : Screen("settings_screen")
    object WorkspaceList : Screen("workspace_list_screen")
    object WorkspaceForm : Screen("workspace_form_screen?workspaceId={workspaceId}") {
        fun createRoute(workspaceId: String? = null): String {
            return if (workspaceId != null) {
                "workspace_form_screen?workspaceId=$workspaceId"
            } else {
                "workspace_form_screen"
            }
        }
    }
    object StorageSetupWizard : Screen("storage_setup_wizard_screen/{workspaceId}") {
        fun createRoute(workspaceId: String): String {
            return "storage_setup_wizard_screen/$workspaceId"
        }
    }
    object EventList : Screen("event_list_screen")
    object EventForm : Screen("event_form_screen?eventId={eventId}") {
        fun createRoute(eventId: String? = null): String {
            return if (eventId != null) {
                "event_form_screen?eventId=$eventId"
            } else {
                "event_form_screen"
            }
        }
    }
    object EventDetails : Screen("event_details_screen/{eventId}") {
        fun createRoute(eventId: String): String {
            return "event_details_screen/$eventId"
        }
    }
    object UploadProgress : Screen("upload_progress_screen/{eventId}") {
        fun createRoute(eventId: String): String {
            return "upload_progress_screen/$eventId"
        }
    }
    object GeneratedContent : Screen("generated_content_screen/{eventId}") {
        fun createRoute(eventId: String): String {
            return "generated_content_screen/$eventId"
        }
    }
    object MediaCenter : Screen("media_center_screen")
    object Ideas : Screen("ideas_screen")
    object Search : Screen("global_search_screen")
    object ContentDetail : Screen("content_detail_screen/{contentId}") {
        fun createRoute(contentId: String): String {
            return "content_detail_screen/$contentId"
        }
    }
}
