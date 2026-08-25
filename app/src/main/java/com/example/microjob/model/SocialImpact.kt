package com.example.microjob.model

import androidx.compose.ui.graphics.Color

data class DonationRecord(
    val organization: String,
    val date: String,
    val amount: String
)

data class VoucherItem(
    val brand: String,
    val title: String,
    val validStores: String,
    val pointsRequired: Int,
    val value: String,
    val brandColor: Color
)

data class PointsHistoryEntry(
    val source: String,
    val points: Int,
    val date: String,
    val isEarned: Boolean
)

val sampleDonations = listOf(
    DonationRecord("Penang Food Aid Foundation", "12 Aug 2026", "RM 500"),
    DonationRecord("Children Education Fund", "03 Aug 2026", "RM 300"),
    DonationRecord("Flood Relief Community", "15 Jul 2026", "RM 1,000"),
    DonationRecord("Old Folks Home Support", "28 Jun 2026", "RM 200")
)

val sampleVouchers = listOf(
    VoucherItem(
        brand = "KFC",
        title = "KFC RM15 Voucher",
        validStores = "Valid at all KFC outlets nationwide",
        pointsRequired = 700,
        value = "RM 15",
        brandColor = Color(0xFFE4002B)
    ),
    VoucherItem(
        brand = "McDonald's",
        title = "McDonald's RM10 Voucher",
        validStores = "Valid at all McDonald's restaurants",
        pointsRequired = 450,
        value = "RM 10",
        brandColor = Color(0xFFFFC72C)
    ),
    VoucherItem(
        brand = "Domino's",
        title = "Domino's RM20 Voucher",
        validStores = "Valid at all Domino's Pizza outlets",
        pointsRequired = 900,
        value = "RM 20",
        brandColor = Color(0xFF006491)
    ),
    VoucherItem(
        brand = "Pizza Hut",
        title = "Pizza Hut RM18 Voucher",
        validStores = "Valid at all Pizza Hut restaurants",
        pointsRequired = 800,
        value = "RM 18",
        brandColor = Color(0xFFE4002B)
    )
)
