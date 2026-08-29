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

data class Course(
    val id: Int,
    val title: String,
    val category: String,
    val emoji: String,
    val lessons: Int,
    val duration: String,
    val description: String,
    val enrolled: Boolean = false,
    val progress: Int = 0
)

data class CourseCategory(
    val name: String,
    val emoji: String,
    val courses: List<Course>
)

data class Certificate(
    val id: Int,
    val courseTitle: String,
    val earnedDate: String,
    val credentialId: String
)

val sampleCourseCategories = listOf(
    CourseCategory(
        name = "Housekeeping",
        emoji = "\uD83E\uDDF9",
        courses = listOf(
            Course(1, "Basic Cleaning Techniques", "Housekeeping", "\uD83E\uDDF9", 8, "2h 30m", "Learn professional home cleaning methods.", enrolled = true, progress = 100),
            Course(2, "Kitchen Deep Cleaning", "Housekeeping", "\uD83E\uDDF9", 6, "2h", "Master kitchen deep cleaning techniques.", enrolled = true, progress = 45),
            Course(3, "Laundry & Ironing Basics", "Housekeeping", "\uD83E\uDDF9", 5, "1h 30m", "Proper laundry and ironing skills.", enrolled = false, progress = 0)
        )
    ),
    CourseCategory(
        name = "Caregiving",
        emoji = "\uD83D\uDC76",
        courses = listOf(
            Course(4, "Elderly Care Fundamentals", "Caregiving", "\uD83D\uDC76", 10, "4h", "Essential skills for elderly care.", enrolled = true, progress = 70),
            Course(5, "Pet Grooming & Care", "Caregiving", "\uD83D\uDC3E", 7, "2h 45m", "How to groom and care for pets.", enrolled = false, progress = 0),
            Course(6, "First Aid Essentials", "Caregiving", "\uD83C\uDFE5", 8, "3h", "Basic first aid and emergency response.", enrolled = false, progress = 0)
        )
    ),
    CourseCategory(
        name = "Delivery & Transport",
        emoji = "\uD83D\uDEF5",
        courses = listOf(
            Course(7, "Food Delivery Safety", "Delivery", "\uD83D\uDEF5", 5, "1h 15m", "Safety guidelines for food delivery riders.", enrolled = true, progress = 60),
            Course(8, "Navigation & Route Planning", "Delivery", "\uD83D\uDDFA\uFE0F", 6, "2h", "Optimize your delivery routes.", enrolled = false, progress = 0)
        )
    ),
    CourseCategory(
        name = "Gardening",
        emoji = "\uD83C\uDF3F",
        courses = listOf(
            Course(9, "Garden Maintenance Basics", "Gardening", "\uD83C\uDF3F", 7, "2h 15m", "Maintain and beautify gardens.", enrolled = false, progress = 0),
            Course(10, "Indoor Plant Care", "Gardening", "\uD83C\uDF31", 5, "1h 45m", "Keep indoor plants healthy and thriving.", enrolled = false, progress = 0)
        )
    )
)

val sampleCertificates = listOf(
    Certificate(1, "Basic Cleaning Techniques", "15 Aug 2026", "MJ-CLN-2026-001"),
    Certificate(2, "Food Delivery Safety", "01 Aug 2026", "MJ-DEL-2026-042")
)

val sampleVouchers = listOf(
    VoucherItem(
        brand = "KFC",
        title = "KFC RM15 Voucher",
        validStores = "Valid at all KFC outlets nationwide",
        pointsRequired = 675,
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
        pointsRequired = 810,
        value = "RM 18",
        brandColor = Color(0xFFE4002B)
    )
)
