# Plan de Tareas: Café & Pan App Upgrade

## Objetivo General
Llevar la aplicación "Café & Pan" de un prototipo acoplado a una arquitectura MVVM limpia de producción, usando Jetpack Compose, Hilt, Room, EncryptedSharedPreferences y sincronización offline.

---

## Estado de las Fases

### Fase 1: Refactorización y Base Sólida
*   **Estado:** **Completada** ✅
*   **Detalles:**
    *   Arquitectura migrada a MVVM limpio (UI → ViewModel → Repository → DataSource).
    *   Persistencia de datos migrada a Room Database.
    *   Hilt configurado para inyección de dependencias.
    *   Jetpack Compose implementado en todas las pantallas.
    *   Single-Activity con Navigation Compose configurada en `MainActivity`.
    *   Cifrado de credenciales usando `EncryptedSharedPreferences`.
    *   Fallo de login con bloqueo temporal de 30 segundos tras 3 intentos.
    *   Opción de cambio de PIN con validación de PIN actual.
    *   Remoción de dependencias muertas (`ksoap2-android`) y desactivación de ViewBinding.

### Fase 2: Nuevas Funcionalidades Core
*   **Estado:** **Completada** ✅
*   **Detalles:**
    *   **Gestión de personal mejorada:** Mapeo de permisos por rol (`UserRole`) y control dinámico de acciones en `MainScreen` según el trabajador logueado.
    *   **Asignación de tareas:** Actualización de Room y de los modelos para permitir asignar tareas a trabajadores específicos. Integrado selector de rol y trabajador en el panel del dueño.
    *   **Dashboard de productividad:** Nueva pantalla `DashboardScreen` que muestra KPIs globales (tasa de éxito, tareas totales), un Canvas Semanal de tareas completadas y el ranking de productividad de los trabajadores.
    *   **Tareas recurrentes:** Generación automática offline de plantillas de tareas esenciales diarias (para Baristas, Panaderos y Cajeros) al arrancar el día, optimizando la asignación inicial.

### Fase 3: Mejoras de Backend y Sincronización
*   **Estado:** **Pendiente** ⏳
*   **Tareas:**
    1.  Migrar backend de PHP a Node.js + Express + Prisma + MySQL.
    2.  Sincronización inteligente offline-first con WorkManager.
    3.  Geolocalización real (radio de local dinámico, caché de ubicación de 2 minutos).

### Fase 4: UX/UI Profesional
*   **Estado:** **Pendiente** ⏳
*   **Tareas:**
    1.  Onboarding inicial (creación del primer OWNER y configuración del local).
    2.  Splash screen animado con Lottie.
    3.  Swipe-to-complete (derecha) y swipe-to-delete (izquierda, solo dueño).
    4.  Animaciones de confeti y feedback háptico.
    5.  Notificaciones push locales programadas.

### Fase 5: Calidad y Despliegue
*   **Estado:** **Pendiente** ⏳
*   **Tareas:**
    1.  Pruebas unitarias de ViewModels con JUnit 5 + MockK.
    2.  Pruebas de integración de Room DAOs.
    3.  UI Testing con Compose Testing.
    4.  Firma de APK y configuración de ProGuard/R8 para ofuscación (<12MB).

---

## Próximas Acciones Inmediatas (Inicio de la Siguiente Sesión)
1.  **Fase 3.1 (Migración de Backend):** Crear el proyecto backend en Node.js + Express + Prisma y mapear la base de datos MySQL existente (`BaseDeDatos.txt`).
2.  **Fase 3.2 (Sincronización offline con WorkManager):** Configurar WorkManager en Android para encolar tareas pendientes de sincronizar en background cuando haya conexión.
