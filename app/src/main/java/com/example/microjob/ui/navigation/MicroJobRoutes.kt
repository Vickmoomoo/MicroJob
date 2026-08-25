package com.example.microjob.ui.navigation

/** Central definition of every navigation route in the app. */
object MicroJobRoutes {
    const val HOME = "home"
    const val COURSE = "course"
    const val MESSAGES = "messages"
    const val PROFILE = "profile"
    const val POST_JOB = "post_job"
    const val VOICE_TRANSLATION = "voice_translation"

    /** Login / register page. */
    const val LOGIN = "login"

    /** Jobs detail page. Route pattern: "job/{jobId}" */
    const val JOB_DETAIL = "job/{jobId}"

    /** Builds the concrete route for a given job id. */
    fun jobDetail(jobId: Int) = "job/$jobId"

    /**
     * Chat with one other user. Route pattern: "chat/{otherUserId}".
     * The chat is scoped to a single conversation partner.
     */
    const val CHAT_DETAIL = "chat/{otherUserId}"

    /** Builds the concrete chat route for a given other user id. */
    fun chatDetail(otherUserId: Long) = "chat/$otherUserId"

    /** User profile page. Route pattern: "user_profile/{userId}" */
    const val USER_PROFILE = "user_profile/{userId}"

    /** Builds the concrete route for a given user id. */
    fun userProfile(userId: Long) = "user_profile/$userId"

    /** Settings page. */
    const val SETTINGS = "settings"

    /** Review form page. Route pattern: "review_form/{reviewedUserId}/{jobId}" */
    const val REVIEW_FORM = "review_form/{reviewedUserId}/{jobId}"

    /** Builds the concrete route for writing a review. */
    fun reviewForm(reviewedUserId: Long, jobId: Int?) = "review_form/$reviewedUserId/${jobId ?: 0}"

    /** Edit review form. Route pattern: "review_form/{reviewedUserId}/{jobId}/{reviewId}" */
    const val REVIEW_FORM_EDIT = "review_form/{reviewedUserId}/{jobId}/{reviewId}"

    /** Builds the concrete route for editing a review. */
    fun reviewFormEdit(reviewedUserId: Long, jobId: Int?, reviewId: Long) =
        "review_form/$reviewedUserId/${jobId ?: 0}/$reviewId"

    /** Reviews list page. Route pattern: "reviews/{userId}" */
    const val REVIEWS_LIST = "reviews/{userId}"

    /** Builds the concrete route for viewing a user's reviews. */
    fun reviewsList(userId: Long) = "reviews/$userId"

    /** Posted jobs list. Route pattern: "posted_jobs/{userId}" */
    const val POSTED_JOBS = "posted_jobs/{userId}"

    /** Builds the concrete route for posted jobs. */
    fun postedJobs(userId: Long) = "posted_jobs/$userId"

    /** Accepted jobs list. Route pattern: "accepted_jobs/{userId}" */
    const val ACCEPTED_JOBS = "accepted_jobs/{userId}"

    /** Builds the concrete route for accepted jobs. */
    fun acceptedJobs(userId: Long) = "accepted_jobs/$userId"

    /** Combined My Jobs (posted + accepted tabs). Route pattern: "my_jobs/{userId}" */
    const val MY_JOBS = "my_jobs/{userId}"
    fun myJobs(userId: Long) = "my_jobs/$userId"

    /** My job detail with calendar. Route pattern: "my_job_detail/{jobId}" */
    const val MY_JOB_DETAIL = "my_job_detail/{jobId}"
    fun myJobDetail(jobId: Int) = "my_job_detail/$jobId"

    /** Review job detail (price/title/desc + reviews). Route pattern: "review_job/{jobId}" */
    const val REVIEW_JOB_DETAIL = "review_job/{jobId}"
    fun reviewJobDetail(jobId: Int) = "review_job/$jobId"

    /** Social Impact page. */
    const val SOCIAL_IMPACT = "social_impact"
}
