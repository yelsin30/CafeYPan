package com.example.cafeypan.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cafeypan.ui.theme.*
import com.example.cafeypan.viewmodel.TareaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    tareaViewModel: TareaViewModel,
    wasteViewModel: com.example.cafeypan.viewmodel.WasteViewModel,
    loggedUserRol: String?,
    loggedUserId: Int,
    loggedUserName: String?,
    onBack: () -> Unit
) {
    val todasLasTareasRaw by tareaViewModel.todasLasTareas.collectAsState()
    val mermaSemanal by wasteViewModel.mermaSemanal.collectAsState()

    LaunchedEffect(Unit) {
        wasteViewModel.cargarMermaSemanal()
    }

    val esDueño = loggedUserRol == "Dueño"

    // Filter tasks if not Dueño
    val todasLasTareas = remember(todasLasTareasRaw, esDueño, loggedUserId, loggedUserName) {
        if (esDueño) {
            todasLasTareasRaw
        } else {
            todasLasTareasRaw.filter {
                it.asignadoAId == loggedUserId || 
                (it.asignadoANombre != null && it.asignadoANombre.equals(loggedUserName, ignoreCase = true))
            }
        }
    }

    // --- PROCESAMIENTO DE DATOS ---
    val totalTareas = todasLasTareas.size
    val completadas = todasLasTareas.count { it.estado == "Completada" }
    val pendientes = todasLasTareas.count { it.estado == "Pendiente" }
    val tasaProductividad = if (totalTareas > 0) (completadas.toFloat() / totalTareas.toFloat() * 100).toInt() else 0

    // 1. Distribución de tareas por Rol
    val tareasPorRol = remember(todasLasTareas) {
        todasLasTareas.groupBy { it.rol }
            .mapValues { entry ->
                val total = entry.value.size
                val comp = entry.value.count { it.estado == "Completada" }
                Pair(total, comp)
            }
    }

    // 2. Ranking de Trabajadores (por tareas completadas)
    val rankingTrabajadores = remember(todasLasTareas) {
        todasLasTareas
            .filter { it.estado == "Completada" && it.asignadoANombre != null }
            .groupBy { it.asignadoANombre!! }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
    }

    // 3. Canvas Semanal (Tareas completadas por día de la semana actual/última)
    // Para simplificar, agrupamos por la fecha ('yyyy-MM-dd') de los últimos 7 días con registro.
    val progresoSemanal = remember(todasLasTareas) {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEE dd", Locale.getDefault())
        
        todasLasTareas
            .filter { it.estado == "Completada" }
            .groupBy { it.fecha }
            .map { (fechaStr, tareas) ->
                val fecha = try { format.parse(fechaStr) } catch (e: Exception) { Date() }
                val label = outputFormat.format(fecha ?: Date())
                label to tareas.size
            }
            .sortedBy { it.first } // Orden simple por nombre de día o fecha
            .takeLast(7)
    }

    val misTareasCompletadas = remember(todasLasTareas) {
        todasLasTareas.filter { it.estado == "Completada" }
    }

    val titleText = if (esDueño) "Dashboard de Productividad" else "Mi Productividad Semanal"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleText, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // KPI Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(if (esDueño) "Total Tareas" else "Mis Tareas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text(totalTareas.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Tasa de Éxito", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text("$tasaProductividad%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3BAE76))
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Pendientes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            Text(pendientes.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Merma KPI (Solo Dueño)
            if (esDueño) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Pérdida por Merma Semanal (ODS 12.3)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Valorización total del desecho alimentario",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "S/ ${String.format(Locale.US, "%.2f", mermaSemanal)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Canvas Semanal (Productividad diaria)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(if (esDueño) "Canvas Semanal (Tareas Completadas)" else "Mi Rendimiento Diario", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        if (progresoSemanal.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No hay datos de completado esta semana", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        } else {
                            val maxValor = progresoSemanal.maxOf { it.second }.coerceAtLeast(1)
                            val barBrush = Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                progresoSemanal.forEach { (dia, cantidad) ->
                                    val alturaProporcional = (cantidad.toFloat() / maxValor.toFloat()).coerceIn(0.1f, 1f)
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Bottom,
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        Text(cantidad.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(22.dp)
                                                .fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), RoundedCornerShape(6.dp)),
                                            contentAlignment = Alignment.BottomCenter
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(22.dp)
                                                    .fillMaxHeight(alturaProporcional)
                                                    .background(
                                                        brush = barBrush,
                                                        shape = RoundedCornerShape(6.dp)
                                                    )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(dia, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (esDueño) {
                // Ranking de Trabajadores
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = Color.Yellow)
                                Text("Ranking de Productividad", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            if (rankingTrabajadores.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Aún no hay tareas completadas por trabajadores específicos.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center)
                                }
                            } else {
                                val maxCompletadas = rankingTrabajadores.first().second.toFloat()

                                rankingTrabajadores.forEachIndexed { index, (nombre, completadasCount) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "${index + 1}.",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.width(20.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(nombre, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            LinearProgressIndicator(
                                                progress = { completadasCount.toFloat() / maxCompletadas },
                                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                                color = MaterialTheme.colorScheme.secondary,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                        }
                                        Text(
                                            text = "$completadasCount completadas",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    if (index < rankingTrabajadores.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Distribución por Especialidad / Rol
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Distribución por Especialidad", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            if (tareasPorRol.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Sin datos de especialidad", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            } else {
                                tareasPorRol.forEach { (rol, metrics) ->
                                    val total = metrics.first
                                    val comp = metrics.second
                                    val percent = if (total > 0) (comp.toFloat() / total.toFloat() * 100).toInt() else 0
                                    
                                    val badgeColor = when (rol) {
                                        "Barista" -> RoleBarista
                                        "Panadero" -> RolePanadero
                                        "Cajero" -> RoleCajero
                                        else -> RoleOwner
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = badgeColor),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.width(90.dp)
                                        ) {
                                            Text(
                                                text = rol,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            )
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 12.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                progress = { if (total > 0) comp.toFloat() / total.toFloat() else 0f },
                                                modifier = Modifier.fillMaxWidth().height(6.dp),
                                                color = badgeColor,
                                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                                            )
                                        }

                                        Text(
                                            text = "$comp/$total ($percent%)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Personal Completed Tasks List (Worker View)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Mis Actividades Completadas de la Semana",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            if (misTareasCompletadas.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(80.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No has completado tareas esta semana", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            } else {
                                misTareasCompletadas.forEachIndexed { index, tarea ->
                                    Column(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(tarea.descripcion, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "Fecha Asignada: ${tarea.fecha}${if (tarea.fechaLimite != null) " | Límite: ${tarea.fechaLimite}" else ""}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (tarea.fechaCompletado != null) {
                                            Text(
                                                text = "Completado el: ${tarea.fechaCompletado}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF3BAE76),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    if (index < misTareasCompletadas.lastIndex) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
