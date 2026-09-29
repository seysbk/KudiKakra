package com.kudikakra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.kudikakra.ui.KudiKakraApp
import com.kudikakra.ui.theme.KudiKakraTheme
import com.kudikakra.ui.viewmodel.TransactionViewModel
import com.kudikakra.ui.viewmodel.SpendingPlanViewModel
import com.kudikakra.ui.viewmodel.UserPreferencesViewModel

class MainActivity : ComponentActivity() {
    private val transactionViewModel by viewModels<TransactionViewModel>()
    private val spendingPlanViewModel by viewModels<SpendingPlanViewModel>()
    private val userPreferencesViewModel by viewModels<UserPreferencesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KudiKakraTheme {
                KudiKakraApp(
                    transactionViewModel = transactionViewModel,
                    spendingPlanViewModel = spendingPlanViewModel,
                    userPreferencesViewModel = userPreferencesViewModel
                )
            }
        }
    }
}
