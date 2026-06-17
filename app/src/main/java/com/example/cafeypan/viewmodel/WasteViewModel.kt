package com.example.cafeypan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeypan.data.repository.WasteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WasteViewModel @Inject constructor(
    private val wasteRepository: WasteRepository
) : ViewModel() {

    private val _mermaSemanal = MutableStateFlow(0.0)
    val mermaSemanal: StateFlow<Double> = _mermaSemanal

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        cargarMermaSemanal()
    }

    fun cargarMermaSemanal() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val result = wasteRepository.fetchCostoMermaSemanal()
            if (result.isSuccess) {
                _mermaSemanal.value = result.getOrNull() ?: 0.0
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Error al obtener costo semanal"
            }
            _isLoading.value = false
        }
    }

    fun registrarMerma(
        product: String,
        quantity: Double,
        reason: String,
        cost: Double,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val hoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val result = wasteRepository.registrarMerma(product, quantity, reason, cost, hoy)
            if (result.isSuccess) {
                cargarMermaSemanal()
                onSuccess()
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Error al registrar merma"
            }
            _isLoading.value = false
        }
    }
}
