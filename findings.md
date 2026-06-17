# Hallazgos Técnicos y Resoluciones: Café & Pan App

Este documento registra los problemas técnicos críticos encontrados durante la Fase 1 y las soluciones aplicadas para que sirvan de guía en la continuación del desarrollo.

---

## 🛠️ Problemas de Configuración Gradle y Compilación (AGP 9.1.1)

### 1. Colisión del Plugin de Kotlin (Built-in Kotlin)
*   **Problema:** Intentar declarar `id("org.jetbrains.kotlin.android")` o `alias(libs.plugins.kotlin.android)` en el bloque `plugins {}` de la app arrojaba la excepción `Cannot add extension with name 'kotlin', as there is an extension already registered with that name.`
*   **Causa:** Android Gradle Plugin (AGP) 9.0+ incorpora soporte nativo y automático de Kotlin (Built-in Kotlin).
*   **Resolución:** No declarar el plugin `kotlin-android` de manera explícita en `app/build.gradle.kts` ni en el root `build.gradle.kts`. AGP se encarga de la compilación de Kotlin directamente usando la versión de Kotlin embebida (2.2.10 en AGP 9.1.1).

### 2. Error en Plugin de Hilt (Android BaseExtension Not Found)
*   **Problema:** Al aplicar el plugin de Dagger Hilt en la app se caía el Gradle Sync con `Android BaseExtension not found`.
*   **Causa:** AGP 9.0+ eliminó clases internas antiguas del DSL como `BaseExtension` de las cuales dependía Hilt.
*   **Resolución:** Se actualizó la versión de Hilt en `libs.versions.toml` de `2.51.1` a `2.59.2`. Esta versión es compatible con el nuevo DSL de AGP 9.x.

### 3. Crash de KSP2 en Room (unexpected jvm signature V)
*   **Problema:** Durante el procesamiento de anotaciones, KSP2 fallaba en la compilación con la excepción: `[ksp] java.lang.IllegalStateException: unexpected jvm signature V`.
*   **Causa:** KSP2 tiene un bug con Kotlin 2.2+ que hace fallar el compilador al analizar funciones suspendidas (`suspend`) de Room DAOs que no retornan ningún valor (su retorno por defecto es `Unit`, correspondiente a `V` / `void` en bytecode JVM).
*   **Resolución:** Se cambiaron los retornos de las funciones de escritura en los DAOs (`UserDao` y `TaskDao`) para que retornen un valor en lugar de `Unit` (`Long` para inserts y `Int` para updates/deletes). Al no retornar `Unit` (firma "V"), KSP2 no se cae.

### 4. Resolución de Maven Coordinates para Compose
*   **Problema:** Gradle 9 no encontraba las dependencias de Compose con nombres como `androidx.compose.ui:compose-ui`.
*   **Causa:** Las coordenadas correctas en Maven Central no llevan el prefijo `compose-` (ej: es `androidx.compose.ui:ui` y `androidx.compose.material3:material3`).
*   **Resolución:** Se corrigieron los nombres de los artefactos en `libs.versions.toml` a su nomenclatura oficial (`ui`, `ui-graphics`, `material3`, etc.) y se definieron versiones explícitas para saltar problemas con el BOM.

### 5. Error 403 Forbidden en Repositorio de ksoap2
*   **Problema:** Fallo de resolución de red por 403 Forbidden en el repositorio de Sonatype para `ksoap2-android`.
*   **Resolución:** Se determinó que `ksoap2` no se utiliza en todo el código y se removió la dependencia y el repositorio personalizado de `settings.gradle.kts`.

---

## 🔒 Arquitectura de Seguridad Implementada
*   **Cifrado:** Usando `MasterKey` y `EncryptedSharedPreferences` en [SessionManager.kt](file:///C:/proyecto/CafeyPan/CafeyPan/app/src/main/java/com/example/cafeypan/data/local/security/SessionManager.kt), las credenciales de sesión se guardan de forma encriptada por hardware/AES256.
*   **Lockout:** La lógica en [LoginViewModel.kt](file:///C:/proyecto/CafeyPan/CafeyPan/app/src/main/java/com/example/cafeypan/viewmodel/LoginViewModel.kt) bloquea de forma temporal la app tras 3 fallos y calcula el tiempo restante mediante `SessionManager` guardando el timestamp del sistema.
