package com.example.cafeypan.view

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cafeypan.ui.theme.CafeYPanTheme
import com.example.cafeypan.viewmodel.LoginViewModel
import com.example.cafeypan.viewmodel.TareaViewModel
import com.example.cafeypan.viewmodel.WasteViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : androidx.fragment.app.FragmentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private val tareaViewModel: TareaViewModel by viewModels()
    private val wasteViewModel: WasteViewModel by viewModels()

    // GPS Lector y Caché de Ubicación
    private lateinit var lectorGps: FusedLocationProviderClient
    private val latitudCafeteria = -12.046374
    private val longitudCafeteria = -77.042793
    private val distanciaMaximaPermitida = 50.0
    private val modoPrueba = true // Fase 1: keep testing bypass as configured in original

    private var cachedLocation: Location? = null
    private var lastLocationTime: Long = 0L
    private val locationCacheLimitMs = 120000L // 2 minutos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lectorGps = LocationServices.getFusedLocationProviderClient(this)

        crearCanalNotificaciones()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            val themePreference by loginViewModel.themePreference.collectAsState()
            val darkTheme = when (themePreference) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            CafeYPanTheme(darkTheme = darkTheme) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(
                            viewModel = loginViewModel,
                            onNavigate = { route ->
                                navController.navigate(route) {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("onboarding") {
                        OnboardingScreen(
                            viewModel = loginViewModel,
                            onGetGpsLocation = { callback ->
                                if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
                                    Toast.makeText(this@MainActivity, "Por favor, concede permisos de ubicación", Toast.LENGTH_SHORT).show()
                                } else {
                                    lectorGps.lastLocation.addOnSuccessListener { location ->
                                        if (location != null) {
                                            callback(location.latitude, location.longitude)
                                        } else {
                                            Toast.makeText(this@MainActivity, "No se pudo obtener el GPS. Activa la ubicación.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onOnboardingComplete = {
                                navController.navigate("login") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("login") {
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = {
                                navController.navigate("main") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onTriggerBiometric = { onSuccess ->
                                mostrarPromptBiometrico(onSuccess)
                            }
                        )
                    }

                    composable("main") {
                        MainScreen(
                            loginViewModel = loginViewModel,
                            tareaViewModel = tareaViewModel,
                            wasteViewModel = wasteViewModel,
                            onNavigateToPersonal = {
                                navController.navigate("gestion_trabajadores")
                            },
                            onNavigateToDashboard = {
                                navController.navigate("dashboard")
                            },
                            onNavigateToLogin = {
                                navController.navigate("login") {
                                    popUpTo("main") { inclusive = true }
                                }
                            },
                            onCheckGpsAndComplete = { taskId ->
                                verificarUbicacionYCompletar(taskId)
                            },
                            onConfigurarUbicacion = {
                                guardarUbicacionActualComoLocal()
                            }
                        )
                    }

                    composable("gestion_trabajadores") {
                        GestionTrabajadoresScreen(
                            viewModel = loginViewModel,
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    composable("dashboard") {
                        DashboardScreen(
                            tareaViewModel = tareaViewModel,
                            wasteViewModel = wasteViewModel,
                            loggedUserRol = loginViewModel.getLoggedUserRol(),
                            loggedUserId = loginViewModel.getLoggedUserId(),
                            loggedUserName = loginViewModel.getLoggedUserName(),
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun verificarUbicacionYCompletar(taskId: Int) {
        val completadoPorNombre = loginViewModel.getLoggedUserName() ?: "Empleado"
        if (modoPrueba) {
            tareaViewModel.marcarComoCompletada(taskId, completadoPorNombre)
            Toast.makeText(this, "MODO PRUEBA: Simulando que estás en la cafetería ✅", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            Toast.makeText(this, "Por favor, acepta el permiso de ubicación y vuelve a intentarlo", Toast.LENGTH_LONG).show()
            return
        }

        // Obtener ubicación dinámica
        val prefs = getSharedPreferences("cafeypan_config", Context.MODE_PRIVATE)
        val latitudConfig = prefs.getFloat("latitud_cafeteria", latitudCafeteria.toFloat()).toDouble()
        val longitudConfig = prefs.getFloat("longitud_cafeteria", longitudCafeteria.toFloat()).toDouble()
        val radioConfig = prefs.getFloat("distancia_maxima", distanciaMaximaPermitida.toFloat()).toDouble()

        val ubicacionLocal = Location("").apply {
            latitude = latitudConfig
            longitude = longitudConfig
        }

        val currentTime = System.currentTimeMillis()
        val cachedLoc = cachedLocation
        if (cachedLoc != null && (currentTime - lastLocationTime) <= locationCacheLimitMs) {
            val metrosDeDistancia = cachedLoc.distanceTo(ubicacionLocal)
            if (metrosDeDistancia <= radioConfig) {
                tareaViewModel.marcarComoCompletada(taskId, completadoPorNombre)
                Toast.makeText(this, "Ubicación verificada (Caché). Tarea completada ✅", Toast.LENGTH_SHORT).show()
            } else {
                val distanciaEntera = metrosDeDistancia.toInt()
                Toast.makeText(this, "Estás a $distanciaEntera metros (Caché). Acércate a la cafetería.", Toast.LENGTH_LONG).show()
            }
            return
        }

        lectorGps.lastLocation.addOnSuccessListener { ubicacionActual: Location? ->
            if (ubicacionActual != null) {
                cachedLocation = ubicacionActual
                lastLocationTime = System.currentTimeMillis()

                val metrosDeDistancia = ubicacionActual.distanceTo(ubicacionLocal)
                if (metrosDeDistancia <= radioConfig) {
                    tareaViewModel.marcarComoCompletada(taskId, completadoPorNombre)
                    Toast.makeText(this, "Ubicación verificada. Tarea completada ✅", Toast.LENGTH_SHORT).show()
                } else {
                    val distanciaEntera = metrosDeDistancia.toInt()
                    Toast.makeText(this, "Estás a $distanciaEntera metros. Acércate a la cafetería.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "No pudimos leer tu GPS. Revisa que esté encendido.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun guardarUbicacionActualComoLocal() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            return
        }
        lectorGps.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val prefs = getSharedPreferences("cafeypan_config", Context.MODE_PRIVATE)
                prefs.edit().apply {
                    putFloat("latitud_cafeteria", location.latitude.toFloat())
                    putFloat("longitud_cafeteria", location.longitude.toFloat())
                    apply()
                }
                Toast.makeText(this, "Ubicación de la cafetería actualizada con éxito ✅", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "No se pudo obtener la ubicación actual. Intente de nuevo.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nombre = "Alertas de Tareas"
            val descripcion = "Avisa cuando una tarea se pasa de 10 minutos"
            val importancia = NotificationManager.IMPORTANCE_HIGH
            val canal = NotificationChannel("canal_cafeteria", nombre, importancia).apply {
                this.description = descripcion
            }
            val administradorDeNotificaciones = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            administradorDeNotificaciones.createNotificationChannel(canal)
        }
    }

    private fun mostrarPromptBiometrico(onSuccess: (String) -> Unit) {
        val biometricManager = androidx.biometric.BiometricManager.from(this)
        val canAuth = biometricManager.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (canAuth != androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Autenticación biométrica no configurada o no soportada.", Toast.LENGTH_SHORT).show()
            return
        }

        val executor = androidx.core.content.ContextCompat.getMainExecutor(this)
        val biometricPrompt = androidx.biometric.BiometricPrompt(this, executor,
            object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@MainActivity, "Error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val savedPin = loginViewModel.getBiometricPin()
                    if (savedPin != null) {
                        onSuccess(savedPin)
                    } else {
                        Toast.makeText(this@MainActivity, "No hay PIN registrado para acceso biométrico.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@MainActivity, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso Rápido")
            .setSubtitle("Escanea tu huella para ingresar")
            .setNegativeButtonText("Cancelar")
            .setAllowedAuthenticators(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}