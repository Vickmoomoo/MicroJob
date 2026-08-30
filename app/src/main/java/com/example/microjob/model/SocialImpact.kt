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
    val brandColor: Color,
    val description: String = "",
    val rules: List<String> = emptyList()
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
        pointsRequired = 675,
        value = "RM 15",
        brandColor = Color(0xFFE4002B),
        description = "Get RM15 off your next KFC meal. Valid for any purchase above RM15 at participating outlets.",
        rules = listOf("Valid at all KFC outlets", "Minimum purchase RM15", "One-time use only", "Not valid with other promotions")
    ),
    VoucherItem(
        brand = "McDonald's",
        title = "McDonald's RM10 Voucher",
        validStores = "Valid at all McDonald's restaurants",
        pointsRequired = 450,
        value = "RM 10",
        brandColor = Color(0xFFFFC72C),
        description = "Enjoy RM10 off your McDonald's order. Valid for dine-in, take-away, and drive-through.",
        rules = listOf("Valid at all McDonald's outlets", "Minimum purchase RM10", "One-time use only", "Not valid for McDelivery")
    ),
    VoucherItem(
        brand = "Domino's",
        title = "Domino's RM20 Voucher",
        validStores = "Valid at all Domino's Pizza outlets",
        pointsRequired = 900,
        value = "RM 20",
        brandColor = Color(0xFF006491),
        description = "Save RM20 on your Domino's Pizza order. Perfect for sharing with friends and family.",
        rules = listOf("Valid at all Domino's outlets", "Minimum purchase RM20", "One-time use only", "Valid for pickup and delivery")
    ),
    VoucherItem(
        brand = "Pizza Hut",
        title = "Pizza Hut RM18 Voucher",
        validStores = "Valid at all Pizza Hut restaurants",
        pointsRequired = 810,
        value = "RM 18",
        brandColor = Color(0xFFE4002B),
        description = "Get RM18 off your Pizza Hut meal. Enjoy delicious pizza at a discounted price.",
        rules = listOf("Valid at all Pizza Hut outlets", "Minimum purchase RM18", "One-time use only", "Not valid with other promos")
    )
)
