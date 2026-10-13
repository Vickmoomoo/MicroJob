package com.example.microjob.model

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
            Course(1, "Basic Cleaning Techniques", "Housekeeping", "\uD83E\uDDF9", 8, "2h 30m", "Learn professional home cleaning methods.", enrolled = false, progress = 0),
            Course(2, "Kitchen Deep Cleaning", "Housekeeping", "\uD83E\uDDF9", 6, "2h", "Master kitchen deep cleaning techniques.", enrolled = false, progress = 0),
            Course(3, "Laundry & Ironing Basics", "Housekeeping", "\uD83E\uDDF9", 5, "1h 30m", "Proper laundry and ironing skills.", enrolled = false, progress = 0),
            Course(4, "Advanced Cleaning & Specialised Surface Care", "Housekeeping", "\uD83E\uDDF9", 10, "4h", "Advanced cleaning techniques for marble, wood, glass and other special surfaces.", enrolled = false, progress = 0),
            Course(5, "Professional Organising & Decluttering", "Housekeeping", "\uD83E\uDDF9", 8, "3h", "Professional tidying and space organisation methods (KonMari, etc).", enrolled = false, progress = 0)
        )
    ),
    CourseCategory(
        name = "Caregiving",
        emoji = "\uD83D\uDC76",
        courses = listOf(
            Course(6, "Elderly Care Fundamentals", "Caregiving", "\uD83D\uDC76", 10, "4h", "Essential skills for elderly care.", enrolled = false, progress = 0),
            Course(7, "Pet Grooming & Care", "Caregiving", "\uD83D\uDC3E", 7, "2h 45m", "How to groom and care for pets.", enrolled = false, progress = 0),
            Course(8, "Professional Confinement Nanny & Infant Care", "Caregiving", "\uD83D\uDC76", 12, "5h", "Newborn care, breastfeeding support, and confinement practices.", enrolled = false, progress = 0),
            Course(9, "Elderly & Dementia Care Certification", "Caregiving", "\uD83D\uDC76", 14, "6h", "Specialised care for elderly patients including dementia and Alzheimer's.", enrolled = false, progress = 0),
            Course(10, "First Aid & CPR Certification", "Caregiving", "\uD83C\uDFE5", 8, "3h", "Basic first aid, CPR, and emergency response certification.", enrolled = false, progress = 0),
            Course(11, "Food Safety & Hygiene Certification", "Caregiving", "\uD83E\uDDF0", 6, "2h", "Food handling, hygiene standards, and safety regulations.", enrolled = false, progress = 0)
        )
    ),
    CourseCategory(
        name = "Delivery & Transport",
        emoji = "\uD83D\uDEF5",
        courses = listOf(
            Course(12, "Food Delivery Safety", "Delivery", "\uD83D\uDEF5", 5, "1h 15m", "Safety guidelines for food delivery riders.", enrolled = false, progress = 0),
            Course(13, "Navigation & Route Planning", "Delivery", "\uD83D\uDDFA\uFE0F", 6, "2h", "Optimize your delivery routes.", enrolled = false, progress = 0)
        )
    ),
    CourseCategory(
        name = "Gardening",
        emoji = "\uD83C\uDF3F",
        courses = listOf(
            Course(14, "Garden Maintenance Basics", "Gardening", "\uD83C\uDF3F", 7, "2h 15m", "Maintain and beautify gardens.", enrolled = false, progress = 0),
            Course(15, "Indoor Plant Care", "Gardening", "\uD83C\uDF31", 5, "1h 45m", "Keep indoor plants healthy and thriving.", enrolled = false, progress = 0)
        )
    ),
    CourseCategory(
        name = "Digital Literacy & Applied Technology",
        emoji = "\uD83D\uDCBB",
        courses = listOf(
            Course(16, "Smart Home Operation & Troubleshooting", "Digital", "\uD83D\uDCBB", 8, "3h", "Operate and troubleshoot smart home devices like speakers, cameras, and appliances.", enrolled = false, progress = 0),
            Course(17, "Digital Bookkeeping & Management", "Digital", "\uD83D\uDCD3", 6, "2h 30m", "Use digital tools for expense tracking, invoicing, and financial management.", enrolled = false, progress = 0),
            Course(18, "Gig Platform Ordering & Operations", "Digital", "\uD83D\uDCF1", 7, "2h 45m", "How to accept, manage and complete orders on gig economy platforms.", enrolled = false, progress = 0)
        )
    ),
    CourseCategory(
        name = "Soft Skills & Professional Ethics",
        emoji = "\uD83D\uDCAC",
        courses = listOf(
            Course(19, "Customer Service Excellence", "Soft Skills", "\uD83D\uDCAC", 6, "2h", "Communicate professionally with clients.", enrolled = false, progress = 0),
            Course(20, "Workplace Communication & Etiquette", "Soft Skills", "\uD83D\uDCAC", 5, "1h 45m", "Professional communication and workplace manners.", enrolled = false, progress = 0),
            Course(21, "Time Management & Productivity", "Soft Skills", "\u23F0", 6, "2h", "Manage your time effectively and boost productivity.", enrolled = false, progress = 0),
            Course(22, "Basic Foreign Language & Dialect", "Soft Skills", "\uD83C\uDF0D", 10, "4h", "Learn essential phrases in English, Mandarin, Malay or other languages for daily work.", enrolled = false, progress = 0)
        )
    )
)

val sampleCertificates = emptyList<Certificate>()
