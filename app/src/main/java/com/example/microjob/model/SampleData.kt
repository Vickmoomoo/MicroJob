package com.example.microjob.model

import androidx.compose.ui.graphics.Color

/**
 * Fake data used to drive the UI until a real database is connected.
 * All repository access will later be swapped without touching the UI.
 */
object SampleData {

    val categories = listOf(
        Category(1, "Cleaning Housework", "🧹"),
        Category(2, "Delivery Courier", "🛵"),
        Category(3, "Digital Marketing", "📱"),
        Category(4, "Graphic Design", "🎨"),
    )

    val jobs = listOf(
        Job(
            id = 1,
            title = "Pet Bathing",
            price = 30.49,
            distanceKm = 6.0,
            category = "Cleaning Housework",
            location = "88, Jalan Batu Ferringhi, 11100 Batu Ferringhi, Pulau Pinang, Malaysia",
            description = "Looking for a gentle and friendly individual to help give our dog a complete bath and basic grooming. All cleaning supplies and tools are provided.",
            imageColor = 0xFF8D6E63
        ),
        Job(
            id = 2,
            title = "Kitchen Deep Cleaning",
            price = 60.99,
            distanceKm = 7.4,
            category = "Cleaning Housework",
            location = "12, Lorong Melayu, 10200 George Town, Pulau Pinang, Malaysia",
            description = "Need help with a thorough kitchen deep cleaning, including stove, cabinets and floor. Products will be provided.",
            imageColor = 0xFF546E7A
        ),
        Job(
            id = 3,
            title = "Food Delivery (Lunch)",
            price = 12.00,
            distanceKm = 2.3,
            category = "Delivery Courier",
            location = "Food Street Hawker Centre, 10400 George Town, Pulau Pinang, Malaysia",
            description = "Collect 3 lunch orders from the hawker centre and deliver to an office at Gurney Plaza. Reimbursement for travel is included.",
            imageColor = 0xFFF9A825
        ),
        Job(
            id = 4,
            title = "Social Media Post Design",
            price = 45.00,
            distanceKm = 0.0,
            category = "Digital Marketing",
            location = "Remote / Online",
            description = "Create 4 simple promotional posts for a local bakery. Can work from home, must be able to communicate in Bahasa Malaysia or English.",
            imageColor = 0xFF3949AB
        ),
        Job(
            id = 5,
            title = "Simple Logo Design",
            price = 80.00,
            distanceKm = 0.0,
            category = "Graphic Design",
            location = "Remote / Online",
            description = "Design a simple logo for a small printing shop. Deliver a high-res PNG and the source file.",
            imageColor = 0xFF00897B
        ),
        Job(
            id = 6,
            title = "Garden Weeding",
            price = 35.00,
            distanceKm = 5.1,
            category = "Cleaning Housework",
            location = "45, Jalan Sultan Ahmad Shah, 10050 George Town, Pulau Pinang, Malaysia",
            description = "Clear weeds from the front garden and trim the hedge. Gloves and tools will be provided.",
            imageColor = 0xFF7CB342
        ),
    )

    data class PromoBanner(
        val title: String,
        val subtitle: String,
        val startColor: Color,
        val endColor: Color,
    )

    val banners = listOf(
        PromoBanner(
            title = "NO POVERTY",
            subtitle = "BUILDING A FUTURE OF EQUALITY AND PROSPERITY",
            startColor = Color(0xFF1E88E5),
            endColor = Color(0xFF43A047),
        ),
        PromoBanner(
            title = "FREE COURSES",
            subtitle = "LEARN A SKILL, EARN A CERTIFICATE, GET HIRED",
            startColor = Color(0xFFF4511E),
            endColor = Color(0xFFFBC02D),
        ),
    )
}
