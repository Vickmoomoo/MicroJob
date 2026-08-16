package com.example.microjob.data

import com.example.microjob.model.Category
import com.example.microjob.model.Job
import com.example.microjob.model.User
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest

/**
 * Supabase-backed repository. Reads jobs and categories from the
 * `jobs` and `categories` tables via PostgREST.
 */
class SupabaseJobRepository : JobRepository {

    private val client = createSupabaseClient(
        supabaseUrl = SupabaseConfig.SUPABASE_URL,
        supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
    }

    override suspend fun getJobs(): List<Job> =
        client.postgrest["jobs"].select().decodeList()

    override suspend fun getCategories(): List<Category> =
        client.postgrest["categories"].select().decodeList()

    override suspend fun getJob(id: Int): Job? =
        client.postgrest["jobs"].select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()

    override suspend fun getUser(id: Long): User? =
        client.postgrest["users"].select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()
}
