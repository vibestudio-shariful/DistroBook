package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.Order
import com.example.data.Shop
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ShopsScreen(
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val shops by viewModel.shops.collectAsState()
    val orders by viewModel.orders.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedShopForEdit by remember { mutableStateOf<Shop?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Shop?>(null) }
    
    var selectedShopDetails by remember { mutableStateOf<Shop?>(null) }

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
                label = { Text("দোকান খুঁজুন (Search Shops)") },
                placeholder = { Text("দোকানের নাম, ফোন নম্বর বা ঠিকানা...") },
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
                text = "মোট দোকান: ${filteredShops.size} টি",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Shops List
            if (filteredShops.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Storefront,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "কোনো দোকান পাওয়া যায়নি!" else "কোনো দোকান এন্ট্রি করা হয়নি!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "নতুন দোকান যোগ করতে নিচের '+' বাটনে চাপুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredShops, key = { it.id }) { shop ->
                        // Calculate total outstanding due for this specific shop
                        val shopOrders = orders.filter { it.shopId == shop.id }
                        val shopTotalDue = shopOrders.sumOf { it.dueAmount }

                        ShopItemRow(
                            shop = shop,
                            dueAmount = shopTotalDue,
                            onClick = { selectedShopDetails = shop },
                            onEditClick = {
                                selectedShopForEdit = shop
                                showAddEditDialog = true
                            },
                            onDeleteClick = {
                                showDeleteConfirmation = shop
                            }
                        )
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
            Icon(Icons.Outlined.Add, contentDescription = "দোকান যোগ করুন")
        }
    }

    // Add / Edit Shop Dialog
    if (showAddEditDialog) {
        AddEditShopDialog(
            shop = selectedShopForEdit,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { name, ownerName, phone, address ->
                if (selectedShopForEdit == null) {
                    viewModel.addShop(name, ownerName, phone, address)
                } else {
                    viewModel.updateShop(
                        selectedShopForEdit!!.copy(
                            name = name,
                            ownerName = ownerName,
                            phone = phone,
                            address = address
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
                    IconButton(onClick = { selectedShopDetails = null }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
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
                            if (shop.ownerName.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                    Text(text = "মালিক: ${shop.ownerName}", fontSize = 14.sp)
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
                                        Text(text = "মোবাইল: ${shop.phone}", fontSize = 14.sp)
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
                                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                    Text(text = "ঠিকানা: ${shop.address}", fontSize = 14.sp)
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
                                Text(text = "মোট বিল", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                Text(text = "৳${String.format("%,.0f", shopTotalSales)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = if (shopTotalDue > 0) MaterialTheme.colorScheme.errorContainer else Color(0xFFE8F5E9))
                        ) {
                            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "মোট বকেয়া", fontSize = 11.sp, color = if (shopTotalDue > 0) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else Color(0xFF2E7D32))
                                Text(text = "৳${String.format("%,.0f", shopTotalDue)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (shopTotalDue > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32))
                            }
                        }
                    }

                    // Delivery History list
                    Text(text = "অর্ডার হিস্টোরি (${shopOrders.size} টি)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    if (shopOrders.isEmpty()) {
                        Text(text = "এই দোকানে এখনো কোনো মালামাল সরবরাহ করা হয়নি।", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
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
                                            Text(text = "${order.items.size} টি আইটেম", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = "৳${String.format("%,.0f", order.totalAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            if (order.dueAmount > 0) {
                                                Text(text = "বকেয়া: ৳${String.format("%,.0f", order.dueAmount)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                            } else {
                                                Text(text = "পরিশোধিত", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
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
                Button(onClick = { selectedShopDetails = null }) {
                    Text("ঠিক আছে")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteConfirmation?.let { shop ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("দোকান মুছে ফেলার শতর্কতা") },
            text = { Text("'${shop.name}' মুছে ফেললে এর বকেয়া ও হিস্টোরি আর দেখা যাবে না। আপনি কি নিশ্চিত?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteShop(shop)
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন (Delete)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("বাতিল (Cancel)")
                }
            }
        )
    }
}

@Composable
fun ShopItemRow(
    shop: Shop,
    dueAmount: Double,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shop.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (shop.ownerName.isNotBlank()) {
                        Text(
                            text = "মালিক: ${shop.ownerName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    if (shop.phone.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.Call, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(text = shop.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    if (shop.address.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                            Text(text = shop.address, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Outstanding Due Badge
                val isDark = isSystemInDarkTheme()
                val dueBg = if (dueAmount > 0) {
                    if (isDark) Color(0xFF8C1D18).copy(alpha = 0.2f) else Color(0xFFF9DEDC)
                } else {
                    if (isDark) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFD2E8D1)
                }
                val dueColor = if (dueAmount > 0) {
                    if (isDark) Color(0xFFF9DEDC) else Color(0xFF410E0B)
                } else {
                    if (isDark) Color(0xFFC8E6C9) else Color(0xFF0A210B)
                }
                val dueText = if (dueAmount > 0) "বকেয়া: ৳${String.format("%,.0f", dueAmount)}" else "কোনো বকেয়া নেই"

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(dueBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = dueText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = dueColor
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditShopDialog(
    shop: Shop?,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(shop?.name ?: "") }
    var ownerName by remember { mutableStateOf(shop?.ownerName ?: "") }
    var phone by remember { mutableStateOf(shop?.phone ?: "") }
    var address by remember { mutableStateOf(shop?.address ?: "") }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (shop == null) "নতুন দোকান যোগ করুন" else "দোকানের তথ্য এডিট করুন") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("দোকানের নাম *") },
                    placeholder = { Text("যেমন- সততা জেনারেল স্টোর") },
                    modifier = Modifier.fillMaxWidth().testTag("shop_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("মালিকের নাম (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন- মো: রফিক") },
                    modifier = Modifier.fillMaxWidth().testTag("shop_owner_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("মোবাইল নম্বর (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন- 017xxxxxxxx") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("shop_phone_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("দোকানের ঠিকানা (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন- মিরপুর ১০, ঢাকা") },
                    modifier = Modifier.fillMaxWidth().testTag("shop_address_input"),
                    maxLines = 2
                )

                if (isError) {
                    Text(
                        text = "অনুগ্রহ করে দোকানের নাম সঠিকভাবে লিখুন।",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
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
                        onConfirm(name, ownerName, phone, address)
                    }
                },
                modifier = Modifier.testTag("shop_dialog_confirm")
            ) {
                Text("সংরক্ষণ করুন (Save)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল (Cancel)")
            }
        }
    )
}
