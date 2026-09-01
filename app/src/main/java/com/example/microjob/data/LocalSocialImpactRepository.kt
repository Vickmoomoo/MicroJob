package com.example.microjob.data

import android.content.Context
import com.example.microjob.model.DonationRecord
import com.example.microjob.model.PointsHistoryEntry
import com.example.microjob.model.UserPoints
import com.example.microjob.model.VoucherItem
import com.example.microjob.model.sampleDonations
import com.example.microjob.model.sampleVouchers
import org.json.JSONArray

class LocalSocialImpactRepository(
    private val context: Context
) : SocialImpactRepository {

    private val prefs = context.getSharedPreferences("social_impact_local", 0)

    override suspend fun getDonationHistory(userId: Long): List<DonationRecord> {
        val json = prefs.getString("donations_$userId", null) ?: return sampleDonations
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                DonationRecord(
                    id = obj.getLong("id"),
                    userId = obj.getLong("userId"),
                    organization = obj.getString("organization"),
                    date = obj.getString("date"),
                    amount = obj.getString("amount")
                )
            }
        } catch (_: Exception) {
            sampleDonations
        }
    }

    override suspend fun getVouchers(): List<VoucherItem> {
        val json = prefs.getString("vouchers", null) ?: return sampleVouchers
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                val rulesArr = obj.getJSONArray("rules")
                val rules = (0 until rulesArr.length()).map { rulesArr.getString(it) }
                VoucherItem(
                    id = obj.getInt("id"),
                    brand = obj.getString("brand"),
                    title = obj.getString("title"),
                    validStores = obj.getString("validStores"),
                    pointsRequired = obj.getInt("pointsRequired"),
                    value = obj.getString("value"),
                    brandColorHex = obj.getLong("brandColorHex"),
                    description = obj.getString("description"),
                    rules = rules
                )
            }
        } catch (_: Exception) {
            sampleVouchers
        }
    }

    override suspend fun getUserPoints(userId: Long): UserPoints? {
        val points = prefs.getInt("points_$userId", -1)
        return if (points >= 0) UserPoints(userId = userId, points = points) else null
    }

    override suspend fun getPointsHistory(userId: Long): List<PointsHistoryEntry> {
        val json = prefs.getString("points_history_$userId", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                PointsHistoryEntry(
                    id = obj.getLong("id"),
                    userId = obj.getLong("userId"),
                    source = obj.getString("source"),
                    points = obj.getInt("points"),
                    date = obj.getString("date"),
                    isEarned = obj.getBoolean("isEarned")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun upsertUserPoints(userId: Long, points: Int) {
        prefs.edit().putInt("points_$userId", points).apply()
    }

    override suspend fun addPointsHistory(entry: PointsHistoryEntry) {
        val current = getPointsHistory(entry.userId).toMutableList()
        current.add(entry)
        val arr = JSONArray()
        current.forEach { e ->
            val obj = org.json.JSONObject().apply {
                put("id", e.id)
                put("userId", e.userId)
                put("source", e.source)
                put("points", e.points)
                put("date", e.date)
                put("isEarned", e.isEarned)
            }
            arr.put(obj)
        }
        prefs.edit().putString("points_history_${entry.userId}", arr.toString()).apply()
    }
}
