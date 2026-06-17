package com.example.cafeypan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cafeypan.data.local.entity.TaskEntity
import com.example.cafeypan.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TareaViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isShowingTodayOnly = MutableStateFlow(true)
    val isShowingTodayOnly: StateFlow<Boolean> = _isShowingTodayOnly.asStateFlow()

    // Status: "sincronizado", "pendiente", "sin conexión"
    private val _syncStatus = MutableStateFlow("sincronizado")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val formattedToday: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val listaTareas: StateFlow<List<TaskEntity>> = combine(
        _isShowingTodayOnly,
        taskRepository.getLocalTasksFlow()
    ) { showingTodayOnly, allTasks ->
        if (showingTodayOnly) {
            allTasks.filter { it.fecha == formattedToday }
        } else {
            allTasks
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todasLasTareas: StateFlow<List<TaskEntity>> = taskRepository.getLocalTasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        cargarTareas()
    }

    fun cargarTareas() {
        viewModelScope.launch {
            _isLoading.value = true
            _isShowingTodayOnly.value = true
            _errorMessage.value = null
            taskRepository.verificarYGenerarTareasRecurrentes(formattedToday)
            val result = taskRepository.fetchAndSyncTasks(formattedToday)
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al cargar tareas"
                _syncStatus.value = "sin conexión"
            } else {
                _syncStatus.value = "sincronizado"
            }
        }
    }

    fun cargarHistorial() {
        viewModelScope.launch {
            _isLoading.value = true
            _isShowingTodayOnly.value = false
            _errorMessage.value = null
            val result = taskRepository.fetchHistorial()
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Error al cargar historial"
                _syncStatus.value = "sin conexión"
            } else {
                _syncStatus.value = "sincronizado"
            }
        }
    }

    fun agregarTarea(
        descripcion: String,
        fecha: String,
        rol: String,
        asignadoAId: Int? = null,
        asignadoANombre: String? = null,
        fechaLimite: String? = null,
        duracionMinutos: Int? = null,
        notas: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _syncStatus.value = "pendiente"
            val result = taskRepository.agregarTarea(descripcion, fecha, rol, asignadoAId, asignadoANombre, fechaLimite, duracionMinutos, notas)
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message
                _syncStatus.value = "sin conexión"
            } else {
                _syncStatus.value = "sincronizado"
            }
        }
    }

    fun marcarComoCompletada(id: Int, completadoPorNombre: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _syncStatus.value = "pendiente"
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val fechaCompletado = format.format(Date())
            val result = taskRepository.completarTarea(id, fechaCompletado, completadoPorNombre)
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message
                _syncStatus.value = "sin conexión"
            } else {
                _syncStatus.value = "sincronizado"
            }
        }
    }

    fun editarTarea(
        id: Int,
        descripcion: String,
        rol: String?,
        asignadoAId: Int?,
        asignadoANombre: String?,
        fecha: String?,
        fechaLimite: String?,
        notas: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _syncStatus.value = "pendiente"
            val result = taskRepository.editarTarea(id, descripcion, rol, asignadoAId, asignadoANombre, fecha, fechaLimite, notas)
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message
                _syncStatus.value = "sin conexión"
            } else {
                _syncStatus.value = "sincronizado"
            }
        }
    }

    fun eliminarTarea(id: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _syncStatus.value = "pendiente"
            val result = taskRepository.eliminarTarea(id)
            _isLoading.value = false
            if (result.isFailure) {
                _errorMessage.value = result.exceptionOrNull()?.message
                _syncStatus.value = "sin conexión"
            } else {
                _syncStatus.value = "sincronizado"
            }
        }
    }
}