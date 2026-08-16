package com.example.microjob.data

import com.example.microjob.model.Category
import com.example.microjob.model.Job
import com.example.microjob.model.User

/**
 * Data source for jobs, categories and users.
 *
 * The UI talks to this interface only, so the implementation can be swapped
 * between Supabase, a Room database, or the local SampleData without touching
 * the ViewModel / UI layer.
 */
interface JobRepository {

    /** Returns all jobs (fetched from the backend). Throws on network failure. */
    suspend fun getJobs(): List<Job>

    /** Returns all job categories. Throws on network failure. */
    suspend fun getCategories(): List<Category>

    /** Returns a single job by id, or null when it does not exist. */
    suspend fun getJob(id: Int): Job?

    /** Returns a single user by id, or null when it does not exist. */
    suspend fun getUser(id: Long): User?
}
