package com.example.microjob.data

/**
 * Supabase connection settings.
 *
 * Fill in your project credentials from Supabase Dashboard:
 *   Project URL      : Dashboard → Project Settings → API → Project URL
 *   anon public key  : Dashboard → Project Settings → API Keys → anon public
 *
 * IMPORTANT: use the anon public key, never the secret key — this file is
 * committed to GitHub (RLS policies keep the anon key safe).
 */
object SupabaseConfig {
    const val SUPABASE_URL = "https://qjfpdigqtbzbopsynpnz.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_2JNYr6Lzh3EgOqgWuHlXHg_Qdxgk9Gq"

    /** True when both placeholders have been replaced with real credentials. */
    val isConfigured: Boolean
        get() = SUPABASE_URL.startsWith("https://") && SUPABASE_ANON_KEY.startsWith("sb_publishable")
}
