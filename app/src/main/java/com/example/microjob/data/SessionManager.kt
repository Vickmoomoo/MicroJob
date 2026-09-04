package com.example.microjob.data

import android.content.Context
import androidx.core.content.edit

/**
 * Stores the current logged-in user id in SharedPreferences so the login
 * survives app restarts.
 */
class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences("session", Context.MODE_PRIVATE)

    /** id of the logged-in user, or null when nobody is logged in. */
    var currentUserId: Long?
        get() = if (prefs.contains(KEY_USER_ID)) prefs.getLong(KEY_USER_ID, -1) else null
        set(value) {
            prefs.edit {
                if (value == null) {
                    // Remove the key entirely; storing -1 would make contains()
                    // return true and isLoggedIn would misreport a logged-in user.
                    remove(KEY_USER_ID)
                } else {
                    putLong(KEY_USER_ID, value)
                }
            }
        }

    /** Email of the current user, retained for the profile when the public
     * profile view masks email because the Auth session is unavailable. */
    var currentUserEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) {
            prefs.edit {
                if (value.isNullOrBlank()) remove(KEY_USER_EMAIL) else putString(KEY_USER_EMAIL, value)
            }
        }

    val isLoggedIn: Boolean get() = currentUserId != null

    fun logout() {
        prefs.edit {
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
        }
    }

    companion object {
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_USER_EMAIL = "current_user_email"
    }
}
