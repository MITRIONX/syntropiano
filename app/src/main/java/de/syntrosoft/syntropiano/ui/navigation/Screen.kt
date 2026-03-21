package de.syntrosoft.syntropiano.ui.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Learn : Screen("learn")
    data object Lesson : Screen("lesson/{lessonLevel}/{lessonOrder}") {
        fun createRoute(level: Int, order: Int) = "lesson/$level/$order"
    }
    data object SongList : Screen("songs")
    data object Play : Screen("play/{songId}/{mode}") {
        fun createRoute(songId: Long, mode: String) = "play/$songId/$mode"
    }
    data object Profile : Screen("profile")
}
