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

    val isLoggedIn: Boolean get() = currentUserId != null

    fun logout() {
        prefs.edit { remove(KEY_USER_ID) }
    }

    companion object {
        private const val KEY_USER_ID = "current_user_id"
    }
}
