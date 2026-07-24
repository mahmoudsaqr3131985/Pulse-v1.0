package com.example.services

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AuthService {
    val isSignedIn: StateFlow<Boolean>
    val userEmail: StateFlow<String?>
    val grantedScopes: StateFlow<List<String>>
    
    suspend fun signInWithGoogle(email: String = "mahmoudsaqr3131985@gmail.com"): Result<String>
    suspend fun signOut(): Result<Unit>
}

class GoogleAuthManager private constructor(context: Context) : AuthService {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isSignedIn = MutableStateFlow(prefs.getBoolean(KEY_IS_SIGNED_IN, false))
    override val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString(KEY_USER_EMAIL, null))
    override val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _grantedScopes = MutableStateFlow(
        listOf("https://www.googleapis.com/auth/drive.file", "https://www.googleapis.com/auth/userinfo.email")
    )
    override val grantedScopes: StateFlow<List<String>> = _grantedScopes.asStateFlow()

    override suspend fun signInWithGoogle(email: String): Result<String> {
        return try {
            val validEmail = email.trim().ifEmpty { "mahmoudsaqr3131985@gmail.com" }
            prefs.edit()
                .putBoolean(KEY_IS_SIGNED_IN, true)
                .putString(KEY_USER_EMAIL, validEmail)
                .putLong(KEY_LAST_LOGIN, System.currentTimeMillis())
                .apply()

            _isSignedIn.value = true
            _userEmail.value = validEmail
            Result.success(validEmail)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            prefs.edit().clear().apply()
            _isSignedIn.value = false
            _userEmail.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val PREFS_NAME = "pulse_google_auth_prefs"
        private const val KEY_IS_SIGNED_IN = "key_is_signed_in"
        private const val KEY_USER_EMAIL = "key_user_email"
        private const val KEY_LAST_LOGIN = "key_last_login"

        @Volatile
        private var instance: GoogleAuthManager? = null

        fun getInstance(context: Context): GoogleAuthManager {
            return instance ?: synchronized(this) {
                instance ?: GoogleAuthManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

