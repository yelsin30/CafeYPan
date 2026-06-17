package com.example.cafeypan.data.local.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "cafeypan_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROL = "user_rol"
        private const val KEY_USER_PIN = "user_pin"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKOUT_TIMESTAMP = "lockout_timestamp"
        private const val LOCKOUT_DURATION_MS = 30000L // 30 seconds
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_LOCAL_NAME = "local_name"
    }

    fun saveUserSession(id: Int, nombre: String, rol: String, pin: String) {
        sharedPreferences.edit().apply {
            putInt(KEY_USER_ID, id)
            putString(KEY_USER_NAME, nombre)
            putString(KEY_USER_ROL, rol)
            putString(KEY_USER_PIN, pin)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun clearUserSession() {
        sharedPreferences.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_ROL)
            remove(KEY_USER_PIN)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }

    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getUserId(): Int = sharedPreferences.getInt(KEY_USER_ID, -1)
    fun getUserName(): String? = sharedPreferences.getString(KEY_USER_NAME, null)
    fun getUserRol(): String? = sharedPreferences.getString(KEY_USER_ROL, null)
    fun getUserPin(): String? = sharedPreferences.getString(KEY_USER_PIN, null)

    fun incrementFailedAttempts(): Int {
        val attempts = getFailedAttempts() + 1
        sharedPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply()
        if (attempts >= 3) {
            lockUser()
        }
        return attempts
    }

    fun getFailedAttempts(): Int = sharedPreferences.getInt(KEY_FAILED_ATTEMPTS, 0)

    fun resetFailedAttempts() {
        sharedPreferences.edit().putInt(KEY_FAILED_ATTEMPTS, 0).remove(KEY_LOCKOUT_TIMESTAMP).apply()
    }

    private fun lockUser() {
        sharedPreferences.edit().putLong(KEY_LOCKOUT_TIMESTAMP, System.currentTimeMillis()).apply()
    }

    fun isLockedOut(): Boolean {
        val lockoutTime = sharedPreferences.getLong(KEY_LOCKOUT_TIMESTAMP, 0L)
        if (lockoutTime == 0L) return false
        val timePassed = System.currentTimeMillis() - lockoutTime
        val locked = timePassed < LOCKOUT_DURATION_MS
        if (!locked) {
            // Lockout period expired, reset failed attempts
            resetFailedAttempts()
        }
        return locked
    }

    fun getLockoutTimeRemaining(): Long {
        val lockoutTime = sharedPreferences.getLong(KEY_LOCKOUT_TIMESTAMP, 0L)
        if (lockoutTime == 0L) return 0L
        val timePassed = System.currentTimeMillis() - lockoutTime
        val remainingMs = LOCKOUT_DURATION_MS - timePassed
        return if (remainingMs > 0L) remainingMs / 1000L else 0L
    }

    fun isOnboardingCompleted(): Boolean = sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun completeOnboarding(localName: String) {
        sharedPreferences.edit().apply {
            putBoolean(KEY_ONBOARDING_COMPLETED, true)
            putString(KEY_LOCAL_NAME, localName)
            apply()
        }
    }

    fun getLocalName(): String? = sharedPreferences.getString(KEY_LOCAL_NAME, null)

    fun isKeepScreenOnEnabled(): Boolean = sharedPreferences.getBoolean("keep_screen_on", false)
    fun setKeepScreenOnEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("keep_screen_on", enabled).apply()
    }

    fun isBiometricEnabled(): Boolean = sharedPreferences.getBoolean("biometric_enabled", false)
    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun saveBiometricPin(pin: String) {
        sharedPreferences.edit().putString("biometric_pin", pin).apply()
    }

    fun getBiometricPin(): String? = sharedPreferences.getString("biometric_pin", null)

    fun clearBiometricPin() {
        sharedPreferences.edit().remove("biometric_pin").apply()
    }

    fun getThemePreference(): String = sharedPreferences.getString("theme_preference", "system") ?: "system"
    fun setThemePreference(theme: String) {
        sharedPreferences.edit().putString("theme_preference", theme).apply()
    }
}
