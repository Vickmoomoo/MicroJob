package com.example.microjob.data

import android.content.Context
import com.example.microjob.model.DonationRecord
import com.example.microjob.model.PointsHistoryEntry
import com.example.microjob.model.UserPoints
import com.example.microjob.model.VoucherItem
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabaseSocialImpactRepository(
    private val context: Context
) : SocialImpactRepository {

    private val client: SupabaseClient by lazy { SupabaseClientHolder.client }

    /**
     * Sign in with Supabase Auth using email/password.
     * This creates an authenticated session so RLS policies work.
     * Called once when the user logs into the app.
     */
    suspend fun signIn(email: String, password: String): Boolean {
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Sign up a new user with Supabase Auth.
     */
    suspend fun signUp(email: String, password: String): Boolean {
        return try {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Check if user is currently authenticated.
     */
    fun isAuthenticated(): Boolean {
        return client.auth.currentSessionOrNull() != null
    }

    override suspend fun getDonationHistory(userId: Long): List<DonationRecord> {
        return try {
            client.from("donation_history")
                .select { filter { eq("user_id", userId) } }
                .decodeList<DonationRecordDto>()
                .map { it.toDonationRecord() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getVouchers(): List<VoucherItem> {
        return try {
            client.from("vouchers")
                .select { filter { eq("is_active", true) } }
                .decodeList<VoucherDto>()
                .map { it.toVoucherItem() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getUserPoints(userId: Long): UserPoints? {
        return try {
            client.from("user_points")
                .select { filter { eq("user_id", userId) } }
                .decodeList<UserPointsDto>()
                .firstOrNull()
                ?.let { UserPoints(id = it.id, userId = it.userId, points = it.points) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getPointsHistory(userId: Long): List<PointsHistoryEntry> {
        return try {
            client.from("points_history")
                .select { filter { eq("user_id", userId) } }
                .decodeList<PointsHistoryEntryDto>()
                .map { it.toPointsHistoryEntry() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun upsertUserPoints(userId: Long, points: Int) {
        try {
            val existing = getUserPoints(userId)
            if (existing != null) {
                client.from("user_points")
                    .update(mapOf("points" to points)) {
                        filter { eq("user_id", userId) }
                    }
            } else {
                client.from("user_points")
                    .insert(UserPointsInput(userId = userId, points = points))
            }
        } catch (_: Exception) {}
    }

    override suspend fun addPointsHistory(entry: PointsHistoryEntry) {
        try {
            client.from("points_history")
                .insert(PointsHistoryInput(
                    userId = entry.userId,
                    source = entry.source,
                    points = entry.points,
                    date = entry.date,
                    isEarned = entry.isEarned
                ))
        } catch (_: Exception) {}
    }
}

@Serializable
private data class DonationRecordDto(
    val id: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    val organization: String = "",
    val date: String = "",
    val amount: String = ""
) {
    fun toDonationRecord() = DonationRecord(
        id = id, userId = userId, organization = organization, date = date, amount = amount
    )
}

@Serializable
private data class VoucherDto(
    val id: Int = 0,
    val brand: String = "",
    val title: String = "",
    @SerialName("valid_stores") val validStores: String = "",
    @SerialName("points_required") val pointsRequired: Int = 0,
    val value: String = "",
    @SerialName("brand_color") val brandColor: Long = 0,
    val description: String = "",
    val rules: List<String> = emptyList()
) {
    fun toVoucherItem() = VoucherItem(
        id = id, brand = brand, title = title, validStores = validStores,
        pointsRequired = pointsRequired, value = value, brandColorHex = brandColor,
        description = description, rules = rules
    )
}

@Serializable
private data class UserPointsDto(
    val id: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    val points: Int = 0
)

@Serializable
private data class PointsHistoryEntryDto(
    val id: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    val source: String = "",
    val points: Int = 0,
    val date: String = "",
    @SerialName("is_earned") val isEarned: Boolean = true
) {
    fun toPointsHistoryEntry() = PointsHistoryEntry(
        id = id, userId = userId, source = source, points = points,
        date = date, isEarned = isEarned
    )
}

@Serializable
private data class UserPointsInput(
    @SerialName("user_id") val userId: Long,
    val points: Int
)

@Serializable
private data class PointsHistoryInput(
    @SerialName("user_id") val userId: Long,
    val source: String,
    val points: Int,
    val date: String,
    @SerialName("is_earned") val isEarned: Boolean
)
