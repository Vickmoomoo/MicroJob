package com.example.microjob.ui.navigation

/** Central definition of every navigation route in the app. */
object MicroJobRoutes {
    const val HOME = "home"
    const val COURSE = "course"
    const val MESSAGES = "messages"
    const val PROFILE = "profile"
    const val POST_JOB = "post_job"

    /** Login / register page. */
    const val LOGIN = "login"

    /** Job detail page. Route pattern: "job/{jobId}" */
    const val JOB_DETAIL = "job/{jobId}"

    /** Builds the concrete route for a given job id. */
    fun jobDetail(jobId: Int) = "job/$jobId"
}
