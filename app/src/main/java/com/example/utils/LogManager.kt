package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogManager {
    private val logs = mutableListOf<String>()
    private const val MAX_LOGS = 100

    private val formatter = SimpleDateFormat("dd/MM/yy HH:mm:ss", Locale.getDefault())

    fun addErrorLog(tag: String, message: String, throwable: Throwable? = null) {
        synchronized(logs) {
            val timestamp = formatter.format(Date())
            val errorDetails = throwable?.let { "\nCause: ${it.message}\n${it.stackTrace.take(3).joinToString("\n")}" } ?: ""
            val logEntry = "[$timestamp] ERROR ($tag): $message$errorDetails"
            
            logs.add(0, logEntry)
            if (logs.size > MAX_LOGS) {
                logs.removeAt(logs.size - 1)
            }
        }
    }

    fun getLogs(): List<String> {
        synchronized(logs) {
            return logs.toList()
        }
    }

    fun clearLogs() {
        synchronized(logs) {
            logs.clear()
        }
    }
}
