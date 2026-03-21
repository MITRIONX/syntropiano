package de.syntrosoft.syntropiano.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import de.syntrosoft.syntropiano.ui.screens.home.HomeScreen
import de.syntrosoft.syntropiano.ui.screens.learn.LearnScreen
import de.syntrosoft.syntropiano.ui.screens.play.PlayScreen
import de.syntrosoft.syntropiano.ui.screens.play.SongListScreen
import de.syntrosoft.syntropiano.ui.screens.profile.ProfileScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToLearn = { navController.navigate(Screen.Learn.route) },
                onNavigateToSongList = { navController.navigate(Screen.SongList.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
            )
        }

        composable(Screen.Learn.route) {
            LearnScreen(
                onBack = { navController.popBackStack() },
                onLevelSelected = { level ->
                    // Navigate to first lesson of this level — will be wired in Task 15
                },
            )
        }

        composable(Screen.SongList.route) {
            SongListScreen(
                onBack = { navController.popBackStack() },
                onSongSelected = { songId ->
                    navController.navigate(Screen.Play.createRoute(songId, "PRACTICE"))
                },
            )
        }

        composable(
            Screen.Play.route,
            arguments = listOf(
                navArgument("songId") { type = NavType.LongType },
                navArgument("mode") { type = NavType.StringType },
            ),
        ) {
            PlayScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
    }
}
