package com.example.microjob.data

import android.content.Context
import com.example.microjob.model.ProfileActivity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseProfileActivityRepository(
    private val context: Context
) {
    private val client: SupabaseClient by lazy { SupabaseClientHolder.client }

    suspend fun getForUser(userId: Long): List<ProfileActivity> {
        return try {
            client.from("profile_activities")
                .select { filter { eq("user_id", userId) } }
                .decodeList<ProfileActivityDto>()
                .map { it.toProfileActivity() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun add(activity: ProfileActivity): ProfileActivity {
        val result = client.from("profile_activities")
            .insert(ActivityInput(
                userId = activity.userId,
                text = activity.text,
                photoUri = activity.photoUri
            ))
            .decodeSingle<ProfileActivityDto>()
        return result.toProfileActivity()
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
