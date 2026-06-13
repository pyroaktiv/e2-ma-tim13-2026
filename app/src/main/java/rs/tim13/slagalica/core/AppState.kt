package rs.tim13.slagalica.core

/**
 * Tracks whether the app is currently in the foreground, so background events
 * (e.g. a finished partija or a game invite) can raise an OS notification only
 * when the user is not actively in the app.
 */
object AppState {
    @Volatile
    private var startedActivities = 0

    val isForeground: Boolean get() = startedActivities > 0

    fun onActivityStarted() { startedActivities++ }
    fun onActivityStopped() { if (startedActivities > 0) startedActivities-- }
}
