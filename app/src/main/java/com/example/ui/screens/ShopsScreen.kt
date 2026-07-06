package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Order
import com.example.data.Shop
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.t
import com.example.ui.tNonCompose
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShopsScreen(
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val shops by viewModel.shops.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedShopForEdit by remember { mutableStateOf<Shop?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Shop?>(null) }
    
    var selectedShopDetails by remember { mutableStateOf<Shop?>(null) }
    var showPaymentDialogForShop by remember { mutableStateOf<Shop?>(null) }
    var paymentAmountInput by remember { mutableStateOf("") }

    val filteredShops = remember(shops, searchQuery) {
        if (searchQuery.isBlank()) {
            shops
        } else {
            shops.filter { 
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery) ||
                it.address.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("shops_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(t(viewModel, "দোকান খুঁজুন (Search Shops)", "Search Shops")) },
                placeholder = { Text(t(viewModel, "দোকানের নাম, ফোন নম্বর বা ঠিকানা...", "Shop name, phone, or address...")) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shop_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Shops Header
            Text(
                text = t(viewModel, "মোট দোকান: ${filteredShops.size} টি", "Total Shops: ${filteredShops.size}"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            var isInitialAdVisible by remember { mutableStateOf(false) }
            AdBanner(onVisibilityChanged = { isInitialAdVisible = it })
            if (isInitialAdVisible) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Shops List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (filteredShops.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = if (searchQuery.isNotEmpty()) {
                                        t(viewModel, "কোনো দোকান পাওয়া যায়নি!", "No shops found!")
                                    } else {
                                        t(viewModel, "কোনো দোকান এন্ট্রি করা হয়নি!", "No shops entered yet!")
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = t(viewModel, "নতুন দোকান যোগ করতে নিচের '+' বাটনে চাপুন।", "Press the '+' button below to add a new shop."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    filteredShops.forEachIndexed { index, shop ->
                        item(key = "shop_${shop.id}") {
                            Column {
                                // Calculate total outstanding due for this specific shop
                                val shopOrders = orders.filter { it.shopId == shop.id }
                                val shopTotalDue = shopOrders.sumOf { it.dueAmount }

                                ShopItemRow(
                                    shop = shop,
                                    dueAmount = shopTotalDue,
                                    isEnglish = isEnglish,
                                    isDarkMode = isDarkMode,
                                    onClick = { selectedShopDetails = shop },
                                    onEditClick = {
                                        selectedShopForEdit = shop
                                        showAddEditDialog = true
                                    },
                                    onDeleteClick = {
                                        showDeleteConfirmation = shop
                                    }
                                )
                                
                                if ((index + 1) % 10 == 0 && index < filteredShops.size - 1) {
                                    var isInlineAdVisible by remember { mutableStateOf(false) }
                                    if (isInlineAdVisible) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                    }
                                    AdBanner(onVisibilityChanged = { isInlineAdVisible = it })
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button to Add Shop
        FloatingActionButton(
            onClick = {
                selectedShopForEdit = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_shop_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Outlined.Add, contentDescription = t(viewModel, "দোকান যোগ করুন", "Add Shop"))
        }
    }

    // Add / Edit Shop Dialog
    if (showAddEditDialog) {
        AddEditShopDialog(
            shop = selectedShopForEdit,
            isEnglish = isEnglish,
            viewModel = viewModel,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { name, ownerName, phone, address, imageUri ->
                if (selectedShopForEdit == null) {
                    viewModel.addShop(name, ownerName, phone, address, imageUri)
                } else {
                    viewModel.updateShop(
                        selectedShopForEdit!!.copy(
                            name = name,
                            ownerName = ownerName,
                            phone = phone,
                            address = address,
                            imageUri = imageUri
                        )
                    )
                }
                showAddEditDialog = false
            }
        )
    }

    // Shop Details Dialog
    selectedShopDetails?.let { shop ->
        val shopOrders = orders.filter { it.shopId == shop.id }
        val shopTotalDue = shopOrders.sumOf { it.dueAmount }
        val shopTotalSales = shopOrders.sumOf { it.totalAmount }

        AlertDialog(
            onDismissRequest = { selectedShopDetails = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = shop.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            selectedShopForEdit = shop
                            showAddEditDialog = true
                        }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { selectedShopDetails = null }) {
                            Icon(Icons.Outlined.Close, contentDescription = if (isEnglish) "Close" else "বন্ধ করুন")
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Contact Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val isImageValid = remember(shop.imageUri) {
                                if (!shop.imageUri.isNullOrBlank()) {
                                    if (shop.imageUri.startsWith("content://") || shop.imageUri.startsWith("http://") || shop.imageUri.startsWith("https://")) {
                                        true
                                    } else {
                                        try {
                                            java.io.File(shop.imageUri).exists()
                                        } catch (e: Exception) {
                                            false
                                        }
                                    }
                                } else {
                                    false
                                }
                            }

                            if (isImageValid) {
                                AsyncImage(
                                    model = shop.imageUri,
                                    contentDescription = "Shop Image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Storefront,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            if (shop.ownerName.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                    Text(text = if (isEnglish) "Owner: ${shop.ownerName}" else "মালিক: ${shop.ownerName}", fontSize = 14.sp)
                                }
                            }
                            if (shop.phone.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Outlined.Call, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                        Text(text = if (isEnglish) "Mobile: ${shop.phone}" else "মোবাইল: ${shop.phone}", fontSize = 14.sp)
                                    }
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${shop.phone}"))
                                            context.startActivity(intent)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Outlined.Call, contentDescription = "Call", tint = Color(0xFF2E7D32))
                                    }
                                }
                            }
                            if (shop.address.isNotBlank()) {
                                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                    Text(text = if (isEnglish) "Address: ${shop.address}" else "ঠিকানা: ${shop.address}", fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // Financial Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = if (isEnglish) "Total Sales" else "মোট বিল", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                Text(text = "৳${String.format("%,.0f", shopTotalSales)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        val isDarkMode by viewModel.isDarkMode.collectAsState()
                        val isDark = isDarkMode
                        val cardBg = if (shopTotalDue > 0) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            if (isDark) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFE8F5E9)
                        }
                        val textColor = if (shopTotalDue > 0) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            if (isDark) Color(0xFFC8E6C9) else Color(0xFF2E7D32)
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = cardBg)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = if (isEnglish) "Total Dues" else "মোট বকেয়া", fontSize = 11.sp, color = textColor.copy(alpha = 0.8f))
                                Text(text = "৳${String.format("%,.0f", shopTotalDue)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                    }

                    // Delivery History list
                    Text(text = if (isEnglish) "Order History (${shopOrders.size})" else "অর্ডার হিস্টোরি (${shopOrders.size} টি)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    if (shopOrders.isEmpty()) {
                        Text(text = if (isEnglish) "No items have been supplied to this shop yet." else "এই দোকানে এখনো কোনো মালামাল সরবরাহ করা হয়নি।", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(shopOrders) { order ->
                                val dateStr = SimpleDateFormat("dd MMM yy", Locale.getDefault()).format(Date(order.timestamp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = dateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(text = if (isEnglish) "${order.items.size} Items" else "${order.items.size} টি আইটেম", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = "৳${String.format("%,.0f", order.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            if (order.dueAmount > 0) {
                                                Text(text = if (isEnglish) "Due: ৳${String.format("%,.0f", order.dueAmount)}" else "বকেয়া: ৳${String.format("%,.0f", order.dueAmount)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                            } else {
                                                val isDarkMode by viewModel.isDarkMode.collectAsState()
                                                Text(text = if (isEnglish) "Paid" else "পরিশোধিত", fontSize = 10.sp, color = if (isDarkMode) Color(0xFFC8E6C9) else Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (shopTotalDue > 0) {
                        Button(
                            onClick = {
                                paymentAmountInput = ""
                                selectedShopDetails = null
                                showPaymentDialogForShop = shop
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Text(if (isEnglish) "Collect Due" else "বকেয়া আদায়")
                        }
                    }
                    Button(onClick = { selectedShopDetails = null }) {
                        Text(if (isEnglish) "OK" else "ঠিক আছে")
                    }
                }
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteConfirmation?.let { shop ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text(t(viewModel, "দোকান মুছে ফেলার সতর্কতা", "Delete Shop Warning")) },
            text = { Text(t(viewModel, "'${shop.name}' মুছে ফেললে এর বকেয়া ও হিস্টোরি আর দেখা যাবে না। আপনি কি নিশ্চিত?", "Deleting '${shop.name}' will permanently remove its history and outstanding dues. Are you sure?")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteShop(shop)
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t(viewModel, "মুছে ফেলুন (Delete)", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text(t(viewModel, "বাতিল (Cancel)", "Cancel"))
                }
            }
        )
    }

    // Payment Amount Dialog
    showPaymentDialogForShop?.let { shop ->
        AlertDialog(
            onDismissRequest = { showPaymentDialogForShop = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = t(viewModel, "বকেয়া আদায়", "Collect Due"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = t(
                            viewModel,
                            "দোকান: ${shop.name}\nকত টাকা বকেয়া আদায় করা হয়েছে?",
                            "Shop: ${shop.name}\nEnter amount collected:"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(t(viewModel, "আদায়ের পরিমাণ", "Amount Collected")) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("payment_amount_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), // High visibility in both modes!
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = paymentAmountInput.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            viewModel.collectPartialShopDues(shop.id, amount)
                        }
                        showPaymentDialogForShop = null
                        selectedShopDetails = null
                    },
                    modifier = Modifier.testTag("payment_dialog_confirm"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(t(viewModel, "নিশ্চিত করুন (Confirm)", "Confirm"), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPaymentDialogForShop = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(t(viewModel, "বাতিল (Cancel)", "Cancel"), fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
fun ShopItemRow(
    shop: Shop,
    dueAmount: Double,
    isEnglish: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val isImageValid = remember(shop.imageUri) {
                            if (!shop.imageUri.isNullOrBlank()) {
                                if (shop.imageUri.startsWith("content://") || shop.imageUri.startsWith("http://") || shop.imageUri.startsWith("https://")) {
                                    true
                                } else {
                                    try {
                                        java.io.File(shop.imageUri).exists()
                                    } catch (e: Exception) {
                                        false
                                    }
                                }
                            } else {
                                false
                            }
                        }

                        if (isImageValid) {
                            AsyncImage(
                                model = shop.imageUri,
                                contentDescription = "Shop Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = shop.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (shop.ownerName.isNotBlank()) {
                            Text(
                                text = if (isEnglish) "Owner: ${shop.ownerName}" else "মালিক: ${shop.ownerName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (shop.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = shop.phone,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (shop.address.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(
                                text = shop.address,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Outstanding Due Badge
                val isDark = isDarkMode
                val dueBg = if (dueAmount > 0) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    if (isDark) Color(0xFF1B5E20).copy(alpha = 0.3f) else Color(0xFFE8F5E9)
                }
                val dueColor = if (dueAmount > 0) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    if (isDark) Color(0xFFC8E6C9) else Color(0xFF2E7D32)
                }
                val dueText = if (dueAmount > 0) {
                    if (isEnglish) "Due: ৳${String.format("%,.0f", dueAmount)}" else "বকেয়া: ৳${String.format("%,.0f", dueAmount)}"
                } else {
                    if (isEnglish) "Paid" else "পরিশোধিত"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(dueBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = dueText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = dueColor
                    )
                }
            }
        }
    }
}
}

@Composable
fun AddEditShopDialog(
    shop: Shop?,
    isEnglish: Boolean,
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String?) -> Unit
) {
    var name by remember { mutableStateOf(shop?.name ?: "") }
    var ownerName by remember { mutableStateOf(shop?.ownerName ?: "") }
    var phone by remember { mutableStateOf(shop?.phone ?: "") }
    var address by remember { mutableStateOf(shop?.address ?: "") }
    var imageUri by remember { mutableStateOf(shop?.imageUri) }

    var isError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.saveShopImage(context, uri) { path ->
                if (path != null) {
                    imageUri = path
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = if (shop == null) Icons.Default.Storefront else Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = if (shop == null) {
                        if (isEnglish) "Add New Shop" else "নতুন দোকান যোগ করুন"
                    } else {
                        if (isEnglish) "Edit Shop Details" else "দোকানের তথ্য এডিট করুন"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Image selection slot
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val currentImageUri = imageUri
                        val isImageValid = remember(currentImageUri) {
                            if (!currentImageUri.isNullOrBlank()) {
                                if (currentImageUri.startsWith("content://") || currentImageUri.startsWith("http://") || currentImageUri.startsWith("https://")) {
                                    true
                                } else {
                                    try {
                                        java.io.File(currentImageUri).exists()
                                    } catch (e: Exception) {
                                        false
                                    }
                                }
                            } else {
                                false
                            }
                        }

                        if (isImageValid) {
                            AsyncImage(
                                model = currentImageUri,
                                contentDescription = "Shop Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.AddAPhoto,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column {
                        Button(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = if (isEnglish) "Select Photo" else "ছবি যুক্ত করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (!imageUri.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isEnglish) "Remove Photo" else "ছবিটি বাদ দিন",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable { imageUri = null }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isEnglish) "Shop Name *" else "দোকানের নাম *") },
                    placeholder = { Text(if (isEnglish) "e.g. Sota General Store" else "যেমন- সততা জেনারেল স্টোর") },
                    modifier = Modifier.fillMaxWidth().testTag("shop_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text(if (isEnglish) "Owner Name (Optional)" else "মালিকের নাম (ঐচ্ছিক)") },
                    placeholder = { Text(if (isEnglish) "e.g. Rafiq" else "যেমন- মো: রফিক") },
                    modifier = Modifier.fillMaxWidth().testTag("shop_owner_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(if (isEnglish) "Mobile Number (Optional)" else "মোবাইল নম্বর (ঐচ্ছিক)") },
                    placeholder = { Text(if (isEnglish) "e.g. 017xxxxxxxx" else "যেমন- 017xxxxxxxx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("shop_phone_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(if (isEnglish) "Shop Address (Optional)" else "দোকানের ঠিকানা (ঐচ্ছিক)") },
                    placeholder = { Text(if (isEnglish) "e.g. Mirpur 10, Dhaka" else "যেমন- মিরপুর ১০, ঢাকা") },
                    modifier = Modifier.fillMaxWidth().testTag("shop_address_input"),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (isError) {
                    Text(
                        text = if (isEnglish) "Please enter a valid shop name." else "অনুগ্রহ করে দোকানের নাম সঠিকভাবে লিখুন।",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                    } else {
                        onConfirm(name, ownerName, phone, address, imageUri)
                    }
                },
                modifier = Modifier.testTag("shop_dialog_confirm"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isEnglish) "Save" else "সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(if (isEnglish) "Cancel" else "বাতিল", fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
