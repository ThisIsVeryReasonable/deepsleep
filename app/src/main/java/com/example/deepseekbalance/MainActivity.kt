package com.example.deepseekbalance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.deepseekbalance.ui.screen.BalanceScreen
import com.example.deepseekbalance.ui.screen.KeyManagementScreen
import com.example.deepseekbalance.ui.theme.DeepSeekBalanceTheme
import com.example.deepseekbalance.ui.viewmodel.BalanceViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BalanceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DeepSeekBalanceTheme {
                var screen by rememberSaveable { mutableStateOf(Screen.BALANCE) }

                when (screen) {
                    Screen.BALANCE -> BalanceScreen(
                        viewModel = viewModel,
                        onManageKeys = { screen = Screen.KEY_MANAGEMENT }
                    )
                    Screen.KEY_MANAGEMENT -> KeyManagementScreen(
                        viewModel = viewModel,
                        onBack = { screen = Screen.BALANCE }
                    )
                }
            }
        }
    }

    private enum class Screen {
        BALANCE,
        KEY_MANAGEMENT
    }
}
