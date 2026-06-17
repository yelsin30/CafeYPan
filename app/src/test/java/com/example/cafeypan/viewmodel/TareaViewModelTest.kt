package com.example.cafeypan.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.cafeypan.data.local.entity.TaskEntity
import com.example.cafeypan.data.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TareaViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var taskRepository: TaskRepository
    private lateinit var viewModel: TareaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        taskRepository = mockk(relaxed = true)

        // Mock default flows and calls during init
        every { taskRepository.getLocalTasksFlow() } returns flowOf(emptyList())
        coEvery { taskRepository.verificarYGenerarTareasRecurrentes(any()) } returns Unit
        coEvery { taskRepository.fetchAndSyncTasks(any()) } returns Result.success(emptyList())

        viewModel = TareaViewModel(taskRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cargarTareas - exitoso`() = runTest(testDispatcher) {
        coEvery { taskRepository.fetchAndSyncTasks(any()) } returns Result.success(emptyList())

        viewModel.cargarTareas()
        advanceUntilIdle()

        assertEquals("sincronizado", viewModel.syncStatus.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `cargarTareas - fallido`() = runTest(testDispatcher) {
        coEvery { taskRepository.fetchAndSyncTasks(any()) } returns Result.failure(Exception("Error de red"))

        viewModel.cargarTareas()
        advanceUntilIdle()

        assertEquals("sin conexión", viewModel.syncStatus.value)
        assertEquals("Error de red", viewModel.errorMessage.value)
    }

    @Test
    fun `agregarTarea - exitoso`() = runTest(testDispatcher) {
        coEvery { taskRepository.agregarTarea(any(), any(), any(), any(), any()) } returns Result.success(true)

        viewModel.agregarTarea("Preparar pan", "2026-06-06", "Panadero")
        advanceUntilIdle()

        assertEquals("sincronizado", viewModel.syncStatus.value)
    }

    @Test
    fun `marcarComoCompletada - exitoso`() = runTest(testDispatcher) {
        coEvery { taskRepository.completarTarea(any(), any(), any()) } returns Result.success(true)

        viewModel.marcarComoCompletada(1, "Juan")
        advanceUntilIdle()

        assertEquals("sincronizado", viewModel.syncStatus.value)
    }
}
