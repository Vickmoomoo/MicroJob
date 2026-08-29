package com.example.microjob.data

import android.content.Context

/**
 * Creates the active data source implementation.
 *
 * - When Supabase credentials are configured → Supabase repositories
 *   (real cloud backend; the UI keeps working through the same interfaces).
 * - When not configured (placeholder) → local JSON repositories, so the app
 *   still runs offline with SampleData fallback (school demo safe).
 */
object RepositoryProvider {

    fun jobRepository(context: Context): JobRepository =
        if (SupabaseConfig.isConfigured) {
            SupabaseJobRepository(context)
        } else {
            LocalJobRepository(context)
        }

    fun chatRepository(context: Context): ChatRepository =
        if (SupabaseConfig.isConfigured) {
            SupabaseChatRepository(context)
        } else {
            LocalChatRepository(context)
        }
}
