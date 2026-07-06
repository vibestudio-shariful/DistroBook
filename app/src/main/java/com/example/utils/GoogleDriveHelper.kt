package com.example.utils

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleDriveHelper {
    private const val TAG = "GoogleDriveHelper"
    private val client = OkHttpClient()

    // Retrieve OAuth2 Token
    suspend fun getAccessToken(context: Context, email: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val scopes = "oauth2:https://www.googleapis.com/auth/drive.appdata https://www.googleapis.com/auth/drive.file"
                val account = Account(email, "com.google")
                GoogleAuthUtil.getToken(context, account, scopes)
            } catch (e: UserRecoverableAuthException) {
                Log.e(TAG, "UserRecoverableAuthException", e)
                throw e // Propagate to activity/viewModel to handle launcher intent
            } catch (e: Exception) {
                Log.e(TAG, "Error getting access token", e)
                null
            }
        }
    }

    // Upload Backup File
    suspend fun uploadBackup(accessToken: String, backupJson: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val filename = "distrobook_backup_${sdf.format(Date())}.json"

                // Create custom multipart request body
                val boundary = "Boundary_${System.currentTimeMillis()}"
                val mediaType = "multipart/related; boundary=$boundary".toMediaTypeOrNull()

                val metadata = JSONObject().apply {
                    put("name", filename)
                    put("parents", listOf("appDataFolder"))
                }.toString()

                val requestBodyText = buildString {
                    append("--$boundary\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(metadata)
                    append("\r\n--$boundary\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(backupJson)
                    append("\r\n--$boundary--\r\n")
                }

                val requestBody = requestBodyText.toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                    .header("Authorization", "Bearer $accessToken")
                    .post(requestBody)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d(TAG, "Backup uploaded successfully: ${response.body?.string()}")
                        true
                    } else {
                        Log.e(TAG, "Backup upload failed: Code ${response.code}, Message: ${response.message}")
                        if (response.code == 401) {
                            throw java.io.IOException("401 Unauthorized")
                        }
                        false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during backup upload", e)
                if (e is java.io.IOException && e.message == "401 Unauthorized") {
                    throw e
                }
                false
            }
        }
    }

    // List Backups in appDataFolder
    suspend fun listBackups(accessToken: String): List<DriveBackupFile> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&fields=files(id,name,createdTime,size)&orderBy=createdTime%20desc")
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@withContext emptyList()
                        val json = JSONObject(body)
                        val filesArray = json.optJSONArray("files") ?: return@withContext emptyList()
                        val backupList = mutableListOf<DriveBackupFile>()
                        for (i in 0 until filesArray.length()) {
                            try {
                                val fileJson = filesArray.getJSONObject(i)
                                val id = fileJson.optString("id", "")
                                val name = fileJson.optString("name", "")
                                val createdTime = fileJson.optString("createdTime", "")
                                val sizeStr = fileJson.optString("size", "0")
                                val size = sizeStr.toLongOrNull() ?: 0L
                                if (id.isNotEmpty()) {
                                    Log.d(TAG, "File in appDataFolder: $name (ID: $id)")
                                    if (name.startsWith("distrobook_backup_")) {
                                        backupList.add(DriveBackupFile(id, name, createdTime, size))
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing backup file resource at index $i", e)
                            }
                        }
                        backupList
                    } else {
                        Log.e(TAG, "List backups failed: Code ${response.code}")
                        if (response.code == 401) {
                            throw java.io.IOException("401 Unauthorized")
                        }
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during list backups", e)
                if (e is java.io.IOException && e.message == "401 Unauthorized") {
                    throw e
                }
                emptyList()
            }
        }
    }

    // Download Backup Content
    suspend fun downloadBackup(accessToken: String, fileId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                    .header("Authorization", "Bearer $accessToken")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        response.body?.string()
                    } else {
                        Log.e(TAG, "Download backup failed: Code ${response.code}")
                        if (response.code == 401) {
                            throw java.io.IOException("401 Unauthorized")
                        }
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during download backup", e)
                if (e is java.io.IOException && e.message == "401 Unauthorized") {
                    throw e
                }
                null
            }
        }
    }

    // Delete a Backup from Drive
    suspend fun deleteBackup(accessToken: String, fileId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files/$fileId")
                    .header("Authorization", "Bearer $accessToken")
                    .delete()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.code == 401) {
                        throw java.io.IOException("401 Unauthorized")
                    }
                    response.isSuccessful
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during delete backup", e)
                if (e is java.io.IOException && e.message == "401 Unauthorized") {
                    throw e
                }
                false
            }
        }
    }
}

data class DriveBackupFile(
    val id: String,
    val name: String,
    val createdTime: String,
    val size: Long
)
