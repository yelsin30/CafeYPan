package com.example.cafeypan.model

enum class UserRole(
    val canDeleteTasks: Boolean,
    val canManageWorkers: Boolean,
    val canCreateTasks: Boolean,
    val canEditTasks: Boolean,
    val canCompleteTasks: Boolean
) {
    OWNER(canDeleteTasks = true, canManageWorkers = true, canCreateTasks = true, canEditTasks = true, canCompleteTasks = true),
    BARISTA(canDeleteTasks = false, canManageWorkers = false, canCreateTasks = false, canEditTasks = false, canCompleteTasks = true),
    PANADERO(canDeleteTasks = false, canManageWorkers = false, canCreateTasks = false, canEditTasks = false, canCompleteTasks = true),
    CAJERO(canDeleteTasks = false, canManageWorkers = false, canCreateTasks = false, canEditTasks = false, canCompleteTasks = true);

    companion object {
        fun fromRolName(rolName: String): UserRole {
            return when {
                rolName.equals("Dueño", ignoreCase = true) || rolName.equals("OWNER", ignoreCase = true) -> OWNER
                rolName.equals("Barista", ignoreCase = true) -> BARISTA
                rolName.equals("Panadero", ignoreCase = true) -> PANADERO
                rolName.equals("Cajero", ignoreCase = true) -> CAJERO
                else -> BARISTA // Fallback por defecto
            }
        }
    }
}
