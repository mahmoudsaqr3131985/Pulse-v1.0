package com.example

import android.app.Application
import android.util.Log
import com.example.utils.PreferencesManager

/**
 * Application entry point for Pulse.
 *
 * Centralizing process-wide setup here (instead of only in MainActivity.onCreate) ensures
 * critical singletons are ready before ANY activity, service, or content provider in the app
 * needs them, and lets us fail safely (log + continue with sane defaults) instead of crashing
 * the whole process during cold start if a single piece of initialization misbehaves.
 */
class PulseApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Catch anything that would otherwise crash the app with no diagnostic trail,
        // log it clearly, and defer to the platform's default handler so behavior for
        // the end user is unchanged (still crashes if truly fatal) but is diagnosable.
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        initPreferences()
    }

    private fun initPreferences() {
        try {
            PreferencesManager.init(this)
        } catch (t: Throwable) {
            // Preferences failing to load must never prevent the app from launching;
            // PreferencesManager falls back to in-memory defaults (SYSTEM theme/language)
            // when init() hasn't completed, so it is safe to continue.
            Log.e(TAG, "Failed to initialize PreferencesManager", t)
        }
    }

    companion object {
        private const val TAG = "PulseApplication"
    }
}
