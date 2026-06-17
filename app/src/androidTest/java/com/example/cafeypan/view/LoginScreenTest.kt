package com.example.cafeypan.view

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cafeypan.viewmodel.LoginViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var loginViewModel: LoginViewModel

    @Before
    fun setUp() {
        loginViewModel = mockk(relaxed = true)
        
        // Mock required VM state flows
        every { loginViewModel.isLoading } returns MutableStateFlow(false)
        every { loginViewModel.loginResult } returns MutableStateFlow(null)
        every { loginViewModel.lockoutTimeRemaining } returns MutableStateFlow(0L)
        every { loginViewModel.keepScreenOn } returns MutableStateFlow(false)
        every { loginViewModel.biometricEnabled } returns MutableStateFlow(false)
        every { loginViewModel.getBiometricPin() } returns null
    }

    @Test
    fun loginScreen_displaysTitleAndFields() {
        composeTestRule.setContent {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {},
                onTriggerBiometric = {}
            )
        }

        // Verify that the title and buttons are displayed correctly
        composeTestRule.onNodeWithText("Café & Pan").assertIsDisplayed()
        composeTestRule.onNodeWithText("Gestión de Personal y Tareas").assertIsDisplayed()
        composeTestRule.onNodeWithText("Introduce tu PIN de 4 números").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ingresar").assertIsDisplayed()
    }
}
