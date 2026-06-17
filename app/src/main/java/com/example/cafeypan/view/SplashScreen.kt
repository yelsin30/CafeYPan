package com.example.cafeypan.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.example.cafeypan.R
import com.example.cafeypan.viewmodel.LoginViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: LoginViewModel,
    onNavigate: (route: String) -> Unit
) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.coffee_animation))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    // Animación de desvanecimiento para el texto de bienvenida
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    val onboardingChecked by viewModel.onboardingStatusChecked.collectAsState()

    // Navegación cuando la animación Lottie termina O el chequeo de onboarding finaliza
    LaunchedEffect(progress, onboardingChecked) {
        if (progress == 1f && onboardingChecked) {
            delay(300) // Un breve respiro
            val route = when {
                !viewModel.isOnboardingCompleted() -> "onboarding"
                viewModel.isUserLoggedIn() -> "main"
                else -> "login"
            }
            onNavigate(route)
        }
    }

    // Iniciamos la verificación del onboarding en el servidor y configuramos el timeout de seguridad
    LaunchedEffect(key1 = true) {
        viewModel.comprobarOnboardingServidor()
        delay(3500)
        val route = when {
            !viewModel.isOnboardingCompleted() -> "onboarding"
            viewModel.isUserLoggedIn() -> "main"
            else -> "login"
        }
        onNavigate(route)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        MaterialTheme.colorScheme.primary
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { progress },
                    modifier = Modifier.size(250.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Café & Pan",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Text(
                text = "Calidad y Tradición",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                modifier = Modifier.alpha(textAlpha.value)
            )
        }
    }
}
