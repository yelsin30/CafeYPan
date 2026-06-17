package com.example.cafeypan.view

import android.widget.Toast
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.app.AlarmManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.cafeypan.data.local.entity.TaskEntity
import com.example.cafeypan.ui.theme.*
import com.example.cafeypan.viewmodel.LoginViewModel
import com.example.cafeypan.viewmodel.TareaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    loginViewModel: LoginViewModel,
    tareaViewModel: TareaViewModel,
    wasteViewModel: com.example.cafeypan.viewmodel.WasteViewModel,
    onNavigateToPersonal: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onCheckGpsAndComplete: (id: Int) -> Unit,
    onConfigurarUbicacion: () -> Unit
) {
    val context = LocalContext.current
    val userName = loginViewModel.getLoggedUserName() ?: "Sin nombre"
    val userRol = loginViewModel.getLoggedUserRol() ?: "Worker"
    
    var showWasteDialog by remember { mutableStateOf(false) }
    var duracionTareaText by remember { mutableStateOf("") }
    var notasTareaText by remember { mutableStateOf("") }
    val userRole = com.example.cafeypan.model.UserRole.fromRolName(userRol)

    val isLoading by tareaViewModel.isLoading.collectAsState()
    val errorMsg by tareaViewModel.errorMessage.collectAsState()
    val syncStatus by tareaViewModel.syncStatus.collectAsState()
    val showingTodayOnly by tareaViewModel.isShowingTodayOnly.collectAsState()
    val listaTareas by tareaViewModel.listaTareas.collectAsState()

    val loggedUserId = loginViewModel.getLoggedUserId()
    val listaTrabajadores by loginViewModel.listaTrabajadores.collectAsState()

    var showPinDialog by remember { mutableStateOf(false) }
    var pinActual by remember { mutableStateOf("") }
    var pinNuevo by remember { mutableStateOf("") }

    var nuevaTareaText by remember { mutableStateOf("") }
    var rolDestinatario by remember { mutableStateOf("Barista") }
    var trabajadorAsignado by remember { mutableStateOf<com.example.cafeypan.data.local.entity.UserEntity?>(null) }
    var rolDropdownExpanded by remember { mutableStateOf(false) }
    var trabajadorDropdownExpanded by remember { mutableStateOf(false) }
    var fechaSeleccionada by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var fechaLimiteSeleccionada by remember { mutableStateOf("") }
    var showDeadlinePicker by remember { mutableStateOf(false) }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var expandAddTaskPanel by remember { mutableStateOf(false) }

    val keepScreenOnEnabled by loginViewModel.keepScreenOn.collectAsState()
    val biometricEnabled by loginViewModel.biometricEnabled.collectAsState()
    val themePreference by loginViewModel.themePreference.collectAsState()

    DisposableEffect(keepScreenOnEnabled) {
        var currentContext = context
        var activity: android.app.Activity? = null
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is android.app.Activity) {
                activity = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }
        if (keepScreenOnEnabled) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    var showCompleteConfirmDialog by remember { mutableStateOf<TaskEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditDialog by remember { mutableStateOf<TaskEntity?>(null) }
    var editDescripcion by remember { mutableStateOf("") }
    var editRol by remember { mutableStateOf("Barista") }
    var editTrabajadorAsignado by remember { mutableStateOf<com.example.cafeypan.data.local.entity.UserEntity?>(null) }
    var editFecha by remember { mutableStateOf("") }
    var editFechaLimite by remember { mutableStateOf<String?>(null) }
    var editNotas by remember { mutableStateOf("") }
    var editRolDropdownExpanded by remember { mutableStateOf(false) }
    var editTrabajadorDropdownExpanded by remember { mutableStateOf(false) }
    var showEditDatePicker by remember { mutableStateOf(false) }
    var showEditDeadlinePicker by remember { mutableStateOf(false) }

    var filtroSoloMisTareas by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loginViewModel.cargarTrabajadores()
    }

    val listaTareasFiltradas = remember(listaTareas, userRol, loggedUserId, filtroSoloMisTareas) {
        if (userRole.canManageWorkers) {
            listaTareas
        } else {
            listaTareas.filter { tarea ->
                val coincideRol = tarea.rol.equals(userRol, ignoreCase = true)
                val coincideAsignado = tarea.asignadoAId == loggedUserId
                if (filtroSoloMisTareas) {
                    coincideAsignado
                } else {
                    coincideRol || coincideAsignado
                }
            }
        }
    }

    val totalPendientes = listaTareasFiltradas.count { it.estado == "Pendiente" }
    val totalCompletadas = listaTareasFiltradas.count { it.estado == "Completada" }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showWasteDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Registrar Merma"
                    )
                }
            },
            topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = "Café & Pan", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
                        Text(text = "$userName ($userRol)", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.74f), fontSize = 12.sp)
                    }
                },
                actions = {
                    if (userRol == "Dueño") {
                        IconButton(onClick = { showPinDialog = true }) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = "Cambiar PIN", tint = Color.White)
                        }
                    }

                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Ajustes", tint = Color.White)
                    }

                    // Sync status icon
                    val syncIcon = when (syncStatus) {
                        "sincronizado" -> Icons.Default.CheckCircle
                        "pendiente" -> Icons.Default.Refresh
                        else -> Icons.Default.Warning
                    }
                    val syncTint = when (syncStatus) {
                        "sincronizado" -> Color.Green
                        "pendiente" -> Color.Yellow
                        else -> Color.Red
                    }
                    Icon(
                        imageVector = syncIcon,
                        contentDescription = "Sincronización: $syncStatus",
                        tint = syncTint,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    IconButton(onClick = {
                        loginViewModel.logout()
                        onNavigateToLogin()
                    }) {
                        Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Salir", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats Counters Card
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape).padding(6.dp)
                        )
                        Column {
                            Text(text = "Pendientes", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text(text = totalPendientes.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF3BAE76),
                            modifier = Modifier.size(28.dp).background(Color(0xFF3BAE76).copy(alpha = 0.1f), CircleShape).padding(6.dp)
                        )
                        Column {
                            Text(text = "Completadas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text(text = totalCompletadas.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3BAE76))
                        }
                    }
                }
            }

            // Owner Actions: Go to Worker Management, Dashboard and Config GPS
            if (userRole.canManageWorkers) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToPersonal,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Personal", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onNavigateToDashboard,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dashboard", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onConfigurarUbicacion,
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fijar GPS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Add Task input Panel
            if (userRole.canCreateTasks) {
                if (!expandAddTaskPanel) {
                    Button(
                        onClick = { expandAddTaskPanel = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Asignar Nueva Tarea", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 440.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Asignar Nueva Tarea",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { expandAddTaskPanel = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = nuevaTareaText,
                                    onValueChange = { nuevaTareaText = it },
                                    label = { Text("Descripción de la tarea...") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Selector de Rol
                                    ExposedDropdownMenuBox(
                                        expanded = rolDropdownExpanded,
                                        onExpandedChange = { rolDropdownExpanded = it },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = rolDestinatario,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Rol") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rolDropdownExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = rolDropdownExpanded,
                                            onDismissRequest = { rolDropdownExpanded = false }
                                        ) {
                                            listOf("Barista", "Panadero", "Cajero", "Dueño").forEach { role ->
                                                DropdownMenuItem(
                                                    text = { Text(role) },
                                                    onClick = {
                                                        rolDestinatario = role
                                                        rolDropdownExpanded = false
                                                        if (trabajadorAsignado?.rol != role) {
                                                            trabajadorAsignado = null
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Selector de Trabajador
                                    val trabajadoresFiltrados = listaTrabajadores.filter { 
                                        it.activo && (it.rol == rolDestinatario)
                                    }

                                    ExposedDropdownMenuBox(
                                        expanded = trabajadorDropdownExpanded,
                                        onExpandedChange = { trabajadorDropdownExpanded = it },
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        OutlinedTextField(
                                            value = trabajadorAsignado?.nombre ?: "Cualquiera (Todos)",
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Asignar a") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = trabajadorDropdownExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = trabajadorDropdownExpanded,
                                            onDismissRequest = { trabajadorDropdownExpanded = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Cualquiera (Todos)") },
                                                onClick = {
                                                    trabajadorAsignado = null
                                                    trabajadorDropdownExpanded = false
                                                }
                                            )
                                            trabajadoresFiltrados.forEach { trabajador ->
                                                DropdownMenuItem(
                                                    text = { Text(trabajador.nombre) },
                                                    onClick = {
                                                        trabajadorAsignado = trabajador
                                                        trabajadorDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Selector de Fecha para la Tarea (Editable y Multi-Fecha)
                                    OutlinedTextField(
                                        value = fechaSeleccionada,
                                        onValueChange = { fechaSeleccionada = it },
                                        label = { Text("Fecha(s) de Asignación") },
                                        placeholder = { Text("ej. 2026-06-11, 2026-06-12") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        trailingIcon = {
                                            IconButton(onClick = { showDatePicker = true }) {
                                                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Seleccionar Fecha")
                                            }
                                        }
                                    )

                                    // Selector de Fecha Límite (Opcional)
                                    OutlinedTextField(
                                        value = fechaLimiteSeleccionada,
                                        onValueChange = { fechaLimiteSeleccionada = it },
                                        label = { Text("Fecha Límite (Opcional)") },
                                        placeholder = { Text("ej. YYYY-MM-DD") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        trailingIcon = {
                                            Row {
                                                if (fechaLimiteSeleccionada.isNotEmpty()) {
                                                    IconButton(onClick = { fechaLimiteSeleccionada = "" }) {
                                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                                                    }
                                                }
                                                IconButton(onClick = { showDeadlinePicker = true }) {
                                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Seleccionar Fecha Límite")
                                                }
                                            }
                                        }
                                    )
                                }

                                // Campo de Duración de la Tarea (Opcional)
                                OutlinedTextField(
                                    value = duracionTareaText,
                                    onValueChange = { duracionTareaText = it },
                                    label = { Text("Duración para temporizador (minutos - opcional)") },
                                    placeholder = { Text("ej. 15") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                // Campo de Notas de la Tarea (Opcional)
                                OutlinedTextField(
                                    value = notasTareaText,
                                    onValueChange = { notasTareaText = it },
                                    label = { Text("Notas / Especificaciones (opcional)") },
                                    placeholder = { Text("ej. Usar leche de soya, hornear a 180°C") },
                                    singleLine = false,
                                    maxLines = 3,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        expandAddTaskPanel = false
                                        nuevaTareaText = ""
                                        notasTareaText = ""
                                        trabajadorAsignado = null
                                        fechaSeleccionada = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                        fechaLimiteSeleccionada = ""
                                        duracionTareaText = ""
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cancelar", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (nuevaTareaText.isNotEmpty()) {
                                             val duracion = duracionTareaText.toIntOrNull()
                                             tareaViewModel.agregarTarea(
                                                 descripcion = nuevaTareaText,
                                                 fecha = fechaSeleccionada,
                                                 rol = rolDestinatario,
                                                 asignadoAId = trabajadorAsignado?.id,
                                                 asignadoANombre = trabajadorAsignado?.nombre,
                                                 fechaLimite = if (fechaLimiteSeleccionada.trim().isEmpty()) null else fechaLimiteSeleccionada.trim(),
                                                 duracionMinutos = duracion,
                                                 notas = if (notasTareaText.trim().isEmpty()) null else notasTareaText.trim()
                                             )
                                             nuevaTareaText = ""
                                             notasTareaText = ""
                                             trabajadorAsignado = null
                                             fechaSeleccionada = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                             fechaLimiteSeleccionada = ""
                                             duracionTareaText = ""
                                             expandAddTaskPanel = false
                                        } else {
                                             Toast.makeText(context, "Escribe una tarea primero", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Guardar y Asignar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Filters Tabs Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { tareaViewModel.cargarTareas() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showingTodayOnly) MaterialTheme.colorScheme.primary else Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    elevation = null,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "Hoy",
                        color = if (showingTodayOnly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = { tareaViewModel.cargarHistorial() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!showingTodayOnly) MaterialTheme.colorScheme.primary else Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    elevation = null,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "Historial",
                        color = if (!showingTodayOnly) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Worker filters (Only for non-owners)
            if (!userRole.canManageWorkers) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !filtroSoloMisTareas,
                        onClick = { filtroSoloMisTareas = false },
                        label = { Text("Mi rol y mis tareas") },
                        leadingIcon = if (!filtroSoloMisTareas) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        shape = RoundedCornerShape(10.dp)
                    )

                    FilterChip(
                        selected = filtroSoloMisTareas,
                        onClick = { filtroSoloMisTareas = true },
                        label = { Text("Solo asignadas a mí") },
                        leadingIcon = if (filtroSoloMisTareas) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Error display
            errorMsg?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }

            // Tasks List
            if (listaTareasFiltradas.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay tareas para mostrar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listaTareasFiltradas, key = { it.id }) { tarea ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                when (dismissValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        if (tarea.estado == "Pendiente" || tarea.estado == "Atrasada") {
                                            onCheckGpsAndComplete(tarea.id)
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showConfetti = true
                                        }
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        if (userRole.canDeleteTasks) {
                                            showDeleteConfirmDialog = tarea
                                        }
                                        false
                                    }
                                    else -> false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = tarea.estado == "Pendiente" || tarea.estado == "Atrasada",
                            enableDismissFromEndToStart = userRole.canDeleteTasks,
                            backgroundContent = {
                                val direction = dismissState.dismissDirection
                                val color = when (direction) {
                                    SwipeToDismissBoxValue.StartToEnd -> Color(0xFF4CAF50).copy(alpha = 0.85f)
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                    else -> Color.Transparent
                                }
                                val alignment = when (direction) {
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                    else -> Alignment.Center
                                }
                                val icon = when (direction) {
                                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Check
                                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                    else -> null
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, shape = RoundedCornerShape(16.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = alignment
                                ) {
                                    icon?.let {
                                        Icon(
                                            imageVector = it,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            },
                            content = {
                                 TareaItemRow(
                                     tarea = tarea,
                                     userRole = userRole,
                                     userRol = userRol,
                                     listaTrabajadores = listaTrabajadores,
                                     onCompleteClick = { showCompleteConfirmDialog = tarea },
                                     onEditClick = {
                                         showEditDialog = tarea
                                         editDescripcion = tarea.descripcion
                                         editRol = tarea.rol
                                         editTrabajadorAsignado = listaTrabajadores.find { it.id == tarea.asignadoAId }
                                         editFecha = tarea.fecha
                                         editFechaLimite = tarea.fechaLimite
                                         editNotas = tarea.notas ?: ""
                                     },
                                     onDeleteClick = { showDeleteConfirmDialog = tarea }
                                 )
                            }
                        )
                    }
                }
            }
        }
    }

    // --- REGISTRAR MERMA DIALOG (ODS 12.3) ---
    if (showWasteDialog) {
        var selectedProduct by remember { mutableStateOf("Baguette") }
        var quantityInput by remember { mutableStateOf("") }
        var reasonInput by remember { mutableStateOf("Excedente") }
        var costInput by remember { mutableStateOf("") }
        var productDropdownExpanded by remember { mutableStateOf(false) }
        var reasonDropdownExpanded by remember { mutableStateOf(false) }

        val productPrices = mapOf(
            "Baguette" to 1.5,
            "Croissant" to 2.0,
            "Leche entera" to 1.0,
            "Café en grano" to 15.0
        )

        AlertDialog(
            onDismissRequest = { showWasteDialog = false },
            title = { Text("Registrar Merma 🗑️", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Reporta desperdicio de insumos para control de excedentes y sostenibilidad (ODS 12.3).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Producto selector
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedProduct,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Producto") },
                            modifier = Modifier.fillMaxWidth().clickable { productDropdownExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null) },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        DropdownMenu(
                            expanded = productDropdownExpanded,
                            onDismissRequest = { productDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            productPrices.keys.forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text(prod) },
                                    onClick = {
                                        selectedProduct = prod
                                        productDropdownExpanded = false
                                        val qty = quantityInput.toDoubleOrNull() ?: 0.0
                                        costInput = String.format(Locale.US, "%.2f", qty * productPrices[prod]!!)
                                    }
                                )
                            }
                        }
                    }

                    // Cantidad
                    OutlinedTextField(
                        value = quantityInput,
                        onValueChange = { valText ->
                            quantityInput = valText
                            val qty = valText.toDoubleOrNull() ?: 0.0
                            val price = productPrices[selectedProduct] ?: 0.0
                            costInput = String.format(Locale.US, "%.2f", qty * price)
                        },
                        label = { Text("Cantidad (unidades o kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Costo Estimado
                    OutlinedTextField(
                        value = costInput,
                        onValueChange = { costInput = it },
                        label = { Text("Costo Total (S/ - editable)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Motivo
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = reasonInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Motivo") },
                            modifier = Modifier.fillMaxWidth().clickable { reasonDropdownExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null) },
                            enabled = false,
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        DropdownMenu(
                            expanded = reasonDropdownExpanded,
                            onDismissRequest = { reasonDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            listOf("Excedente", "Quemado", "Caducado", "Dañado", "Otro").forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r) },
                                    onClick = {
                                        reasonInput = r
                                        reasonDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = quantityInput.toDoubleOrNull()
                        val costVal = costInput.toDoubleOrNull()
                        if (qty != null && qty > 0 && costVal != null && costVal >= 0) {
                            wasteViewModel.registrarMerma(
                                product = selectedProduct,
                                quantity = qty,
                                reason = reasonInput,
                                cost = costVal
                            ) {
                                Toast.makeText(context, "Merma registrada correctamente", Toast.LENGTH_SHORT).show()
                                showWasteDialog = false
                            }
                        } else {
                            Toast.makeText(context, "Por favor, ingresa cantidad y costo válidos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Registrar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showWasteDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- CHANGE PIN DIALOG ---
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Cambiar PIN de Acceso") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = pinActual,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) pinActual = it
                        },
                        label = { Text("PIN Actual") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = pinNuevo,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) pinNuevo = it
                        },
                        label = { Text("PIN Nuevo") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        loginViewModel.cambiarPin(pinActual, pinNuevo) { result ->
                            if (result.isSuccess) {
                                Toast.makeText(context, "PIN cambiado con éxito", Toast.LENGTH_SHORT).show()
                                showPinDialog = false
                                pinActual = ""
                                pinNuevo = ""
                            } else {
                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = pinActual.length == 4 && pinNuevo.length == 4
                ) {
                    Text("Cambiar PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; pinActual = ""; pinNuevo = "" }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- COMPLETE CONFIRMATION DIALOG (Triggers GPS validation) ---
    showCompleteConfirmDialog?.let { tarea ->
        AlertDialog(
            onDismissRequest = { showCompleteConfirmDialog = null },
            title = { Text("¿Tarea terminada?") },
            text = { Text("¿Estás seguro de marcar '${tarea.descripcion}' como completada?") },
            confirmButton = {
                Button(onClick = {
                    onCheckGpsAndComplete(tarea.id)
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showConfetti = true
                    showCompleteConfirmDialog = null
                }) {
                    Text("Sí, completar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirmDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- EDIT DIALOG ---
    showEditDialog?.let { tarea ->
        AlertDialog(
            onDismissRequest = { showEditDialog = null },
            title = { Text("Editar Tarea", fontWeight = FontWeight.Bold, color = CafePrimary) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editDescripcion,
                        onValueChange = { editDescripcion = it },
                        label = { Text("Descripción") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Selector de Rol
                    ExposedDropdownMenuBox(
                        expanded = editRolDropdownExpanded,
                        onExpandedChange = { editRolDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editRol,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Rol") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editRolDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = editRolDropdownExpanded,
                            onDismissRequest = { editRolDropdownExpanded = false }
                        ) {
                            listOf("Barista", "Panadero", "Cajero", "Dueño").forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) },
                                    onClick = {
                                        editRol = role
                                        editRolDropdownExpanded = false
                                        if (editTrabajadorAsignado?.rol != role) {
                                            editTrabajadorAsignado = null
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Selector de Trabajador
                    val trabajadoresFiltrados = listaTrabajadores.filter {
                        it.activo && (it.rol == editRol)
                    }

                    ExposedDropdownMenuBox(
                        expanded = editTrabajadorDropdownExpanded,
                        onExpandedChange = { editTrabajadorDropdownExpanded = it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editTrabajadorAsignado?.nombre ?: "Cualquiera (Todos)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Asignar a") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editTrabajadorDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = editTrabajadorDropdownExpanded,
                            onDismissRequest = { editTrabajadorDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Cualquiera (Todos)") },
                                onClick = {
                                    editTrabajadorAsignado = null
                                    editTrabajadorDropdownExpanded = false
                                }
                            )
                            trabajadoresFiltrados.forEach { trabajador ->
                                DropdownMenuItem(
                                    text = { Text(trabajador.nombre) },
                                    onClick = {
                                        editTrabajadorAsignado = trabajador
                                        editTrabajadorDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Selector de Fecha
                    OutlinedTextField(
                        value = editFecha,
                        onValueChange = { editFecha = it },
                        label = { Text("Fecha de Asignación") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            IconButton(onClick = { showEditDatePicker = true }) {
                                Icon(imageVector = Icons.Default.DateRange, contentDescription = "Seleccionar Fecha")
                            }
                        }
                    )

                    // Selector de Fecha Límite
                    OutlinedTextField(
                        value = editFechaLimite ?: "",
                        onValueChange = { editFechaLimite = if (it.isEmpty()) null else it },
                        label = { Text("Fecha Límite (Opcional)") },
                        placeholder = { Text("ej. YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        trailingIcon = {
                            Row {
                                if (!editFechaLimite.isNullOrEmpty()) {
                                    IconButton(onClick = { editFechaLimite = null }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                                IconButton(onClick = { showEditDeadlinePicker = true }) {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Seleccionar Fecha Límite")
                                }
                            }
                        }
                    )

                    // Campo de Notas de la Tarea (Opcional)
                    OutlinedTextField(
                        value = editNotas,
                        onValueChange = { editNotas = it },
                        label = { Text("Notas / Especificaciones (opcional)") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (editDescripcion.isNotEmpty()) {
                        tareaViewModel.editarTarea(
                            id = tarea.id,
                            descripcion = editDescripcion,
                            rol = editRol,
                            asignadoAId = editTrabajadorAsignado?.id,
                            asignadoANombre = editTrabajadorAsignado?.nombre,
                            fecha = editFecha,
                            fechaLimite = if (editFechaLimite.isNullOrEmpty()) null else editFechaLimite,
                            notas = if (editNotas.trim().isEmpty()) null else editNotas.trim()
                        )
                        showEditDialog = null
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // --- DELETE CONFIRMATION DIALOG ---
    showDeleteConfirmDialog?.let { tarea ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("¿Eliminar tarea?") },
            text = { Text("¿Estás seguro de borrar '${tarea.descripcion}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        tareaViewModel.eliminarTarea(tarea.id)
                        showDeleteConfirmDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            val selectedDate = sdf.format(Date(millis))
                            if (fechaSeleccionada.trim().isEmpty()) {
                                fechaSeleccionada = selectedDate
                            } else {
                                fechaSeleccionada = fechaSeleccionada.trim().removeSuffix(",") + ", " + selectedDate
                            }
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeadlinePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDeadlinePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            fechaLimiteSeleccionada = sdf.format(Date(millis))
                        }
                        showDeadlinePicker = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlinePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEditDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEditDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            editFecha = sdf.format(Date(millis))
                        }
                        showEditDatePicker = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEditDeadlinePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showEditDeadlinePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            editFechaLimite = sdf.format(Date(millis))
                        }
                        showEditDeadlinePicker = false
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDeadlinePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Ajustes de la Aplicación", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Keep Screen On
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Manos Ocupadas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Mantiene la pantalla encendida mientras usas la app.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = keepScreenOnEnabled,
                            onCheckedChange = { loginViewModel.setKeepScreenOnEnabled(it) }
                        )
                    }

                    Divider()

                    // 2. Biometrics (Acceso Rápido)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Acceso Biométrico", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Permite ingresar rápidamente usando tu huella dactilar.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { loginViewModel.setBiometricEnabled(it) }
                        )
                    }

                    Divider()

                    // 3. Dark Mode Theme
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Tema de la Aplicación", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("system" to "Sistema", "light" to "Claro", "dark" to "Oscuro").forEach { (value, label) ->
                                OutlinedButton(
                                    onClick = { loginViewModel.setThemePreference(value) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (themePreference == value) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (themePreference == value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (themePreference == value) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
    } // Cierre del Box principal

    ConfettiEffect(
        trigger = showConfetti,
        onFinished = { showConfetti = false }
    )
}

@Composable
fun ConfettiEffect(
    trigger: Boolean,
    onFinished: () -> Unit
) {
    if (!trigger) return

    val colors = listOf(
        Color(0xFFFFC107), // Oro
        Color(0xFFFF5722), // Naranja
        Color(0xFF4CAF50), // Verde
        Color(0xFF2196F3), // Azul
        Color(0xFFE91E63), // Rosa
        Color(0xFF9C27B0)  // Púrpura
    )

    // Lista de partículas con posiciones, velocidades y ángulos
    val particles = remember(trigger) {
        List(100) {
            ConfettiParticle(
                x = (50..950).random().toFloat(),
                y = -50f,
                speedY = (6..18).random().toFloat(),
                speedX = (-4..4).random().toFloat(),
                rotationSpeed = (-8..8).random().toFloat(),
                size = (8..20).random().dp,
                color = colors.random()
            )
        }
    }

    // Animador de progreso (de 0f a 1f)
    val progress = remember(trigger) { Animatable(0f) }

    LaunchedEffect(key1 = trigger) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = LinearEasing)
        )
        onFinished()
    }

    val density = LocalDensity.current

    Canvas(modifier = Modifier.fillMaxSize()) {
        val pValue = progress.value
        particles.forEach { particle ->
            // La posición Y depende del progreso y su velocidad individual
            val currentY = particle.y + (particle.speedY * 80 * pValue)
            val currentX = particle.x + (particle.speedX * 15 * pValue)
            val currentRotation = particle.rotationSpeed * 360 * pValue

            if (currentY < size.height) {
                val sizePx = with(density) { particle.size.toPx() }
                
                withTransform({
                    rotate(currentRotation, pivot = androidx.compose.ui.geometry.Offset(currentX + sizePx/2, currentY + sizePx/2))
                }) {
                    if (particle.isCircle) {
                        drawCircle(
                            color = particle.color,
                            radius = sizePx / 2,
                            center = androidx.compose.ui.geometry.Offset(currentX, currentY)
                        )
                    } else {
                        drawRect(
                            color = particle.color,
                            topLeft = androidx.compose.ui.geometry.Offset(currentX, currentY),
                            size = androidx.compose.ui.geometry.Size(sizePx, sizePx / 2)
                        )
                    }
                }
            }
        }
    }
}

data class ConfettiParticle(
    val x: Float,
    val y: Float,
    val speedY: Float,
    val speedX: Float,
    val rotationSpeed: Float,
    val size: androidx.compose.ui.unit.Dp,
    val color: Color,
    val isCircle: Boolean = (0..1).random() == 0
)

@Composable
fun TareaItemRow(
    tarea: TaskEntity,
    userRole: com.example.cafeypan.model.UserRole,
    userRol: String,
    listaTrabajadores: List<com.example.cafeypan.data.local.entity.UserEntity>,
    onCompleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var timerRunning by remember { mutableStateOf(false) }
    var timeLeftSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timeLeftSeconds > 0) {
                kotlinx.coroutines.delay(1000L)
                timeLeftSeconds -= 1
            }
            timerRunning = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (tarea.estado == "Pendiente" || tarea.estado == "Atrasada") {
                    onCompleteClick()
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        val roleColor = when (tarea.rol) {
            "Barista" -> RoleBarista
            "Panadero" -> RolePanadero
            "Cajero" -> RoleCajero
            else -> RoleOwner
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Indicator Strip
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(roleColor)
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Task Description and Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tarea.descripcion,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (tarea.estado == "Completada") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )

                    if (!tarea.notas.isNullOrEmpty()) {
                        Text(
                            text = tarea.notas,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Task role badge color
                        Card(
                            colors = CardDefaults.cardColors(containerColor = roleColor.copy(alpha = 0.15f)),
                            border = BorderStroke(0.5.dp, roleColor),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = tarea.rol,
                                color = roleColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        tarea.asignadoANombre?.let { nombreAsignado ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = nombreAsignado,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        val statusColor = when (tarea.estado) {
                            "Completada" -> Color(0xFF3BAE76)
                            "Atrasada" -> Color(0xFFD32F2F)
                            else -> Color(0xFFF57C00)
                        }
                        Text(
                            text = tarea.estado,
                            fontSize = 11.sp,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (tarea.estado == "Completada" && tarea.fechaCompletado != null) {
                        val completadoPorText = if (!tarea.completadoPorNombre.isNullOrEmpty()) {
                            " por ${tarea.completadoPorNombre}"
                        } else {
                            ""
                        }
                        Text(
                            text = "Completado: ${tarea.fechaCompletado}$completadoPorText",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!tarea.fechaLimite.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Límite: ${tarea.fechaLimite}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // TIMER UI
                    if (tarea.durationMinutes != null && (tarea.estado == "Pendiente" || tarea.estado == "Atrasada")) {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (timerRunning) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                CircularProgressIndicator(
                                    progress = { timeLeftSeconds.toFloat() / (tarea.durationMinutes * 60f) },
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val minutesLeft = timeLeftSeconds / 60
                                val secondsLeft = timeLeftSeconds % 60
                                Text(
                                    text = String.format(Locale.getDefault(), "Listo en: %02d:%02d ⏱️", minutesLeft, secondsLeft),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    timeLeftSeconds = tarea.durationMinutes * 60
                                    timerRunning = true
                                    
                                    try {
                                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                        val alarmIntent = Intent(context, com.example.cafeypan.receiver.TemporizadorExpiradoReceiver::class.java).apply {
                                            putExtra("task_id", tarea.id)
                                            putExtra("task_desc", tarea.descripcion)
                                        }
                                        val pendingIntent = PendingIntent.getBroadcast(
                                            context,
                                            tarea.id + 10000,
                                            alarmIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                        alarmManager.setExactAndAllowWhileIdle(
                                            AlarmManager.RTC_WAKEUP,
                                            System.currentTimeMillis() + tarea.durationMinutes * 60 * 1000L,
                                            pendingIntent
                                        )
                                        Toast.makeText(context, "Temporizador de ${tarea.durationMinutes} min iniciado", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier.padding(top = 4.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Iniciar (${tarea.durationMinutes} min)", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Click Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (userRole.canEditTasks && (tarea.estado == "Pendiente" || tarea.estado == "Atrasada")) {
                        IconButton(onClick = onEditClick) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = CafePrimary)
                        }
                    }

                    if (userRole.canDeleteTasks) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
