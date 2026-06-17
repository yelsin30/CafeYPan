package com.example.cafeypan.data.repository

import com.example.cafeypan.data.ApiService
import com.example.cafeypan.data.local.dao.UserDao
import com.example.cafeypan.data.local.entity.UserEntity
import com.example.cafeypan.data.local.security.SessionManager
import com.example.cafeypan.model.RespuestaLogin
import com.example.cafeypan.model.Trabajador
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val apiService: ApiService,
    private val sessionManager: SessionManager
) {
    suspend fun verificarPin(pin: String): Result<RespuestaLogin> = withContext(Dispatchers.IO) {
        // 1. Check if locked out
        if (sessionManager.isLockedOut()) {
            val remaining = sessionManager.getLockoutTimeRemaining()
            return@withContext Result.failure(Exception("Bloqueado. Intenta de nuevo en $remaining segundos."))
        }

        // 2. Try Remote verification
        try {
            val response = apiService.verificarPin(pin)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.exito && body.datos != null) {
                    // Success online
                    val trabajador = body.datos
                    // Clean up local-only duplicates with same PIN but different ID
                    val existingLocal = userDao.getUserByPin(trabajador.pin)
                    if (existingLocal != null && existingLocal.id != trabajador.id) {
                        userDao.deleteUser(existingLocal)
                    }
                    // Sync with local Room database
                    userDao.insertUser(
                        UserEntity(
                            id = trabajador.id,
                            nombre = trabajador.nombre,
                            rol = trabajador.rol,
                            pin = trabajador.pin,
                            activo = true,
                            apellido = trabajador.apellido,
                            telefono = trabajador.telefono,
                            contactoEmergenciaNombre = trabajador.contactoEmergenciaNombre,
                            contactoEmergenciaTelefono = trabajador.contactoEmergenciaTelefono,
                            notas = trabajador.notas
                        )
                    )
                    // Reset lockout tracking
                    sessionManager.resetFailedAttempts()
                    // Save Session
                    sessionManager.saveUserSession(
                        id = trabajador.id,
                        nombre = trabajador.nombre,
                        rol = trabajador.rol,
                        pin = trabajador.pin
                    )
                    return@withContext Result.success(body)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Ignore network errors and fallback to local check
        }

        // 3. Fallback/Primary Local verification from Room
        val localUser = userDao.getUserByPin(pin)
        if (localUser != null) {
            if (!localUser.activo) {
                return@withContext Result.failure(Exception("Esta cuenta de trabajador está desactivada."))
            }
            // Success offline
            sessionManager.resetFailedAttempts()
            sessionManager.saveUserSession(
                id = localUser.id,
                nombre = localUser.nombre,
                rol = localUser.rol,
                pin = localUser.pin
            )
            val response = RespuestaLogin(
                exito = true,
                mensaje = "Ingreso exitoso (Local)",
                datos = Trabajador(
                    id = localUser.id,
                    nombre = localUser.nombre,
                    rol = localUser.rol,
                    pin = localUser.pin
                )
            )
            return@withContext Result.success(response)
        }

        // 4. Failed attempt tracking
        val attempts = sessionManager.incrementFailedAttempts()
        val remainingAttempts = 3 - attempts
        val message = if (remainingAttempts > 0) {
            "PIN incorrecto. Te quedan $remainingAttempts intentos."
        } else {
            "Demasiados intentos fallidos. Bloqueado por 30 segundos."
        }
        return@withContext Result.failure(Exception(message))
    }

    suspend fun registrarTrabajador(
        nombre: String,
        pin: String,
        rol: String,
        apellido: String? = null,
        telefono: String? = null,
        contactoEmergenciaNombre: String? = null,
        contactoEmergenciaTelefono: String? = null,
        notas: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        // Register remotely
        var remoteSuccess = false
        var message = "Error de conexión"
        var remoteUser: Trabajador? = null
        try {
            val response = apiService.guardarTrabajador(
                nombre, pin, rol, apellido, telefono,
                contactoEmergenciaNombre, contactoEmergenciaTelefono, notas
            )
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                remoteSuccess = body.exito
                message = body.mensaje
                if (body.exito) {
                    remoteUser = body.datos
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Register locally anyway (offline-first) using remote ID if available
        val userEntity = UserEntity(
            id = remoteUser?.id ?: 0,
            nombre = nombre,
            pin = pin,
            rol = rol,
            activo = true,
            apellido = apellido,
            telefono = telefono,
            contactoEmergenciaNombre = contactoEmergenciaNombre,
            contactoEmergenciaTelefono = contactoEmergenciaTelefono,
            notas = notas
        )
        
        // Clean up any local duplicate with the same PIN
        val existingLocal = userDao.getUserByPin(pin)
        if (existingLocal != null && (remoteUser != null && existingLocal.id != remoteUser.id)) {
            userDao.deleteUser(existingLocal)
        }
        
        userDao.insertUser(userEntity)

        return@withContext if (remoteSuccess || true) {
            Result.success(true)
        } else {
            Result.failure(Exception(message))
        }
    }

    suspend fun cambiarPin(pinNuevo: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val currentUserId = sessionManager.getUserId()
        if (currentUserId == -1) {
            return@withContext Result.failure(Exception("No hay una sesión activa."))
        }
        val currentPin = sessionManager.getUserPin() ?: ""
        val nombre = sessionManager.getUserName() ?: ""
        val rol = sessionManager.getUserRol() ?: ""

        // Update locally
        val currentUser = userDao.getAllUsers().find { it.id == currentUserId }
        val user = currentUser?.copy(pin = pinNuevo) ?: UserEntity(
            id = currentUserId,
            nombre = nombre,
            rol = rol,
            pin = pinNuevo,
            activo = true
        )
        userDao.insertUser(user)

        // Update in session
        sessionManager.saveUserSession(currentUserId, nombre, rol, pinNuevo)

        // We can sync this change with the remote backend in subsequent phases
        return@withContext Result.success(true)
    }

    fun isUserLoggedIn(): Boolean = sessionManager.isLoggedIn()
    fun getLoggedUserRol(): String? = sessionManager.getUserRol()
    fun getLoggedUserName(): String? = sessionManager.getUserName()
    fun getLoggedUserId(): Int = sessionManager.getUserId()
    fun logout() = sessionManager.clearUserSession()

    suspend fun getTrabajadores(): List<UserEntity> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.obtenerTrabajadores()
            if (response.isSuccessful && response.body() != null) {
                val remoteUsers = response.body()!!
                for (user in remoteUsers) {
                    // Check if a local user with the same PIN but a different ID exists to delete it
                    val localUserByPin = userDao.getUserByPin(user.pin)
                    if (localUserByPin != null && localUserByPin.id != user.id) {
                        userDao.deleteUser(localUserByPin)
                    }
                    userDao.insertUser(user)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Ignore network errors, fallback to local DB below
        }
        userDao.getAllUsers()
    }

    suspend fun toggleTrabajadorActivo(id: Int, activo: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        val user = userDao.getAllUsers().find { it.id == id }
        if (user != null) {
            userDao.updateUser(user.copy(activo = activo))
        }

        try {
            val response = apiService.toggleTrabajador(id, activo)
            if (response.isSuccessful && response.body()?.exito == true) {
                Result.success(true)
            } else {
                val errorMsg = response.body()?.mensaje ?: "Error en el servidor"
                if (user != null) {
                    userDao.updateUser(user) // Rollback
                }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Offline support: keep local modification
            Result.success(true)
        }
    }

    suspend fun editarTrabajador(
        id: Int,
        nombre: String,
        pin: String,
        rol: String,
        apellido: String? = null,
        telefono: String? = null,
        contactoEmergenciaNombre: String? = null,
        contactoEmergenciaTelefono: String? = null,
        notas: String? = null
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        var remoteSuccess = false
        var message = "Error de conexión"
        try {
            val response = apiService.editarTrabajador(
                id, nombre, pin, rol, apellido, telefono,
                contactoEmergenciaNombre, contactoEmergenciaTelefono, notas
            )
            if (response.isSuccessful && response.body() != null) {
                remoteSuccess = response.body()!!.exito
                message = response.body()!!.mensaje
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val user = userDao.getAllUsers().find { it.id == id }
        if (user != null) {
            val existingLocal = userDao.getUserByPin(pin)
            if (existingLocal != null && existingLocal.id != id) {
                userDao.deleteUser(existingLocal)
            }
            userDao.insertUser(
                user.copy(
                    nombre = nombre,
                    pin = pin,
                    rol = rol,
                    apellido = apellido,
                    telefono = telefono,
                    contactoEmergenciaNombre = contactoEmergenciaNombre,
                    contactoEmergenciaTelefono = contactoEmergenciaTelefono,
                    notas = notas
                )
            )
        }

        return@withContext if (remoteSuccess || true) {
            Result.success(true)
        } else {
            Result.failure(Exception(message))
        }
    }
}

