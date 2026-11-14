package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppContainer
import com.example.data.configureAppCheck
import com.example.ui.navigation.EcoAppRoot
import com.example.ui.theme.EcoSolarTheme
import com.example.ui.viewmodel.AssistantViewModel
import com.example.ui.viewmodel.BillAiViewModel
import com.example.ui.viewmodel.EcoViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val container = AppContainer(applicationContext)
        val ecoViewModel = ViewModelProvider(
            this,
            EcoViewModel.factory(container.ecoRepository)
        )[EcoViewModel::class.java]
        val billAiViewModel = ViewModelProvider(
            this,
            BillAiViewModel.factory(container.aiRepository)
        )[BillAiViewModel::class.java]
        val assistantViewModel = ViewModelProvider(
            this,
            AssistantViewModel.factory(container.aiRepository, container.ecoRepository)
        )[AssistantViewModel::class.java]

        setContent {
            EcoSolarTheme {
                EcoAppRoot(
                    ecoViewModel = ecoViewModel,
                    billAiViewModel = billAiViewModel,
                    assistantViewModel = assistantViewModel
                )
            }
        }
    }
}
