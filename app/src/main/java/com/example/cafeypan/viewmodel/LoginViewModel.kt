package com.example.cafeypan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeypan.data.repository.UserRepository
import com.example.cafeypan.data.local.security.SessionManager
import com.example.cafeypan.model.RespuestaLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loginResult = MutableStateFlow<Result<RespuestaLogin>?>(null)
    val loginResult: StateFlow<Result<RespuestaLogin>?> = _loginResult.asStateFlow()

    private val _lockoutTimeRemaining = MutableStateFlow(0L)
    val lockoutTimeRemaining: StateFlow<Long> = _lockoutTimeRemaining.asStateFlow()

    private var timerJob: Job? = null

    init {
        checkLockout()
    }

    fun verificarPin(pin: String) {
        if (sessionManager.isLockedOut()) {
            checkLockout()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _loginResult.value = null
            val result = userRepository.verificarPin(pin)
            _isLoading.value = false
            _loginResult.value = result

            if (result.isFailure) {
                checkLockout()
            }
        }
    }

    fun checkLockout() {
        if (sessionManager.isLockedOut()) {
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (sessionManager.isLockedOut()) {
                    _lockoutTimeRemaining.value = sessionManager.getLockoutTimeRemaining()
                    delay(1000)
                }
                _lockoutTimeRemaining.value = 0L
            }
        } else {
            _lockoutTimeRemaining.value = 0L
        }
    }

    fun cambiarPin(pinActual: String, pinNuevo: String, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            val savedPin = sessionManager.getUserPin()
            if (savedPin != pinActual) {
                onResult(Result.failure(Exception("El PIN actual es incorrecto.")))
                return@launch
            }
            if (pinNuevo.length != 4 || !pinNuevo.all { it.isDigit() }) {
                onResult(Result.failure(Exception("El PIN nuevo debe tener 4 números.")))
                return@launch
            }
            val result = userRepository.cambiarPin(pinNuevo)
            onResult(result)
        }
    }

    fun isUserLoggedIn(): Boolean = userRepository.isUserLoggedIn()
    fun getLoggedUserRol(): String? = userRepository.getLoggedUserRol()
    fun getLoggedUserName(): String? = userRepository.getLoggedUserName()
    fun getLoggedUserId(): Int = userRepository.getLoggedUserId()
    fun logout() {
        userRepository.logout()
        _loginResult.value = null
    }

    private val _listaTrabajadores = MutableStateFlow<List<com.example.cafeypan.data.local.entity.UserEntity>>(emptyList())
    val listaTrabajadores: StateFlow<List<com.example.cafeypan.data.local.entity.UserEntity>> = _listaTrabajadores.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(sessionManager.isKeepScreenOnEnabled())
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(sessionManager.isBiometricEnabled())
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    private val _themePreference = MutableStateFlow(sessionManager.getThemePreference())
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    fun setKeepScreenOnEnabled(enabled: Boolean) {
        sessionManager.setKeepScreenOnEnabled(enabled)
        _keepScreenOn.value = enabled
    }

    fun setBiometricEnabled(enabled: Boolean) {
        sessionManager.setBiometricEnabled(enabled)
        _biometricEnabled.value = enabled
        if (enabled) {
            val currentPin = sessionManager.getUserPin()
            if (currentPin != null) {
                sessionManager.saveBiometricPin(currentPin)
            }
        } else {
            sessionManager.clearBiometricPin()
        }
    }

    fun getBiometricPin(): String? = sessionManager.getBiometricPin()

    fun setThemePreference(theme: String) {
        sessionManager.setThemePreference(theme)
        _themePreference.value = theme
    }

    fun cargarTrabajadores() {
        viewModelScope.launch {
            _listaTrabajadores.value = userRepository.getTrabajadores()
        }
    }

    fun registrarTrabajador(
        nombre: String,
        pin: String,
        rol: String,
        apellido: String? = null,
        telefono: String? = null,
        contactoEmergenciaNombre: String? = null,
        contactoEmergenciaTelefono: String? = null,
        notas: String? = null,
        onResult: (Result<Boolean>) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userRepository.registrarTrabajador(
                nombre = nombre,
                pin = pin,
                rol = rol,
                apellido = apellido,
                telefono = telefono,
                contactoEmergenciaNombre = contactoEmergenciaNombre,
                contactoEmergenciaTelefono = contactoEmergenciaTelefono,
                notas = notas
            )
            _isLoading.value = false
            onResult(result)
            cargarTrabajadores()
        }
    }

    fun editarTrabajador(
        id: Int,
        nombre: String,
        pin: String,
        rol: String,
        apellido: String? = null,
        telefono: String? = null,
        contactoEmergenciaNombre: String? = null,
        contactoEmergenciaTelefono: String? = null,
        notas: String? = null,
        onResult: (Result<Boolean>) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userRepository.editarTrabajador(
                id = id,
                nombre = nombre,
                pin = pin,
                rol = rol,
                apellido = apellido,
                telefono = telefono,
                contactoEmergenciaNombre = contactoEmergenciaNombre,
                contactoEmergenciaTelefono = contactoEmergenciaTelefono,
                notas = notas
            )
            _isLoading.value = false
            onResult(result)
            cargarTrabajadores()
        }
    }

    fun toggleTrabajadorActivo(id: Int, activo: Boolean) {
        viewModelScope.launch {
            userRepository.toggleTrabajadorActivo(id, activo)
            cargarTrabajadores()
        }
    }

    private val _onboardingStatusChecked = MutableStateFlow(false)
    val onboardingStatusChecked: StateFlow<Boolean> = _onboardingStatusChecked.asStateFlow()

    fun comprobarOnboardingServidor() {
        viewModelScope.launch {
            try {
                val trabajadores = userRepository.getTrabajadores()
                if (trabajadores.isNotEmpty()) {
                    sessionManager.completeOnboarding("Café & Pan")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _onboardingStatusChecked.value = true
            }
        }
    }

    fun isOnboardingCompleted(): Boolean {
        return sessionManager.isOnboardingCompleted()
    }

    fun registrarDueñoYCompletarOnboarding(
        nombreDueño: String,
        pinDueño: String,
        nombreLocal: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = userRepository.registrarTrabajador(nombreDueño, pinDueño, "Dueño")
            _isLoading.value = false
            if (result.isSuccess) {
                sessionManager.completeOnboarding(nombreLocal)
                onResult(Result.success(true))
            } else {
                onResult(Result.failure(result.exceptionOrNull() ?: Exception("Error al registrar dueño")))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

}