package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.t
import com.example.ui.tNonCompose
import com.example.ui.viewmodel.AppViewModel

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

    // Form states
    var nameInput by remember(currentUserName) { mutableStateOf(currentUserName) }
    var businessInput by remember(currentBusinessName) { mutableStateOf(currentBusinessName) }
    var phoneInput by remember(currentUserPhone) { mutableStateOf(currentUserPhone) }
    var emailInput by remember(currentUserEmail) { mutableStateOf(currentUserEmail) }
    var addressInput by remember(currentUserAddress) { mutableStateOf(currentUserAddress) }

    // Backup File Picker Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
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

    Scaffold(
        // TopAppBar is handled by MainActivity
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .testTag("profile_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: User Profile Form
            item {
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
                                val avatarModel = userAvatarPath ?: R.drawable.img_user_avatar
                                AsyncImage(
                                    model = avatarModel,
                                    contentDescription = "User Profile Picture",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(50.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                
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
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text(t(viewModel, "আপনার নাম *", "Your Name *")) },
                            leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        OutlinedTextField(
                            value = businessInput,
                            onValueChange = { businessInput = it },
                            label = { Text(t(viewModel, "ব্যবসা বা ডিস্ট্রিবিউশন নাম *", "Business / Distribution Name *")) },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        OutlinedTextField(
                            value = phoneInput,
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
                            )
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text(t(viewModel, "ইমেইল বা সোশ্যাল প্রোফাইল", "Email or Social Profile")) },
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            label = { Text(t(viewModel, "ঠিকানা", "Address")) },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Button(
                            onClick = {
                                if (nameInput.trim().isEmpty() || businessInput.trim().isEmpty()) {
                                    Toast.makeText(context, tNonCompose(isEnglish, "নাম এবং ব্যবসার নাম খালি রাখা যাবেনা!", "Name and Business Name cannot be empty!"), Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.saveUserProfile(
                                        name = nameInput.trim(),
                                        business = businessInput.trim(),
                                        phone = phoneInput.trim(),
                                        email = emailInput.trim(),
                                        address = addressInput.trim()
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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

            // Section 2.5: Language Selection Card
            item {
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

            // Section 3: Data Backup & Restore Card
            item {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = t(viewModel, "ডেটা ব্যাকআপ ও রিস্টোর (Backup & Restore)", "Data Backup & Restore"),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = t(viewModel, "হিসাব নিকাশ সুরক্ষিত রাখুন এবং নতুন ফোনে রিস্টোর করুন", "Keep records secure and restore on a new device"),
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
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t(viewModel, "ব্যাকআপ", "Backup"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(t(viewModel, "রিস্টোর", "Restore"), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Section 4: App Developer Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
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
                            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = t(viewModel, "অ্যাপ ডেভেলপার তথ্য", "App Developer Info"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                        DeveloperInfoRow(
                            label = t(viewModel, "ডেভেলপার নাম", "Developer Name"),
                            value = "Shariful Islam",
                            icon = Icons.Default.Badge
                        )

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
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
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

            // Section 5: Data Management Card
            item {
                var showBackupFiles by remember { mutableStateOf(false) }
                
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
                            Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = t(viewModel, "ডাটা ম্যানেজমেন্ট", "Data Management"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Text(
                            text = t(viewModel, "আপনার সমস্ত ডাটা সুরক্ষিত রাখতে ব্যাকআপ নিন এবং প্রয়োজনে রিস্টোর করুন। ব্যাকআপ ফাইলটি Documents/DistroBook ফোল্ডারে জমা হয়।", "Backup your data to keep it safe and restore when needed. Backup files are saved in Documents/DistroBook folder."),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.backupData { success, message ->
                                        if (success) {
                                            Toast.makeText(context, tNonCompose(isEnglish, "ব্যাকআপ সফল হয়েছে!", "Backup Successful!"), Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, tNonCompose(isEnglish, "ব্যাকআপ ব্যর্থ হয়েছে: $message", "Backup Failed: $message"), Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(t(viewModel, "ব্যাকআপ", "Backup"), fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { showBackupFiles = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(t(viewModel, "রিস্টোর", "Restore"), fontSize = 13.sp)
                            }
                        }
                    }
                }
                
                if (showBackupFiles) {
                    val backups = viewModel.getBackupFiles()
                    AlertDialog(
                        onDismissRequest = { showBackupFiles = false },
                        title = { Text(t(viewModel, "ব্যাকআপ ফাইল নির্বাচন করুন", "Select Backup File")) },
                        text = {
                            if (backups.isEmpty()) {
                                Text(t(viewModel, "কোনো ব্যাকআপ ফাইল পাওয়া যায়নি!", "No backup files found!"))
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(backups) { file ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.restoreData(file) { success, message ->
                                                        if (success) {
                                                            Toast.makeText(context, tNonCompose(isEnglish, "সফলভাবে রিস্টোর হয়েছে!", "Restore Successful!"), Toast.LENGTH_SHORT).show()
                                                            showBackupFiles = false
                                                        } else {
                                                            Toast.makeText(context, "Error: $message", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = file.name,
                                                modifier = Modifier.padding(12.dp),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showBackupFiles = false }) {
                                Text(t(viewModel, "বাতিল", "Cancel"))
                            }
                        }
                    )
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
                        Text(
                            text = t(viewModel, "সংস্করণ: ১.০.৪ (v1.0.4)", "Version: 1.0.4 (v1.0.4)"),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperInfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isClickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isClickable) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }

        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isClickable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
