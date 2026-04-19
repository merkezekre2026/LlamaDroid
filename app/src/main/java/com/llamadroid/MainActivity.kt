package com.llamadroid

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.llamadroid.ui.navigation.LlamaDroidRoot
import com.llamadroid.ui.theme.LlamaDroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = (application as LlamaDroidApp).graph
        setContent {
            val settings by graph.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = com.llamadroid.domain.settings.InferenceSettings(),
            )
            if (settings.keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            LlamaDroidTheme(themeMode = settings.themeMode) {
                LlamaDroidRoot(graph = graph)
            }
        }
    }
}
