package com.example.utils

object LogManager {
    private val logs = mutableListOf<String>()
    private const val MAX_LOGS = 50

    fun addLog(log: String) {
        synchronized(logs) {
            logs.add(0, "${System.currentTimeMillis()}: $log")
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
}
