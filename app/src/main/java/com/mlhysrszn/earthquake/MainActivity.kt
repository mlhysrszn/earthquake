package com.mlhysrszn.earthquake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mlhysrszn.earthquake.ui.navigation.EarthquakeNavigation
import com.mlhysrszn.earthquake.ui.theme.EarthquakeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EarthquakeTheme {
                EarthquakeNavigation(
                    initialEarthquakeId = intent.getStringExtra(EXTRA_EARTHQUAKE_ID),
                )
            }
        }
    }

    companion object {
        const val EXTRA_EARTHQUAKE_ID = "com.mlhysrszn.earthquake.extra.EARTHQUAKE_ID"
    }
}
