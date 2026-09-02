package com.example.microjob.viewmodel

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.microjob.data.RepositoryProvider
import com.example.microjob.model.CommunityImpact
import com.example.microjob.model.DonationRecord
import com.example.microjob.model.PointsHistoryEntry
import com.example.microjob.model.UserPoints
import com.example.microjob.model.VoucherItem
import com.example.microjob.model.sampleDonations
import com.example.microjob.model.sampleVouchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SocialImpactUiState(
    val userPoints: Int = 0,
    val peopleHelped: Int = 128,
    val totalDonated: String = "RM 15,680",
    val donationHistory: List<DonationRecord> = sampleDonations,
    val voucherList: List<VoucherItem> = sampleVouchers,
    val pointsHistory: List<PointsHistoryEntry> = emptyList(),
    val gamesPlayedToday: Int = 0,
    val maxGamesPerDay: Int = 6,
    val isLoading: Boolean = false,
)

class SocialImpactViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val prefs: SharedPreferences =
        application.getSharedPreferences("game_prefs", 0)

    private val repository = RepositoryProvider.socialImpactRepository(application)

    private val _uiState = MutableStateFlow(SocialImpactUiState())
    val uiState: StateFlow<SocialImpactUiState> = _uiState.asStateFlow()

    private var currentUserId: Long? = null

    fun setUserId(userId: Long) {
        if (currentUserId == userId) return
        currentUserId = userId
        _uiState.value = SocialImpactUiState()
        loadGameCount()
        loadData()
    }

    private fun loadData() {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val communityImpact = withContext(Dispatchers.IO) { repository.getCommunityImpact() }
                val donations = withContext(Dispatchers.IO) { repository.getDonationHistory(userId) }
                val vouchers = withContext(Dispatchers.IO) { repository.getVouchers() }
                val userPoints = withContext(Dispatchers.IO) { repository.getUserPoints(userId) }
                val pointsHistory = withContext(Dispatchers.IO) { repository.getPointsHistory(userId) }
                _uiState.value = _uiState.value.copy(
                    peopleHelped = communityImpact?.peopleHelped ?: 0,
                    totalDonated = communityImpact?.totalDonated ?: "RM 0",
                    donationHistory = donations.ifEmpty { sampleDonations },
                    voucherList = vouchers.ifEmpty { sampleVouchers },
                    userPoints = userPoints?.points ?: 0,
                    pointsHistory = pointsHistory,
                    isLoading = false
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    donationHistory = sampleDonations,
                    voucherList = sampleVouchers,
                    isLoading = false
                )
            }
        }
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
            val newPoints = current.userPoints - cost
            _uiState.value = current.copy(
                userPoints = newPoints,
                pointsHistory = current.pointsHistory + PointsHistoryEntry(
                    source = "Redeemed ${voucher.title}",
                    points = -cost,
                    date = "Today",
                    isEarned = false
                )
            )
            val userId = currentUserId ?: return
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    repository.upsertUserPoints(userId, newPoints)
                    repository.addPointsHistory(PointsHistoryEntry(
                        userId = userId,
                        source = "Redeemed ${voucher.title}",
                        points = -cost,
                        date = "Today",
                        isEarned = false
                    ))
                }
            }
        }
    }

    fun earnPoints(source: String, points: Int) {
        val current = _uiState.value
        val newPoints = current.userPoints + points
        _uiState.value = current.copy(
            userPoints = newPoints,
            pointsHistory = current.pointsHistory + PointsHistoryEntry(
                source = source,
                points = points,
                date = "Today",
                isEarned = true
            )
        )
        val userId = currentUserId ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.upsertUserPoints(userId, newPoints)
                repository.addPointsHistory(PointsHistoryEntry(
                    userId = userId,
                    source = source,
                    points = points,
                    date = "Today",
                    isEarned = true
                ))
            }
        }
    }
}
