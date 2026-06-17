package com.example.cafeypan.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.cafeypan.data.repository.UserRepository
import com.example.cafeypan.data.local.security.SessionManager
import com.example.cafeypan.model.RespuestaLogin
import com.example.cafeypan.model.Trabajador
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userRepository: UserRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk(relaxed = true)
        sessionManager = mockk(relaxed = true)

        // Default sessionManager configurations to avoid crashes during init
        every { sessionManager.isLockedOut() } returns false
        every { sessionManager.isKeepScreenOnEnabled() } returns false
        every { sessionManager.isBiometricEnabled() } returns false
        every { sessionManager.getThemePreference() } returns "system"

        viewModel = LoginViewModel(userRepository, sessionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `verificarPin - exitoso`() = runTest(testDispatcher) {
        val pin = "1234"
        val respuesta = RespuestaLogin(
            exito = true,
            mensaje = "Exito",
            datos = Trabajador(1, "Juan", "Barista", pin)
        )

        coEvery { userRepository.verificarPin(pin) } returns Result.success(respuesta)

        viewModel.verificarPin(pin)

        // Advance coroutine execution
        advanceUntilIdle()

        val result = viewModel.loginResult.value
        assertTrue(result != null && result.isSuccess)
        assertEquals("Exito", result?.getOrNull()?.mensaje)
    }

    @Test
    fun `verificarPin - fallido`() = runTest(testDispatcher) {
        val pin = "9999"
        val exception = Exception("PIN incorrecto")

        coEvery { userRepository.verificarPin(pin) } returns Result.failure(exception)

        viewModel.verificarPin(pin)

        advanceUntilIdle()

        val result = viewModel.loginResult.value
        assertTrue(result != null && result.isFailure)
        assertEquals("PIN incorrecto", result?.exceptionOrNull()?.message)
    }

    @Test
    fun `cambiarPin - PIN actual incorrecto`() = runTest(testDispatcher) {
        every { sessionManager.getUserPin() } returns "1234"

        var finalResult: Result<Boolean>? = null
        viewModel.cambiarPin("1111", "5678") { result ->
            finalResult = result
        }

        advanceUntilIdle()

        assertTrue(finalResult != null && finalResult!!.isFailure)
        assertEquals("El PIN actual es incorrecto.", finalResult?.exceptionOrNull()?.message)
    }

    @Test
    fun `cambiarPin - PIN nuevo no tiene 4 digitos`() = runTest(testDispatcher) {
        every { sessionManager.getUserPin() } returns "1234"

        var finalResult: Result<Boolean>? = null
        viewModel.cambiarPin("1234", "567") { result ->
            finalResult = result
        }

        advanceUntilIdle()

        assertTrue(finalResult != null && finalResult!!.isFailure)
        assertEquals("El PIN nuevo debe tener 4 números.", finalResult?.exceptionOrNull()?.message)
    }

    @Test
    fun `cambiarPin - exitoso`() = runTest(testDispatcher) {
        every { sessionManager.getUserPin() } returns "1234"
        coEvery { userRepository.cambiarPin("5678") } returns Result.success(true)

        var finalResult: Result<Boolean>? = null
        viewModel.cambiarPin("1234", "5678") { result ->
            finalResult = result
        }

        advanceUntilIdle()

        assertTrue(finalResult != null && finalResult!!.isSuccess)
        assertEquals(true, finalResult?.getOrNull())
    }
}
