package com.hhldiniz.praondefoiomeudinheiro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hhldiniz.praondefoiomeudinheiro.presentation.navigation.AppNavigation
import com.hhldiniz.praondefoiomeudinheiro.presentation.theme.PraOndeFoiOMeuDinheiroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PraOndeFoiOMeuDinheiroTheme {
                AppNavigation()
            }
        }
    }
}
