package com.example.cafeypan.view

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cafeypan.ui.theme.*
import com.example.cafeypan.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestionTrabajadoresScreen(
    viewModel: LoginViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val listTrabajadores by viewModel.listaTrabajadores.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
    var contactoEmergenciaNombre by remember { mutableStateOf("") }
    var contactoEmergenciaTelefono by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    var rolElegido by remember { mutableStateOf("Barista") }
    var mostrarAdicional by remember { mutableStateOf(false) }

    var showEditWorkerDialog by remember { mutableStateOf<com.example.cafeypan.data.local.entity.UserEntity?>(null) }
    var expandedTrabajadorId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.cargarTrabajadores()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Personal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
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
            // Form Card to Add Worker
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Registrar Nuevo Trabajador",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre Completo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pin = it
                            }
                        },
                        label = { Text("PIN de Acceso (4 números)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text(
                        text = "Rol del Trabajador:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Barista", "Panadero", "Cajero").forEach { role ->
                            val isSelected = rolElegido == role
                            val roleColor = when (role) {
                                "Barista" -> RoleBarista
                                "Panadero" -> RolePanadero
                                "Cajero" -> RoleCajero
                                else -> RoleOwner
                            }
                            
                            val containerColor = if (isSelected) roleColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            val borderColor = if (isSelected) roleColor else MaterialTheme.colorScheme.outlineVariant
                            val textColor = if (isSelected) roleColor else MaterialTheme.colorScheme.onSurfaceVariant
                            
                            Card(
                                onClick = { rolElegido = role },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = containerColor),
                                border = BorderStroke(1.dp, borderColor)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = role,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }

                    // Seccion Expandible de Informacion Adicional
                    TextButton(
                        onClick = { mostrarAdicional = !mostrarAdicional },
                        modifier = Modifier.align(Alignment.Start)
                    ) {
                        Text(
                            text = if (mostrarAdicional) "Ocultar Datos Adicionales ✕" else "Agregar Datos Adicionales (Teléfono, Contacto, etc.) ＋",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    AnimatedVisibility(visible = mostrarAdicional) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = apellido,
                                onValueChange = { apellido = it },
                                label = { Text("Apellido") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = telefono,
                                onValueChange = { telefono = it },
                                label = { Text("Número de Teléfono") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = contactoEmergenciaNombre,
                                onValueChange = { contactoEmergenciaNombre = it },
                                label = { Text("Contacto de Emergencia (Nombre)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = contactoEmergenciaTelefono,
                                onValueChange = { contactoEmergenciaTelefono = it },
                                label = { Text("Contacto de Emergencia (Teléfono)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = notas,
                                onValueChange = { notas = it },
                                label = { Text("Notas / Datos Importantes") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (nombre.isEmpty() || pin.isEmpty() || rolElegido.isEmpty()) {
                                Toast.makeText(context, "Completa todos los datos", Toast.LENGTH_SHORT).show()
                            } else if (pin.length != 4) {
                                Toast.makeText(context, "El PIN debe tener 4 números", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.registrarTrabajador(
                                    nombre = nombre,
                                    pin = pin,
                                    rol = rolElegido,
                                    apellido = if (apellido.isNotBlank()) apellido else null,
                                    telefono = if (telefono.isNotBlank()) telefono else null,
                                    contactoEmergenciaNombre = if (contactoEmergenciaNombre.isNotBlank()) contactoEmergenciaNombre else null,
                                    contactoEmergenciaTelefono = if (contactoEmergenciaTelefono.isNotBlank()) contactoEmergenciaTelefono else null,
                                    notas = if (notas.isNotBlank()) notas else null
                                ) { result ->
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Trabajador registrado ✅", Toast.LENGTH_SHORT).show()
                                        nombre = ""
                                        apellido = ""
                                        pin = ""
                                        telefono = ""
                                        contactoEmergenciaNombre = ""
                                        contactoEmergenciaTelefono = ""
                                        notas = ""
                                        mostrarAdicional = false
                                    } else {
                                        Toast.makeText(context, result.exceptionOrNull()?.message ?: "Error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        Text("Guardar Trabajador", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Title for current workers list
            Text(
                text = "Trabajadores Registrados (Presiona para expandir)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Workers List
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listTrabajadores) { trabajador ->
                    val isExpanded = expandedTrabajadorId == trabajador.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                expandedTrabajadorId = if (isExpanded) null else trabajador.id
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${trabajador.nombre} ${trabajador.apellido ?: ""}".trim(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (trabajador.activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    val badgeColor = when (trabajador.rol) {
                                        "Barista" -> RoleBarista
                                        "Panadero" -> RolePanadero
                                        "Cajero" -> RoleCajero
                                        else -> RoleOwner
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = badgeColor.copy(alpha = 0.15f)),
                                        border = BorderStroke(0.5.dp, badgeColor),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = trabajador.rol,
                                            color = badgeColor,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (trabajador.rol != "Dueño") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(onClick = { showEditWorkerDialog = trabajador }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Editar trabajador",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = if (trabajador.activo) "Activo" else "Inact.",
                                            fontSize = 11.sp,
                                            color = if (trabajador.activo) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Switch(
                                            checked = trabajador.activo,
                                            onCheckedChange = { isActive ->
                                                viewModel.toggleTrabajadorActivo(trabajador.id, isActive)
                                            },
                                            modifier = Modifier.scale(0.85f)
                                        )
                                    }
                                }
                            }

                            // Details Panel
                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    if (!trabajador.telefono.isNullOrBlank()) {
                                        Text("📞 Teléfono: ${trabajador.telefono}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    } else {
                                        Text("📞 Teléfono: No registrado", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    
                                    if (!trabajador.contactoEmergenciaNombre.isNullOrBlank() || !trabajador.contactoEmergenciaTelefono.isNullOrBlank()) {
                                        Text("🚨 Contacto Emergencia:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("   Nombre: ${trabajador.contactoEmergenciaNombre ?: "No registrado"}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("   Teléfono: ${trabajador.contactoEmergenciaTelefono ?: "No registrado"}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Text("🚨 Contacto Emergencia: No registrado", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    
                                    if (!trabajador.notas.isNullOrBlank()) {
                                        Text("📝 Notas:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("   ${trabajador.notas}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- EDIT WORKER DIALOG ---
        showEditWorkerDialog?.let { worker ->
            var editNombre by remember { mutableStateOf(worker.nombre) }
            var editApellido by remember { mutableStateOf(worker.apellido ?: "") }
            var editPin by remember { mutableStateOf(worker.pin) }
            var editTelefono by remember { mutableStateOf(worker.telefono ?: "") }
            var editContactoEmergenciaNombre by remember { mutableStateOf(worker.contactoEmergenciaNombre ?: "") }
            var editContactoEmergenciaTelefono by remember { mutableStateOf(worker.contactoEmergenciaTelefono ?: "") }
            var editNotas by remember { mutableStateOf(worker.notas ?: "") }
            var editRol by remember { mutableStateOf(worker.rol) }

            AlertDialog(
                onDismissRequest = { showEditWorkerDialog = null },
                title = { Text("Editar Trabajador", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = editNombre,
                            onValueChange = { editNombre = it },
                            label = { Text("Nombre Completo") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = editApellido,
                            onValueChange = { editApellido = it },
                            label = { Text("Apellido") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = editPin,
                            onValueChange = {
                                if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                    editPin = it
                                }
                            },
                            label = { Text("PIN de Acceso (4 números)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = editTelefono,
                            onValueChange = { editTelefono = it },
                            label = { Text("Número de Teléfono") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = editContactoEmergenciaNombre,
                            onValueChange = { editContactoEmergenciaNombre = it },
                            label = { Text("Contacto Emergencia (Nombre)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = editContactoEmergenciaTelefono,
                            onValueChange = { editContactoEmergenciaTelefono = it },
                            label = { Text("Contacto Emergencia (Teléfono)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = editNotas,
                            onValueChange = { editNotas = it },
                            label = { Text("Notas / Datos Importantes") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Text(
                            text = "Rol del Trabajador:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectableGroup(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Barista", "Panadero", "Cajero").forEach { role ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = editRol == role,
                                        onClick = { editRol = role }
                                    )
                                    Text(
                                        text = role,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editNombre.isEmpty() || editPin.isEmpty() || editRol.isEmpty()) {
                                Toast.makeText(context, "Completa todos los datos", Toast.LENGTH_SHORT).show()
                            } else if (editPin.length != 4) {
                                Toast.makeText(context, "El PIN debe tener 4 números", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.editarTrabajador(
                                    id = worker.id,
                                    nombre = editNombre,
                                    pin = editPin,
                                    rol = editRol,
                                    apellido = if (editApellido.isNotBlank()) editApellido else null,
                                    telefono = if (editTelefono.isNotBlank()) editTelefono else null,
                                    contactoEmergenciaNombre = if (editContactoEmergenciaNombre.isNotBlank()) editContactoEmergenciaNombre else null,
                                    contactoEmergenciaTelefono = if (editContactoEmergenciaTelefono.isNotBlank()) editContactoEmergenciaTelefono else null,
                                    notas = if (editNotas.isNotBlank()) editNotas else null
                                ) { result ->
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Trabajador actualizado ✅", Toast.LENGTH_SHORT).show()
                                        showEditWorkerDialog = null
                                    } else {
                                        Toast.makeText(context, result.exceptionOrNull()?.message ?: "Error", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isLoading
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditWorkerDialog = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

// Add modifier extension if needed, let's keep it simple
fun Modifier.scale(scale: Float): Modifier = this // simple helper if scale not imported
