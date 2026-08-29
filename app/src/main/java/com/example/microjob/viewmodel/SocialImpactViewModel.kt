package com.example.microjob.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import com.example.microjob.model.DonationRecord
import com.example.microjob.model.PointsHistoryEntry
import com.example.microjob.model.VoucherItem
import com.example.microjob.model.sampleDonations
import com.example.microjob.model.sampleVouchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SocialImpactUiState(
    val userPoints: Int = 0,
    val peopleHelped: Int = 128,
    val totalDonated: String = "RM 15,680",
    val donationHistory: List<DonationRecord> = sampleDonations,
    val voucherList: List<VoucherItem> = sampleVouchers,
    val pointsHistory: List<PointsHistoryEntry> = emptyList(),
    val gamesPlayedToday: Int = 0,
    val maxGamesPerDay: Int = 6,
)

class SocialImpactViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("game_prefs", 0)

    private val _uiState = MutableStateFlow(SocialImpactUiState())
    val uiState: StateFlow<SocialImpactUiState> = _uiState.asStateFlow()

    private var currentUserId: Long? = null

    fun setUserId(userId: Long) {
        if (currentUserId == userId) return
        currentUserId = userId
        // Reset to initial state when switching accounts
        _uiState.value = SocialImpactUiState()
        loadGameCount()
    }

    private fun loadGameCount() {
        val userId = currentUserId ?: return
        val keyPrefix = "user_${userId}_"
        val savedDate = prefs.getString("${keyPrefix}game_date", "")
        val today = java.time.LocalDate.now().toString()
        val count = if (savedDate == today) prefs.getInt("${keyPrefix}game_count", 0) else 0
        _uiState.value = _uiState.value.copy(gamesPlayedToday = count)
    }

    fun canPlayGame(): Boolean {
        val current = _uiState.value
        if (current.gamesPlayedToday >= current.maxGamesPerDay) return false

        val userId = currentUserId ?: return false
        val keyPrefix = "user_${userId}_"
        val today = java.time.LocalDate.now().toString()
        val newCount = current.gamesPlayedToday + 1
        prefs.edit()
            .putString("${keyPrefix}game_date", today)
            .putInt("${keyPrefix}game_count", newCount)
            .apply()
        _uiState.value = current.copy(gamesPlayedToday = newCount)
        return true
    }

    fun redeemVoucher(voucher: VoucherItem) {
        val current = _uiState.value
        val cost = voucher.pointsRequired
        if (current.userPoints >= cost) {
            _uiState.value = current.copy(
                userPoints = current.userPoints - cost,
                pointsHistory = current.pointsHistory + PointsHistoryEntry(
                    source = "Redeemed ${voucher.title}",
                    points = -cost,
                    date = "Today",
                    isEarned = false
                )
            )
        }
    }

    fun earnPoints(source: String, points: Int) {
        val current = _uiState.value
        _uiState.value = current.copy(
            userPoints = current.userPoints + points,
            pointsHistory = current.pointsHistory + PointsHistoryEntry(
                source = source,
                points = points,
                date = "Today",
                isEarned = true
            )
        )
    }
}
