package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

    // Form states
    var nameInput by remember(currentUserName) { mutableStateOf(currentUserName) }
    var businessInput by remember(currentBusinessName) { mutableStateOf(currentBusinessName) }
    var phoneInput by remember(currentUserPhone) { mutableStateOf(currentUserPhone) }
    var emailInput by remember(currentUserEmail) { mutableStateOf(currentUserEmail) }
    var addressInput by remember(currentUserAddress) { mutableStateOf(currentUserAddress) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("প্রোফাইল ও ডেভেলপার তথ্য", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F9FF))
                .padding(innerPadding)
                .testTag("profile_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: User Profile Form
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC2C7CF)),
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
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF0061A4))
                            Text(
                                text = "ব্যবহারকারী প্রোফাইল এডিট",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36)
                            )
                        }

                        Divider(color = Color(0xFFC2C7CF).copy(alpha = 0.5f))

                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("আপনার নাম *") },
                            leadingIcon = { Icon(Icons.Default.PersonOutline, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0061A4),
                                unfocusedBorderColor = Color(0xFFC2C7CF)
                            )
                        )

                        OutlinedTextField(
                            value = businessInput,
                            onValueChange = { businessInput = it },
                            label = { Text("ব্যবসা বা ডিস্ট্রিবিউশন নাম *") },
                            leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0061A4),
                                unfocusedBorderColor = Color(0xFFC2C7CF)
                            )
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("ফোন নম্বর") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0061A4),
                                unfocusedBorderColor = Color(0xFFC2C7CF)
                            )
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("ইমেইল বা সোশ্যাল প্রোফাইল") },
                            leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0061A4),
                                unfocusedBorderColor = Color(0xFFC2C7CF)
                            )
                        )

                        OutlinedTextField(
                            value = addressInput,
                            onValueChange = { addressInput = it },
                            label = { Text("ঠিকানা") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF0061A4),
                                unfocusedBorderColor = Color(0xFFC2C7CF)
                            )
                        )

                        Button(
                            onClick = {
                                if (nameInput.trim().isEmpty() || businessInput.trim().isEmpty()) {
                                    Toast.makeText(context, "নাম এবং ব্যবসার নাম খালি রাখা যাবেনা!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.saveUserProfile(
                                        name = nameInput.trim(),
                                        business = businessInput.trim(),
                                        phone = phoneInput.trim(),
                                        email = emailInput.trim(),
                                        address = addressInput.trim()
                                    )
                                    Toast.makeText(context, "প্রোফাইল সফলভাবে আপডেট করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("profile_save_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0061A4))
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("তথ্য সংরক্ষণ করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Section 2: App Developer Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE)),
                    border = BorderStroke(1.5.dp, Color(0xFF0061A4)),
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
                            Icon(Icons.Default.Code, contentDescription = null, tint = Color(0xFF0061A4))
                            Text(
                                text = "অ্যাপ ডেভেলপার তথ্য",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36)
                            )
                        }

                        Divider(color = Color(0xFF0061A4).copy(alpha = 0.3f))

                        // Developer info rows
                        DeveloperInfoRow(
                            label = "ডেভেলপার নাম",
                            value = "Shariful Islam",
                            icon = Icons.Default.Badge
                        )

                        DeveloperInfoRow(
                            label = "ফেসবুক প্রোফাইল",
                            value = "Facebook.com/shariful.uxd",
                            icon = Icons.Default.Link,
                            isClickable = true,
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/shariful.uxd"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "লিঙ্ক ওপেন করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        DeveloperInfoRow(
                            label = "মোবাইল নম্বর",
                            value = "01768899599",
                            icon = Icons.Default.Phone,
                            isClickable = true,
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:01768899599"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "ডায়ালার ওপেন করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }

            // Section 3: App Information Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC2C7CF)),
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
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF42474E))
                            Text(
                                text = "অ্যাপের বিবরণ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1C1E)
                            )
                        }

                        Divider(color = Color(0xFFC2C7CF).copy(alpha = 0.5f))

                        Text("অ্যাপের নাম: ডিস্ট্রো-বুক (Distro-Book)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("সংস্করণ: ১.০.৪ (v1.0.4)", fontSize = 13.sp, color = Color(0xFF42474E))
                        Text("উদ্দেশ্য: দোকান সরবরাহ ও ডিস্ট্রিবিউশন হিসাব রক্ষণাবেক্ষণ এবং সেলস ট্র্যাকিং ডায়েরি।", fontSize = 13.sp, color = Color(0xFF42474E))
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
                .background(Color(0xFF0061A4).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF0061A4), modifier = Modifier.size(18.dp))
        }

        Column {
            Text(label, fontSize = 11.sp, color = Color(0xFF42474E))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isClickable) Color(0xFF0061A4) else Color(0xFF1A1C1E)
            )
        }
    }
}
