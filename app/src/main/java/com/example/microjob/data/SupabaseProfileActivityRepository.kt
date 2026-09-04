package com.example.microjob.data

import android.content.Context
import com.example.microjob.model.ProfileActivity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal fun supabaseActivityFileUrl(path: String): String =
    com.example.microjob.data.SupabaseConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/profile-activity-images/$path"

class SupabaseProfileActivityRepository(
    private val context: Context
) {
    private val client: SupabaseClient by lazy { SupabaseClientHolder.client }

    suspend fun getForUser(userId: Long): List<ProfileActivity> {
        // Newest first so the latest post stays on top. Let errors propagate
        // so loadProfile can fall back to local instead of wiping with empty.
        return client.from("profile_activities")
            .select {
                filter { eq("user_id", userId) }
                order("id", Order.DESCENDING)
            }
            .decodeList<ProfileActivityDto>()
            .map { it.toProfileActivity() }
    }

    suspend fun add(activity: ProfileActivity): ProfileActivity {
        val result = client.from("profile_activities")
            .insert(ActivityInput(
                userId = activity.userId,
                text = activity.text,
                photoUri = activity.photoUri
            )) { select() }
            .decodeSingle<ProfileActivityDto>()
        return result.toProfileActivity()
    }

    /** Uploads an activity photo to the public bucket and returns its public URL.
     *  [userId] must be the numeric public.users.id — the storage policy checks
     *  folder[2] against it, not the Auth uuid. */
    suspend fun uploadActivityImage(userId: Long, bytes: ByteArray, extension: String): String {
        val cleanExt = extension.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "jpg"
        val path = "activities/$userId/${System.currentTimeMillis()}.$cleanExt"
        client.storage.from("profile-activity-images").upload(path, bytes)
        return supabaseActivityFileUrl(path)
    }

    suspend fun delete(activityId: Long) {
        client.from("profile_activities")
            .delete { filter { eq("id", activityId) } }
    }
}

@Serializable
private data class ProfileActivityDto(
    val id: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    val text: String = "",
    @SerialName("photo_uri") val photoUri: String = "",
    @SerialName("created_at") val createdAt: String = ""
) {
    fun toProfileActivity() = ProfileActivity(
        id = id, userId = userId, text = text, photoUri = photoUri, createdAt = createdAt
    )
}

@Serializable
private data class ActivityInput(
    @SerialName("user_id") val userId: Long,
    val text: String,
    @SerialName("photo_uri") val photoUri: String
)
