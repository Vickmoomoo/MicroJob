package com.example.microjob.model

/**
 * Malaysian states and their common areas, used by the job poster to
 * select a location (state + area) instead of typing a free-form address,
 * and later by the location filter on the home screen.
 */
object MalaysianRegions {

    /** A state with its preset areas. */
    data class State(
        val name: String,
        val areas: List<String>
    )

    val states: List<State> = listOf(
        State("Johor", listOf("Johor Bahru", "Batu Pahat", "Muar", "Kluang", "Iskandar Puteri")),
        State("Kedah", listOf("Alor Setar", "Sungai Petani", "Kulim", "Langkawi")),
        State("Kelantan", listOf("Kota Bharu", "Pasir Mas", "Tumpat", "Machang")),
        State("Melaka", listOf("Bandar Melaka", "Ayer Keroh", "Jasin", "Alor Gajah")),
        State("Negeri Sembilan", listOf("Seremban", "Port Dickson", "Nilai", "Bahau")),
        State("Pahang", listOf("Kuantan", "Cameron Highlands", "Temerloh", "Genting Highlands")),
        State("Pulau Pinang", listOf("George Town", "Batu Ferringhi", "Tanjung Bungah", "Bayan Baru", "Bukit Mertajam")),
        State("Perak", listOf("Ipoh", "Taiping", "Teluk Intan", "Sitiawan")),
        State("Perlis", listOf("Kangar", "Arau", "Padang Besar")),
        State("Sabah", listOf("Kota Kinabalu", "Sandakan", "Tawau", "Keningau")),
        State("Sarawak", listOf("Kuching", "Miri", "Sibu", "Bintulu")),
        State("Selangor", listOf("Petaling Jaya", "Shah Alam", "Subang Jaya", "Klang", "Kajang", "Ampang")),
        State("Terengganu", listOf("Kuala Terengganu", "Kemaman", "Dungun", "Marang")),
        State("Kuala Lumpur", listOf("Bukit Bintang", "Cheras", "Kepong", "Bangsar", "KLCC")),
        State("Putrajaya", listOf("Precinct 1", "Precinct 9", "Precinct 16")),
        State("Labuan", listOf("Victoria", "Kiamsam", "Layang-Layangan")),
    )

    /** Names of every state, in order (for dropdowns / filters). */
    val stateNames: List<String> = states.map { it.name }

    /** Returns the areas of a state, or an empty list when the state is unknown. */
    fun areasOf(state: String): List<String> =
        states.firstOrNull { it.name == state }?.areas ?: emptyList()
}
