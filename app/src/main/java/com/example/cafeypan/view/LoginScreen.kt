package com.example.cafeypan.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cafeypan.viewmodel.LoginViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginSuccess: () -> Unit,
    onTriggerBiometric: (onSuccess: (String) -> Unit) -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val loginResult by viewModel.loginResult.collectAsState()
    val lockoutTime by viewModel.lockoutTimeRemaining.collectAsState()

    var pin by remember { mutableStateOf("") }
    var acceptTerms by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    val keepScreenOnEnabled by viewModel.keepScreenOn.collectAsState()
    val context = LocalContext.current
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val hasBiometricPin = remember { viewModel.getBiometricPin() != null }

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

    LaunchedEffect(Unit) {
        if (biometricEnabled && hasBiometricPin) {
            onTriggerBiometric { biometricPin ->
                viewModel.verificarPin(biometricPin)
            }
        }
    }

    LaunchedEffect(loginResult) {
        loginResult?.let {
            if (it.isSuccess) {
                onLoginSuccess()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Background Image
        Image(
            painter = painterResource(id = com.example.cafeypan.R.drawable.img_fondo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Dark translucent overlay to guarantee high contrast for login text and inputs
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
        )

        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(id = com.example.cafeypan.R.drawable.cafe_y_pan_logo),
                    contentDescription = "Logo Café & Pan",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            shape = CircleShape
                        )
                )

                Text(
                    text = "Café & Pan",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Gestión de Personal y Tareas",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (lockoutTime > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Bloqueado",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "PIN incorrecto. Intenta en $lockoutTime segundos.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pin = it
                            }
                        },
                        label = { Text("Introduce tu PIN de 4 números") },
                        placeholder = { Text("••••") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        trailingIcon = if (biometricEnabled && hasBiometricPin) {
                            {
                                IconButton(onClick = {
                                    onTriggerBiometric { biometricPin ->
                                        viewModel.verificarPin(biometricPin)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Face,
                                        contentDescription = "Ingreso Biométrico",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = acceptTerms,
                        onCheckedChange = { acceptTerms = it },
                        enabled = lockoutTime == 0L
                    )
                    Text(
                        text = "Acepto los ",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = { showTermsDialog = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "términos y condiciones",
                            fontSize = 14.sp,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Switch(
                        checked = keepScreenOnEnabled,
                        onCheckedChange = { viewModel.setKeepScreenOnEnabled(it) }
                    )
                    Text(
                        text = "Mantener pantalla encendida",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val buttonEnabled = acceptTerms && pin.length == 4 && lockoutTime == 0L && !isLoading
                val buttonBrush = if (buttonEnabled) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                    )
                }

                Button(
                    onClick = {
                        if (!acceptTerms) {
                            return@Button
                        }
                        if (pin.length == 4) {
                            viewModel.verificarPin(pin)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(
                            brush = buttonBrush,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = buttonEnabled
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Ingresar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (buttonEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }

                loginResult?.let { result ->
                    if (result.isFailure) {
                        Text(
                            text = result.exceptionOrNull()?.message ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Términos y Condiciones") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. El uso de esta aplicación es exclusivo para el personal interno de la cafetería.")
                    Text("2. Al marcar una tarea como 'Completada', el sistema registrará tu ubicación GPS actual para validar que te encuentras físicamente en el local.")
                    Text("3. Tu PIN de acceso es personal e intransferible.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}
