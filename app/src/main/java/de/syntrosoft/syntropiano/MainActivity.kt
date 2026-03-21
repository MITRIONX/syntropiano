package de.syntrosoft.syntropiano

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import de.syntrosoft.syntropiano.ui.navigation.NavGraph
import de.syntrosoft.syntropiano.ui.theme.SyntroPianoTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SyntroPianoTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
