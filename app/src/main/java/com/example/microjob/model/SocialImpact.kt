package com.example.microjob.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun safeColor(hex: Long): Color {
    return try {
        val value = hex.toInt()
        if (value == 0) Color.Gray
        else Color(value or 0xFF000000.toInt())
    } catch (_: Exception) {
        Color.Gray
    }
}

@Serializable
data class CommunityImpact(
    val id: Long = 1,
    @SerialName("people_helped") val peopleHelped: Int = 0,
    @SerialName("total_donated") val totalDonated: String = "RM 0"
)

@Serializable
data class DonationRecord(
    val id: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    val organization: String = "",
    val date: String = "",
    val amount: String = ""
)

@Serializable
data class VoucherItem(
    val id: Int = 0,
    val brand: String = "",
    val title: String = "",
    @SerialName("valid_stores") val validStores: String = "",
    @SerialName("points_required") val pointsRequired: Int = 0,
    val value: String = "",
    @SerialName("brand_color") val brandColorHex: Long = 0,
    val description: String = "",
    val rules: List<String> = emptyList()
) {
    val brandColor: Color
        get() = safeColor(brandColorHex)
}

@Serializable
data class PointsHistoryEntry(
    val id: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    val source: String = "",
    val points: Int = 0,
    val date: String = "",
    @SerialName("is_earned") val isEarned: Boolean = true
)

@Serializable
data class UserPoints(
    val id: Long = 0,
    @SerialName("user_id") val userId: Long = 0,
    val points: Int = 0
)

val sampleDonations = listOf(
    DonationRecord(organization = "Penang Food Aid Foundation", date = "12 Aug 2026", amount = "RM 500"),
    DonationRecord(organization = "Children Education Fund", date = "03 Aug 2026", amount = "RM 300"),
    DonationRecord(organization = "Flood Relief Community", date = "15 Jul 2026", amount = "RM 1,000"),
    DonationRecord(organization = "Old Folks Home Support", date = "28 Jun 2026", amount = "RM 200")
)

val sampleVouchers = listOf(
    VoucherItem(
        brand = "KFC",
        title = "KFC RM15 Voucher",
        validStores = "Valid at all KFC outlets nationwide",
        pointsRequired = 675,
        value = "RM 15",
        brandColorHex = 0xFFE4002B,
        description = "Get RM15 off your next KFC meal. Valid for any purchase above RM15 at participating outlets.",
        rules = listOf("Valid at all KFC outlets", "Minimum purchase RM15", "One-time use only", "Not valid with other promotions")
    ),
    VoucherItem(
        brand = "McDonald's",
        title = "McDonald's RM10 Voucher",
        validStores = "Valid at all McDonald's restaurants",
        pointsRequired = 450,
        value = "RM 10",
        brandColorHex = 0xFFFFC72C,
        description = "Enjoy RM10 off your McDonald's order. Valid for dine-in, take-away, and drive-through.",
        rules = listOf("Valid at all McDonald's outlets", "Minimum purchase RM10", "One-time use only", "Not valid for McDelivery")
    ),
    VoucherItem(
        brand = "Domino's",
        title = "Domino's RM20 Voucher",
        validStores = "Valid at all Domino's Pizza outlets",
        pointsRequired = 900,
        value = "RM 20",
        brandColorHex = 0xFF006491,
        description = "Save RM20 on your Domino's Pizza order. Perfect for sharing with friends and family.",
        rules = listOf("Valid at all Domino's outlets", "Minimum purchase RM20", "One-time use only", "Valid for pickup and delivery")
    ),
    VoucherItem(
        brand = "Pizza Hut",
        title = "Pizza Hut RM18 Voucher",
        validStores = "Valid at all Pizza Hut restaurants",
        pointsRequired = 810,
        value = "RM 18",
        brandColorHex = 0xFFE4002B,
        description = "Get RM18 off your Pizza Hut meal. Enjoy delicious pizza at a discounted price.",
        rules = listOf("Valid at all Pizza Hut outlets", "Minimum purchase RM18", "One-time use only", "Not valid with other promos")
    )
)
