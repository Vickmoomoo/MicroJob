package com.example.microjob.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.microjob.model.DonationRecord
import com.example.microjob.model.VoucherItem
import com.example.microjob.model.sampleDonations
import com.example.microjob.model.sampleVouchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SocialImpactUiState(
    val userPoints: Int = 2450,
    val peopleHelped: Int = 128,
    val totalDonated: String = "RM 15,680",
    val donationHistory: List<DonationRecord> = sampleDonations,
    val voucherList: List<VoucherItem> = sampleVouchers,
)

class SocialImpactViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SocialImpactUiState())
    val uiState: StateFlow<SocialImpactUiState> = _uiState.asStateFlow()

    fun redeemVoucher(voucher: VoucherItem) {
        val current = _uiState.value
        val cost = voucher.pointsRequired
        if (current.userPoints >= cost) {
            _uiState.value = current.copy(userPoints = current.userPoints - cost)
        }
    }
}
