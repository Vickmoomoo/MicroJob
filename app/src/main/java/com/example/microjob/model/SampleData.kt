package com.example.microjob.model

import androidx.compose.ui.graphics.Color

/**
 * Fake data used to drive the UI until a real database is connected.
 * All repository access will later be swapped without touching the UI.
 */
object SampleData {

    /**
     * Security questions shown in the register form (used to reset a
     * forgotten password). The list is read each time the register screen
     * opens; it does not change when the app is restarted.
     */
    val securityQuestions = listOf(
        "What is your favourite food?",
        "What is the name of your first pet?",
        "What city were you born in?",
        "What is your favourite movie?",
        "What was your childhood nickname?",
        "What is your mother's maiden name?",
    )
    /** Fake users used to drive the poster row on the job detail page. */
    val users = listOf(
        User(id = 0, name = "MicroJob System", username = "system", password = "", email = "", bio = "Automated system messages and notifications."),
        User(id = 1, name = "Ahmad bin Ali", username = "ahmad", password = "1234", email = "ahmad@example.com", bio = "House owner in Batu Ferringhi, looking for helpers."),
        User(id = 2, name = "Siti Aminah", username = "siti", password = "1234", email = "siti@example.com", bio = "Freelance cleaner, available on weekends."),
        User(id = 3, name = "Wei Qi", username = "weiqi", password = "1234", email = "weiqi@example.com", bio = "Marketing student, can design social media posts."),
        User(id = 4, name = "Ravi Kumar", username = "ravi", password = "1234", email = "ravi@example.com", bio = "Courier rider, delivery anywhere in Penang island."),
    )

    val categories = listOf(
        Category(1, "Cleaning Housework", "🧹"),
        Category(2, "Delivery Courier", "🛵"),
        Category(3, "Digital Marketing", "📱"),
        Category(4, "Graphic Design", "🎨"),
        Category(5, "Gardening & Outdoor", "🌿"),
        Category(6, "Home Repairs", "🔧"),
        Category(7, "Moving & Heavy Lifting", "📦"),
        Category(8, "Tutoring & Lessons", "📚"),
        Category(9, "Event Help", "🎉"),
        Category(10, "Cooking & Catering", "🍳"),
        Category(11, "Photography & Video", "📸"),
        Category(12, "Pet Care", "🐾"),
        Category(13, "IT & Programming", "💻"),
        Category(14, "Assembly & Furniture", "🛠️"),
        Category(15, "Other", "📌"),
    )

    val jobs = listOf<Job>(
    )

    data class PromoBanner(
        val title: String,
        val subtitle: String,
        val startColor: Color,
        val endColor: Color,
    )

    val banners = listOf(
        PromoBanner(
            title = "MICROJOB",
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
