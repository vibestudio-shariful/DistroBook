package com.example.ui.screens

import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Locale
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.t
import com.example.ui.tNonCompose
import com.example.ui.viewmodel.AppViewModel
import com.example.utils.LogManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    
    // User Profile flow states
    val currentUserName by viewModel.userName.collectAsState()
    val currentBusinessName by viewModel.businessName.collectAsState()
    val currentUserPhone by viewModel.userPhone.collectAsState()
    val currentUserEmail by viewModel.userEmail.collectAsState()
    val currentUserAddress by viewModel.userAddress.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()

    // Default values for comparison
    val defaultName = t(viewModel, "আপনার নাম", "Your Name")
    val defaultBusiness = t(viewModel, "আপনার প্রতিষ্ঠানের নাম", "Your Business Name")
    val defaultPhone = t(viewModel, "০১৭xxxxxxxx", "017xxxxxxxx")
    val defaultEmail = t(viewModel, "ইমেইল বা সোশ্যাল প্রোফাইল", "Email or Social Profile")
    val defaultAddress = t(viewModel, "আপনার ঠিকানা", "Your Address")

    // Form states
    var nameInput by remember(currentUserName) { mutableStateOf(currentUserName) }
    var businessInput by remember(currentBusinessName) { mutableStateOf(currentBusinessName) }
    var phoneInput by remember(currentUserPhone) { mutableStateOf(currentUserPhone) }
    var emailInput by remember(currentUserEmail) { mutableStateOf(currentUserEmail) }
    var addressInput by remember(currentUserAddress) { mutableStateOf(currentUserAddress) }

    // Backup & restore pending confirmation states
    var pendingLocalRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCloudRestoreBackup by remember { mutableStateOf<com.example.utils.DriveBackupFile?>(null) }
    var pendingCloudDeleteBackup by remember { mutableStateOf<com.example.utils.DriveBackupFile?>(null) }

    // Backup File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingLocalRestoreUri = uri
        }
    }

    // Photo picker for User Profile Avatar
    val avatarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveUserAvatar(context, uri)
            Toast.makeText(context, tNonCompose(isEnglish, "প্রোফাইল ছবি সফলভাবে আপডেট হয়েছে!", "Profile picture updated successfully!"), Toast.LENGTH_SHORT).show()
        }
    }

    // SAF-based backup file creator
    val fileDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    val defaultFilename = "distro_book_backup_$fileDate.json"

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportBackupToUri(
                context = context,
                uri = uri,
                onSuccess = {
                    Toast.makeText(context, tNonCompose(isEnglish, "ব্যাকআপ সরাসরি মেমোরিতে সেভ করা হয়েছে!", "Backup saved directly to storage!"), Toast.LENGTH_LONG).show()
                },
                onError = { error ->
                    Toast.makeText(context, tNonCompose(isEnglish, "ব্যাকআপ সেভ ব্যর্থ হয়েছে: $error", "Backup save failed: $error"), Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    // Google Sign-In config for Google Drive Backup
    val gso = remember {
        com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
        )
            .requestEmail()
            .requestScopes(
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.appdata"),
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file")
            )
            .build()
    }
    val googleSignInClient = remember {
        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            if (account != null) {
                viewModel.setGoogleAccount(account.email, account.displayName)
                Toast.makeText(context, tNonCompose(isEnglish, "গুগল ড্রাইভ সফলভাবে সংযুক্ত হয়েছে!", "Google Drive connected successfully!"), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, tNonCompose(isEnglish, "সংযুক্ত করতে ব্যর্থ হয়েছে: ${e.message}", "Connection failed: ${e.message}"), Toast.LENGTH_LONG).show()
        }
    }

    var launchRecovery by remember { mutableStateOf<((Intent) -> Unit)?>(null) }
    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showBackupConfirm by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var showLogDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    val authRecoveryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.loadGoogleDriveBackups { intent ->
                launchRecovery?.invoke(intent)
            }
            Toast.makeText(context, tNonCompose(isEnglish, "অনুমতি দেওয়া হয়েছে!", "Permission granted!"), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, tNonCompose(isEnglish, "অনুমতি বাতিল করা হয়েছে!", "Permission denied!"), Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(authRecoveryLauncher) {
        launchRecovery = { intent ->
            authRecoveryLauncher.launch(intent)
        }
        onDispose {
            launchRecovery = null
        }
    }

    val onAuthRequired: (Intent) -> Unit = { intent ->
        launchRecovery?.invoke(intent)
    }

    if (showPrivacyDialog) {
        PrivacyPolicyDialog(
            isEnglish = isEnglish,
            onDismiss = { showPrivacyDialog = false }
        )
    }

    if (showTermsDialog) {
        TermsAndConditionsDialog(
            isEnglish = isEnglish,
            onDismiss = { showTermsDialog = false }
        )
    }

    // Confirmation dialog for Local Restore
    if (pendingLocalRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { pendingLocalRestoreUri = null },
            title = {
                Text(
                    text = t(viewModel, "রিস্টোর নিশ্চিতকরণ", "Confirm Restore"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = t(
                        viewModel,
                        "আপনি কি নিশ্চিত যে আপনি এই লোকাল ব্যাকআপ ফাইল থেকে রিস্টোর করতে চান? আপনার বর্তমান সমস্ত লোকাল ডাটা এই ফাইলের ডেটা দিয়ে প্রতিস্থাপিত হবে এবং পূর্ববর্তী ডেটা মুছে যাবে!",
                        "Are you sure you want to restore from this local backup file? All your current local data will be replaced by the data from this file, and previous data will be lost!"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uri = pendingLocalRestoreUri
                        pendingLocalRestoreUri = null
                        if (uri != null) {
                            viewModel.importBackup(
                                context = context,
                                uri = uri,
                                onSuccess = {
                                    Toast.makeText(context, tNonCompose(isEnglish, "ডেটা সফলভাবে রিস্টোর হয়েছে!", "Data restored successfully!"), Toast.LENGTH_LONG).show()
                                },
                                onError = { error ->
                                    Toast.makeText(context, tNonCompose(isEnglish, "রিস্টোর ব্যর্থ হয়েছে: $error", "Restore failed: $error"), Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t(viewModel, "হ্যাঁ, রিস্টোর করুন", "Yes, Restore"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLocalRestoreUri = null }) {
                    Text(t(viewModel, "বাতিল", "Cancel"))
                }
            }
        )
    }

    // Confirmation dialog for Cloud Backup
    if (showBackupConfirm) {
        AlertDialog(
            onDismissRequest = { showBackupConfirm = false },
            title = { Text(t(viewModel, "ব্যাকআপ নিশ্চিতকরণ", "Confirm Backup")) },
            text = { Text(t(viewModel, "আপনি কি আপনার বর্তমান ডাটা গুগল ড্রাইভে ব্যাকআপ করতে চান?", "Are you sure you want to backup your current data to Google Drive?")) },
            confirmButton = {
                Button(onClick = {
                    showBackupConfirm = false
                    viewModel.backupToGoogleDrive(context, onAuthRequired) { success, error ->
                        if (success) Toast.makeText(context, "Backup Successful!", Toast.LENGTH_LONG).show()
                        else Toast.makeText(context, "Backup Failed: $error", Toast.LENGTH_LONG).show()
                    }
                }) { Text(t(viewModel, "হ্যাঁ", "Yes")) }
            },
            dismissButton = {
                TextButton(onClick = { showBackupConfirm = false }) { Text(t(viewModel, "না", "No")) }
            }
        )
    }

    // Confirmation dialog for Sign Out
    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            title = { Text(t(viewModel, "লগআউট নিশ্চিতকরণ", "Confirm Sign Out")) },
            text = { Text(t(viewModel, "আপনি কি নিশ্চিত যে আপনি লগআউট করতে চান?", "Are you sure you want to sign out?")) },
            confirmButton = {
                Button(onClick = {
                    showSignOutConfirm = false
                    googleSignInClient.signOut().addOnCompleteListener { viewModel.setGoogleAccount(null, null) }
                }) { Text(t(viewModel, "হ্যাঁ", "Yes")) }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) { Text(t(viewModel, "না", "No")) }
            }
        )
    }

    // Confirmation dialog for Cloud Restore
    if (pendingCloudRestoreBackup != null) {
        AlertDialog(
            onDismissRequest = { pendingCloudRestoreBackup = null },
            title = {
                Text(
                    text = t(viewModel, "ক্লাউড রিস্টোর নিশ্চিতকরণ", "Confirm Cloud Restore"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = t(
                        viewModel,
                        "আপনি কি নিশ্চিত যে আপনি এই ক্লাউড ব্যাকআপ ফাইল থেকে রিস্টোর করতে চান? আপনার বর্তমান সমস্ত লোকাল ডাটা এই ফাইলের ডেটা দিয়ে প্রতিস্থাপিত হবে!",
                        "Are you sure you want to restore from this cloud backup file? All your current local data will be replaced by the data from this file!"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backup = pendingCloudRestoreBackup
                        pendingCloudRestoreBackup = null
                        if (backup != null) {
                            viewModel.restoreFromGoogleDrive(context, backup.id, onAuthRequired) { success, error ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        tNonCompose(isEnglish, "ক্লাউড থেকে সফলভাবে রিস্টোর হয়েছে!", "Cloud Restore Successful!"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Error: $error",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t(viewModel, "হ্যাঁ, রিস্টোর করুন", "Yes, Restore"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCloudRestoreBackup = null }) {
                    Text(t(viewModel, "বাতিল", "Cancel"))
                }
            }
        )
    }

    // Confirmation dialog for Cloud Delete
    if (pendingCloudDeleteBackup != null) {
        AlertDialog(
            onDismissRequest = { pendingCloudDeleteBackup = null },
            title = {
                Text(
                    text = t(viewModel, "ব্যাকআপ ফাইল মুছুন", "Delete Backup File"),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = t(
                        viewModel,
                        "আপনি কি নিশ্চিত যে আপনি এই ক্লাউড ব্যাকআপ ফাইলটি সম্পূর্ণভাবে মুছে ফেলতে চান? এটি আর ফিরিয়ে আনা সম্ভব হবে না!",
                        "Are you sure you want to permanently delete this cloud backup file? This action cannot be undone!"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val backup = pendingCloudDeleteBackup
                        pendingCloudDeleteBackup = null
                        if (backup != null) {
                            viewModel.deleteGoogleDriveBackup(context, backup.id, onAuthRequired) { success ->
                                if (success) {
                                    Toast.makeText(
                                        context,
                                        tNonCompose(isEnglish, "ব্যাকআপ মুছে ফেলা হয়েছে", "Backup deleted"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        tNonCompose(isEnglish, "মুছতে ব্যর্থ হয়েছে", "Failed to delete"),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t(viewModel, "মুছে ফেলুন", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingCloudDeleteBackup = null }) {
                    Text(t(viewModel, "বাতিল", "Cancel"))
                }
            }
        )
    }

    // Log Feedback Dialog
    var showDriveBackupsDialog by remember { mutableStateOf(false) }
    if (showLogDialog) {
        val logs = remember { LogManager.getLogs() }
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(t(viewModel, "সিস্টেম এরর লগ", "System Error Logs"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (logs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text(t(viewModel, "কোনো এরর পাওয়া যায়নি। অ্যাপটি ভালো চলছে!", "No errors found. App is running smoothly!"), textAlign = TextAlign.Center, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.height(300.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(8.dp)) {
                            items(logs) { log ->
                                Text(text = log, fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontFamily = FontFamily.Monospace, lineHeight = 14.sp)
                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                            }
                        }
                    }
                    
                    Text(t(viewModel, "সাপোর্ট পেতে লগ পাঠান:", "Send logs for support:"), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                try {
                                    val logText = if (logs.isEmpty()) "No errors found." else logs.joinToString("\n")
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:connect.shariful@gmail.com")).apply {
                                        putExtra(Intent.EXTRA_SUBJECT, "Distro-Book Error Logs")
                                        putExtra(Intent.EXTRA_TEXT, logText)
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Email app not found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Email", fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = {
                                try {
                                    val logText = if (logs.isEmpty()) "No errors found." else logs.joinToString("\n")
                                    val uri = Uri.parse("https://wa.me/8801768899599?text=${Uri.encode("Distro-Book Error Logs:\n\n$logText")}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp not found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("WhatsApp", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLogDialog = false }) { Text(t(viewModel, "বন্ধ করুন", "Close")) }
            }
        )
    }

    // Cloud backup list dialog
    if (showDriveBackupsDialog) {
        val driveBackups by viewModel.googleDriveBackups.collectAsState()
        val isDriveLoading by viewModel.isDriveLoading.collectAsState()
        
        AlertDialog(
            onDismissRequest = { showDriveBackupsDialog = false },
            title = {
                Text(
                    text = t(viewModel, "ক্লাউড ব্যাকআপ তালিকা", "Cloud Backup List"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (isDriveLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (driveBackups.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = t(viewModel, "কোনো ক্লাউড ব্যাকআপ পাওয়া যায়নি!", "No cloud backups found!"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(driveBackups) { backup ->
                            if (backup != null && backup.name != null) {
                                val displayName = try {
                                    val parts = backup.name.replace("distrobook_backup_", "").replace(".json", "")
                                    val parser = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                                    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                                    val date = parser.parse(parts)
                                    if (date != null) formatter.format(date) else backup.name
                                } catch (e: Exception) {
                                    backup.name
                                }

                                val sizeInKb = String.format("%.1f", backup.size / 1024.0)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            pendingCloudRestoreBackup = backup
                                        },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = displayName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "$sizeInKb KB",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = {
                                                pendingCloudDeleteBackup = backup
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Backup",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDriveBackupsDialog = false }) {
                    Text(text = t(viewModel, "বাতিল", "Cancel"))
                }
            }
        )
    }

    Scaffold(
        // TopAppBar is handled by MainActivity
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("profile_screen"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Section 1: User Profile Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = t(viewModel, "ব্যবহারকারী প্রোফাইল এডিট", "Edit User Profile"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        // Profile Avatar Section
                        val userAvatarPath by viewModel.userAvatarPath.collectAsState()
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clickable {
                                        avatarLauncher.launch("image/*")
                                    }
                            ) {
                                if (userAvatarPath != null) {
                                    AsyncImage(
                                        model = userAvatarPath,
                                        contentDescription = "User Profile Picture",
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storefront,
                                            contentDescription = "User Profile",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(50.dp)
                                        )
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(30.dp)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, RoundedCornerShape(15.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change avatar",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            if (userAvatarPath != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = t(viewModel, "ছবি মুছুন", "Delete Picture"),
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            viewModel.deleteUserAvatar()
                                            Toast.makeText(context, tNonCompose(isEnglish, "প্রোফাইল ছবি মুছে ফেলা হয়েছে", "Profile picture deleted"), Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = if (nameInput == defaultName) "" else nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text(t(viewModel, "আপনার নাম *", "Your Name *")) },
                            leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            placeholder = { if (nameInput == defaultName) Text(defaultName) }
                        )

                        OutlinedTextField(
                            value = if (businessInput == defaultBusiness) "" else businessInput,
                            onValueChange = { businessInput = it },
                            label = { Text(t(viewModel, "ব্যবসা বা ডিস্ট্রিবিউশন নাম *", "Business / Distribution Name *")) },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            placeholder = { if (businessInput == defaultBusiness) Text(defaultBusiness) }
                        )

                        OutlinedTextField(
                            value = if (phoneInput == defaultPhone) "" else phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text(t(viewModel, "ফোন নম্বর", "Phone Number")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            placeholder = { if (phoneInput == defaultPhone) Text(defaultPhone) }
                        )

                        OutlinedTextField(
                            value = if (emailInput == defaultEmail) "" else emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text(t(viewModel, "ইমেইল বা সোশ্যাল প্রোফাইল", "Email or Social Profile")) },
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            placeholder = { if (emailInput == defaultEmail) Text(defaultEmail) }
                        )

                        OutlinedTextField(
                            value = if (addressInput == defaultAddress) "" else addressInput,
                            onValueChange = { addressInput = it },
                            label = { Text(t(viewModel, "ঠিকানা", "Address")) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            placeholder = { if (addressInput == defaultAddress) Text(defaultAddress) }
                        )

                        Button(
                            onClick = {
                                val finalName = nameInput.trim().ifEmpty { defaultName }
                                val finalBusiness = businessInput.trim().ifEmpty { defaultBusiness }
                                val finalPhone = phoneInput.trim().ifEmpty { defaultPhone }
                                val finalEmail = emailInput.trim().ifEmpty { defaultEmail }
                                val finalAddress = addressInput.trim().ifEmpty { defaultAddress }
                                
                                if (finalName == defaultName || finalBusiness == defaultBusiness) {
                                    Toast.makeText(context, tNonCompose(isEnglish, "নাম এবং ব্যবসার নাম খালি রাখা যাবেনা!", "Name and Business Name cannot be empty!"), Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.saveUserProfile(
                                        name = finalName,
                                        business = finalBusiness,
                                        phone = finalPhone,
                                        email = finalEmail,
                                        address = finalAddress
                                    )
                                    Toast.makeText(context, tNonCompose(isEnglish, "প্রোফাইল সফলভাবে আপডেট করা হয়েছে!", "Profile updated successfully!"), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_save_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(t(viewModel, "তথ্য সংরক্ষণ করুন", "Save Profile Info"), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Section 2: Dark Mode Settings Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = t(viewModel, "ডার্ক মোড", "Dark Mode"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = t(viewModel, "চোখের সুরক্ষায় ডার্ক থিম সক্রিয় করুন", "Enable dark theme to reduce eye strain"),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { viewModel.setDarkMode(it) }
                            )
                        }
                    }
                }
            }

            // Section 2.5: Language Selection Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surface,
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = if (isEnglish) "App Language / অ্যাপের ভাষা" else "অ্যাপের ভাষা / App Language",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = t(viewModel, "আপনার পছন্দসই ভাষা নির্বাচন করুন", "Select your preferred application language"),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Bangla button
                                Button(
                                    onClick = { viewModel.setLanguage(false) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!isEnglish) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (!isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = if (isEnglish) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null
                                ) {
                                    Text("বাংলা (Bangla)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }

                                // English button
                                Button(
                                    onClick = { viewModel.setLanguage(true) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isEnglish) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (isEnglish) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = if (!isEnglish) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null
                                ) {
                                    Text("English", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Unified Backup & Restore Card (Local & Cloud side-by-side / one below the other)
            item {
                val googleAccountEmail by viewModel.googleAccountEmail.collectAsState()
                val googleAccountDisplayName by viewModel.googleAccountDisplayName.collectAsState()
                val driveBackups by viewModel.googleDriveBackups.collectAsState()
                val isDriveLoading by viewModel.isDriveLoading.collectAsState()
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = t(viewModel, "ডাটা ব্যাকআপ ও রিস্টোর", "Data Backup & Restore"),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // SUBSECTION 1: Local Backup & Restore
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SdStorage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = t(viewModel, "লোকাল ব্যাকআপ ও রিস্টোর", "Local Backup & Restore"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = t(viewModel, "আপনার সমস্ত ডাটা ফোনের মেমোরিতে ফাইল আকারে ব্যাকআপ রাখুন এবং যেকোনো সময় রিস্টোর করুন।", "Backup your entire data as a JSON file on your phone's memory and restore anytime."),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Backup Button
                                Button(
                                    onClick = {
                                        try {
                                            createDocumentLauncher.launch(defaultFilename)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, tNonCompose(isEnglish, "ব্যাকআপ ক্রিয়েটর ওপেন করা যায়নি", "Failed to open backup creator"), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(t(viewModel, "ব্যাকআপ তৈরি", "Create Backup"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                // Restore Button
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            filePickerLauncher.launch("*/*")
                                        } catch (e: Exception) {
                                            Toast.makeText(context, tNonCompose(isEnglish, "রিস্টোর লঞ্চার ওপেন করা যায়নি", "Failed to open restore launcher"), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(t(viewModel, "রিস্টোর করুন", "Restore Backup"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // SUBSECTION 2: Google Drive / Cloud Backup & Restore
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = t(viewModel, "অনলাইন ব্যাকআপ (গুগল ড্রাইভ)", "Online Backup (Google Drive)"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (googleAccountEmail != null) {
                                // Signed in State
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccountCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = googleAccountDisplayName ?: "",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Text(
                                                    text = googleAccountEmail ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                        
                                        TextButton(
                                            onClick = { showSignOutConfirm = true }
                                        ) {
                                            Text(
                                                text = t(viewModel, "লগআউট", "Sign Out"),
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = t(
                                        viewModel,
                                        "আপনার সমস্ত বেচাকেনা ও ক্রেতার তথ্য সম্পূর্ণ নিরাপদ রাখতে সরাসরি গুগল ড্রাইভে ব্যাকআপ রাখুন।",
                                        "Securely backup your sales records and customer details directly to Google Drive."
                                    ),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )

                                if (isDriveLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = { showBackupConfirm = true },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(t(viewModel, "ক্লাউড ব্যাকআপ", "Cloud Backup"), fontSize = 13.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.loadGoogleDriveBackups(onAuthRequired)
                                                showDriveBackupsDialog = true
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(t(viewModel, "ক্লাউড রিস্টোর", "Cloud Restore"), fontSize = 13.sp)
                                        }
                                    }
                                }
                            } else {
                                // Signed out State
                                Text(
                                    text = t(
                                        viewModel,
                                        "আপনার বেচাকেনার সমস্ত ডাটা সুরক্ষিত রাখতে গুগল ড্রাইভ ব্যাকআপ সিস্টেম চালু করুন। হোয়াটসঅ্যাপের মতো যখন খুশি তখন যেকোনো ফোনে সম্পূর্ণ ডাটা রিস্টোর করতে পারবেন।",
                                        "Enable Google Drive cloud backups to keep your sales data absolutely secure. Restore your entire data anytime on any phone, just like WhatsApp."
                                    ),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )

                                Button(
                                    onClick = {
                                        googleSignInLauncher.launch(googleSignInClient.signInIntent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.CloudQueue, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = t(viewModel, "গুগল একাউন্ট দিয়ে সাইন-ইন করুন", "Sign-in with Google"),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: App Error Logs (New Position)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = t(viewModel, "অ্যাপ এরর লগ (সাপোর্ট)", "App Error Logs (Support)"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = t(
                                    viewModel,
                                    "অ্যাপ ব্যবহারে কোনো কারিগরি সমস্যা হলে নিচের বাটনে ক্লিক করে এরর লগগুলো আমাদের ইমেইল বা হোয়াটসঅ্যাপে পাঠাতে পারেন। এটি আমাদের সমস্যা সমাধানে সাহায্য করবে।",
                                    "If you face any technical issues, click the button below to send error logs via Email or WhatsApp. This helps us fix bugs quickly."
                                ),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                            Button(
                                onClick = { showLogDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t(viewModel, "এরর লগ রিপোর্ট চেক করুন", "Check Error Log Reports"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

            // Section 4: App Developer Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Designer Header with Avatar Placeholder
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Developer Profile Picture",
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Column {
                                Text(
                                    text = "Shariful Islam",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = t(viewModel, "ইউজার এক্সপেরিয়েন্স ও অ্যাপ ডেভেলপার", "UI/UX & App Developer"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                        DeveloperInfoRow(
                            label = t(viewModel, "ফেসবুক প্রোফাইল", "Facebook Profile"),
                            value = "Facebook.com/shariful.uxd",
                            icon = Icons.Default.Link,
                            isClickable = true,
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/shariful.uxd"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, tNonCompose(isEnglish, "লিঙ্ক ওপেন করা সম্ভব হয়নি", "Could not open link"), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        DeveloperInfoRow(
                            label = t(viewModel, "মোবাইল নম্বর (হোয়াটসঅ্যাপ)", "Mobile Number (WhatsApp)"),
                            value = "01768899599",
                            icon = Icons.Default.Phone,
                            isClickable = true,
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801768899599"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, tNonCompose(isEnglish, "হোয়াটসঅ্যাপ ওপেন করা সম্ভব হয়নি", "Could not open WhatsApp"), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        DeveloperInfoRow(
                            label = t(viewModel, "ইমেইল", "Email"),
                            value = "connect.shariful@gmail.com",
                            icon = Icons.Default.Email,
                            isClickable = true,
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:connect.shariful@gmail.com"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, tNonCompose(isEnglish, "ইমেইল অ্যাপ খুঁজে পাওয়া যায়নি", "Email app not found"), Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801768899599"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, tNonCompose(isEnglish, "হোয়াটসঅ্যাপ ওপেন করা সম্ভব হয়নি", "Could not open WhatsApp"), Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = t(viewModel, "নতুন আপডেট পেতে সরাসরি আমাদের সাথে হোয়াটসঅ্যাপে যোগাযোগ করতে এখানে চাপুন", "Click here to contact us directly on WhatsApp to get new updates"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section 6: App Information Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = t(viewModel, "অ্যাপ ইনফো", "App Info"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Text(
                            text = t(viewModel, "অ্যাপের নাম: ডিস্ট্রো-বুক (Distro-Book)", "App Name: Distro-Book"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val packageInfo = remember(context) {
                            try {
                                context.packageManager.getPackageInfo(context.packageName, 0)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        val appVersion = packageInfo?.versionName ?: "1.2.0"
                        Text(
                            text = t(viewModel, "সংস্করণ: $appVersion", "Version: $appVersion"),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Section 6.5: Privacy & Terms Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = t(viewModel, "প্রাইভেসি ও শর্তাবলী", "Privacy & Terms"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // Privacy Policy Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showPrivacyDialog = true }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = t(viewModel, "প্রাইভেসি পলিসি", "Privacy Policy"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Terms and Conditions Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTermsDialog = true }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = t(viewModel, "ব্যবহারের শর্তাবলী", "Terms & Conditions"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Section 7: Sponsored Ad Card (Scrollable, inside LazyColumn)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = t(viewModel, "স্পনসরড বিজ্ঞাপন", "Sponsored Ad"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Text(
                                text = t(viewModel, "মুক্ত বিজ্ঞাপন", "Ad"),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .border(
                                        0.5.dp,
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                            
                            // Ad Card that only appears if ad is loaded
                            var isAdVisible by remember { mutableStateOf(false) }
                            
                            if (isAdVisible) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Campaign,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = t(viewModel, "স্পনসরড বিজ্ঞাপন", "Sponsored Ad"),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            
                                            Text(
                                                text = t(viewModel, "মুক্ত বিজ্ঞাপন", "Ad"),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                modifier = Modifier
                                                    .border(
                                                        0.5.dp,
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }

                                    }
                                }
                            }

}
}
}
}
}
}
}
