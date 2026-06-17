package com.example.cafeypan.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    /**
     * Converts a database date string (yyyy-MM-dd) to a UI-friendly Peruvian format (dd-MM-yyyy).
     */
    fun formatDateForUi(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return ""
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            val date = parser.parse(dateStr)
            if (date != null) formatter.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    /**
     * Converts a list of comma-separated yyyy-MM-dd date strings to dd-MM-yyyy.
     */
    fun formatMultiDateForUi(multiDateStr: String?): String {
        if (multiDateStr.isNullOrEmpty()) return ""
        return multiDateStr.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { formatDateForUi(it) }
            .joinToString(", ")
    }

    /**
     * Converts a database date-time string (yyyy-MM-dd HH:mm:ss) to UI format (dd-MM-yyyy HH:mm:ss).
     */
    fun formatDateTimeForUi(dateTimeStr: String?): String {
        if (dateTimeStr.isNullOrEmpty()) return ""
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US)
            val date = parser.parse(dateTimeStr)
            if (date != null) formatter.format(date) else dateTimeStr
        } catch (e: Exception) {
            dateTimeStr
        }
    }
}
