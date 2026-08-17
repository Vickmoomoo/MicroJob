package com.example.microjob.data

import android.content.Context
import com.example.microjob.model.User

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
            val editor = prefs.edit()
            if (value == null) {
                // Remove the key entirely; storing -1 would make contains()
                // return true and isLoggedIn would misreport a logged-in user.
                editor.remove(KEY_USER_ID)
            } else {
                editor.putLong(KEY_USER_ID, value)
            }
            editor.apply()
        }

    val isLoggedIn: Boolean get() = currentUserId != null

    fun logout() {
        prefs.edit().remove(KEY_USER_ID).apply()
    }

    companion object {
        private const val KEY_USER_ID = "current_user_id"
    }
}
