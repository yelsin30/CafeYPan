package com.example.cafeypan.view

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
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
import com.example.cafeypan.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: LoginViewModel,
    onGetGpsLocation: (onSuccess: (lat: Double, lng: Double) -> Unit) -> Unit,
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()

    var ownerName by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var pinConfirm by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }

    val isFormValid = ownerName.isNotBlank() &&
            pin.length == 4 && pin.all { it.isDigit() } &&
            pin == pinConfirm &&
            shopName.isNotBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)))),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera
                Text(
                    text = "¡Bienvenido a Café & Pan!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Configura tu local y tu cuenta de Dueño por primera vez",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                // Campo: Nombre del Dueño
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Nombre del Dueño") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Campo: PIN (4 dígitos)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                    label = { Text("PIN de Acceso (4 dígitos)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )

                // Campo: Confirmar PIN
                OutlinedTextField(
                    value = pinConfirm,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinConfirm = it },
                    label = { Text("Confirmar PIN") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )

                // Campo: Nombre de la Tienda
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Nombre de la Cafetería") },
                    leadingIcon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Sección GPS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Geolocalización del Local",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )

                        if (latitude != null && longitude != null) {
                            Text(
                                text = "Lat: ${String.format("%.6f", latitude)}\nLng: ${String.format("%.6f", longitude)}",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Ubicación no configurada (se usará el valor predeterminado si omites)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Button(
                            onClick = {
                                onGetGpsLocation { lat, lng ->
                                    latitude = lat
                                    longitude = lng
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Fijar mi Ubicación Actual")
                        }
                    }
                }

                if (pin.isNotEmpty() && pinConfirm.isNotEmpty() && pin != pinConfirm) {
                    Text(
                        text = "⚠️ Los PINs no coinciden",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón Completar
                Button(
                    onClick = {
                        if (isFormValid) {
                            // Si se capturó GPS, guardarlo en SharedPreferences
                            val latToSave = latitude
                            val lngToSave = longitude
                            if (latToSave != null && lngToSave != null) {
                                val prefs = context.getSharedPreferences("cafeypan_config", android.content.Context.MODE_PRIVATE)
                                prefs.edit().apply {
                                    putFloat("latitud_cafeteria", latToSave.toFloat())
                                    putFloat("longitud_cafeteria", lngToSave.toFloat())
                                    apply()
                                }
                            }

                            viewModel.registrarDueñoYCompletarOnboarding(
                                nombreDueño = ownerName,
                                pinDueño = pin,
                                nombreLocal = shopName
                            ) { result ->
                                if (result.isSuccess) {
                                    Toast.makeText(context, "¡Configuración inicial completada! 🎉", Toast.LENGTH_LONG).show()
                                    onOnboardingComplete()
                                } else {
                                    Toast.makeText(context, "Error: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = isFormValid && !isLoading,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Comenzar Mi Negocio")
                    }
                }
            }
        }
    }
}
