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
}
