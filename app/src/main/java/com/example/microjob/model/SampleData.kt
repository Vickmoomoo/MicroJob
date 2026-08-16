package com.example.microjob.model

import androidx.compose.ui.graphics.Color

/**
 * Fake data used to drive the UI until a real database is connected.
 * All repository access will later be swapped without touching the UI.
 */
object SampleData {

    /** Fake users used to drive the poster row on the job detail page. */
    val users = listOf(
        User(id = 1, name = "Ahmad bin Ali", bio = "House owner in Batu Ferringhi, looking for helpers."),
        User(id = 2, name = "Siti Aminah", bio = "Freelance cleaner, available on weekends."),
        User(id = 3, name = "Wei Qi", bio = "Marketing student, can design social media posts."),
        User(id = 4, name = "Ravi Kumar", bio = "Courier rider, delivery anywhere in Penang island."),
    )

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
            category = "Cleaning Housework",
            state = "Pulau Pinang",
            area = "Batu Ferringhi",
            description = "Looking for a gentle and friendly individual to help give our dog a complete bath and basic grooming. All cleaning supplies and tools are provided.",
            imageColor = 0xFF8D6E63,
            posterId = 1,
            status = "OPEN",
            requireGps = true,
            toolsRequired = "None (supplies provided)",
            paymentMethod = "Cash",
            language = "English"
        ),
        Job(
            id = 2,
            title = "Kitchen Deep Cleaning",
            price = 60.99,
            category = "Cleaning Housework",
            state = "Pulau Pinang",
            area = "George Town",
            description = "Need help with a thorough kitchen deep cleaning, including stove, cabinets and floor. Products will be provided.",
            imageColor = 0xFF546E7A,
            posterId = 1,
            status = "IN_PROGRESS",
            toolsRequired = "Cleaning gloves",
            paymentMethod = "TNG eWallet",
            language = "Bahasa Malaysia"
        ),
        Job(
            id = 3,
            title = "Food Delivery (Lunch)",
            price = 12.00,
            category = "Delivery Courier",
            state = "Pulau Pinang",
            area = "George Town",
            description = "Collect 3 lunch orders from the hawker centre and deliver to an office at Gurney Plaza. Reimbursement for travel is included.",
            imageColor = 0xFFF9A825,
            posterId = 4,
            status = "OPEN",
            requireGps = true,
            paymentMethod = "Cash",
            language = "English"
        ),
        Job(
            id = 4,
            title = "Social Media Post Design",
            price = 45.00,
            category = "Digital Marketing",
            state = "Kuala Lumpur",
            area = "Bukit Bintang",
            description = "Create 4 simple promotional posts for a local bakery. Can work from home, must be able to communicate in Bahasa Malaysia or English.",
            imageColor = 0xFF3949AB,
            posterId = 3,
            status = "OPEN",
            paymentMethod = "Bank Transfer",
            language = "Chinese / English"
        ),
        Job(
            id = 5,
            title = "Simple Logo Design",
            price = 80.00,
            category = "Graphic Design",
            state = "Selangor",
            area = "Petaling Jaya",
            description = "Design a simple logo for a small printing shop. Deliver a high-res PNG and the source file.",
            imageColor = 0xFF00897B,
            posterId = 3,
            status = "OPEN",
            toolsRequired = "Adobe Illustrator",
            paymentMethod = "Online Banking",
            language = "English"
        ),
        Job(
            id = 6,
            title = "Garden Weeding",
            price = 35.00,
            category = "Cleaning Housework",
            state = "Pulau Pinang",
            area = "Tanjung Bungah",
            description = "Clear weeds from the front garden and trim the hedge. Gloves and tools will be provided.",
            imageColor = 0xFF7CB342,
            posterId = 2,
            status = "COMPLETED",
            requireGps = true,
            toolsRequired = "Garden gloves",
            paymentMethod = "Cash",
            paymentStatus = "RELEASED",
            language = "Bahasa Malaysia"
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
