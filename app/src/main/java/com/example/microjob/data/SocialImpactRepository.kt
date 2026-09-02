package com.example.microjob.data

import com.example.microjob.model.CommunityImpact
import com.example.microjob.model.DonationRecord
import com.example.microjob.model.PointsHistoryEntry
import com.example.microjob.model.UserPoints
import com.example.microjob.model.VoucherItem

interface SocialImpactRepository {
    suspend fun getCommunityImpact(): CommunityImpact?
    suspend fun updateCommunityImpact(peopleHelped: Int, totalDonated: String)
    suspend fun getDonationHistory(userId: Long): List<DonationRecord>
    suspend fun getVouchers(): List<VoucherItem>
    suspend fun getUserPoints(userId: Long): UserPoints?
    suspend fun getPointsHistory(userId: Long): List<PointsHistoryEntry>
    suspend fun upsertUserPoints(userId: Long, points: Int)
    suspend fun addPointsHistory(entry: PointsHistoryEntry)
}
