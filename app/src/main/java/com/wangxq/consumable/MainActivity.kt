package com.wangxq.consumable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.wangxq.consumable.ui.theme.ConsumableTheme
import com.wangxq.consumable.ui.MainScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConsumableTheme {
                MainScreen()
            }
        }
    }
}
