package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.OrderItem
import com.example.data.Product
import com.example.data.Shop
import com.example.ui.t
import com.example.ui.tNonCompose
import com.example.ui.viewmodel.AppViewModel
import android.net.Uri
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    viewModel: AppViewModel,
    onOrderPlacedSuccessfully: () -> Unit
) {
    val context = LocalContext.current
    val shops by viewModel.shops.collectAsState()
    val products by viewModel.products.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()

    // Screen State
    var selectedShop by remember { mutableStateOf<Shop?>(null) }
    var shopDropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    var showShopSelectionDialog by remember { mutableStateOf(false) }
    var shopSearchQuery by remember { mutableStateOf("") }
    var showAddShopQuickDialog by remember { mutableStateOf(false) }
    
    // Map of ProductId -> Selected Quantity
    val selectedQuantities = remember { mutableStateMapOf<Int, Int>() }
    // Map of ProductId -> Custom Price
    val customPrices = remember { mutableStateMapOf<Int, Double>() }
    
    var paidAmountStr by remember { mutableStateOf("") }

    
    var showReviewDialog by remember { mutableStateOf(false) }

    // Live calculations
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) {
            products
        } else {
            products.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    val selectedItemsList = products.mapNotNull { product ->
        val qty = selectedQuantities[product.id] ?: 0
        if (qty > 0) {
            val price = customPrices[product.id] ?: product.price
            OrderItem(
                productId = product.id,
                productName = product.name,
                price = price,
                quantity = qty,
                unit = product.unit
            )
        } else null
    }

    val totalAmount = selectedItemsList.sumOf { it.totalLinePrice }

    val paidAmount = remember(paidAmountStr) {
        paidAmountStr.toDoubleOrNull() ?: 0.0
    }

    val dueAmount = (totalAmount - paidAmount).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("create_order_screen")
    ) {
        // Scrollable content (Shop selection + Product selections)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Select Shop
            item {
                Text(
                    text = if (isEnglish) "1. Select Shop *" else "১. দোকান নির্বাচন করুন *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showShopSelectionDialog = true }
                        .testTag("shop_selection_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            val imageUri = selectedShop?.imageUri
                            if (!imageUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Shop Image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            if (selectedShop != null) {
                                Text(
                                    text = selectedShop!!.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (selectedShop!!.ownerName.isNotBlank()) {
                                    Text(
                                        text = if (isEnglish) "Owner: ${selectedShop!!.ownerName}" else "মালিক: ${selectedShop!!.ownerName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (selectedShop!!.address.isNotBlank()) {
                                    Text(
                                        text = selectedShop!!.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = if (isEnglish) "No Shop Selected" else "কোনো দোকান নির্বাচন করা হয়নি",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isEnglish) "Tap to select or add a new shop" else "দোকান সিলেক্ট বা নতুন যোগ করতে ট্যাপ করুন",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = { showShopSelectionDialog = true },
                            modifier = Modifier.testTag("shop_change_button")
                        ) {
                            Icon(
                                imageVector = if (selectedShop != null) Icons.Default.Edit else Icons.Default.Add,
                                contentDescription = "Edit Shop Selection",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Section 2: Choose Products & Quantities
            item {
                Text(
                    text = if (isEnglish) "2. Select Products & Quantities *" else "২. প্রোডাক্ট ও পরিমাণ সিলেক্ট করুন *",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (products.isNotEmpty()) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (isEnglish) "Search product..." else "প্রোডাক্ট খুঁজুন...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("product_search_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            if (products.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                            Text(if (isEnglish) "Please add some products first!" else "প্রথমে কিছু প্রোডাক্ট এন্ট্রি করে নিন!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            } else if (filteredProducts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(40.dp))
                            Text(if (isEnglish) "No products found with this name!" else "এই নামে কোনো প্রোডাক্ট পাওয়া যায়নি!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    val selectedQty = selectedQuantities[product.id] ?: 0
                    val isSelected = selectedQuantities.containsKey(product.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isSelected) {
                                if (product.stock > 0) {
                                    selectedQuantities[product.id] = 1
                                } else {
                                    val stockErr = tNonCompose(isEnglish, "এই প্রোডাক্টটি স্টকে নেই!", "This product is out of stock!")
                                    Toast.makeText(context, stockErr, Toast.LENGTH_SHORT).show()
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Product Info
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                    val currentPriceVal = customPrices[product.id] ?: product.price
                                    Text(
                                        text = "৳${String.format("%,.2f", currentPriceVal)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isEnglish) "Stock: ${product.stock} ${product.unit}" else "স্টক: ${product.stock} ${product.unit}",
                                        fontSize = 11.sp,
                                        color = if (product.stock == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                                        fontWeight = if (product.stock == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                // Stepper Increment / Decrement
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (selectedQty > 1) {
                                                selectedQuantities[product.id] = selectedQty - 1
                                            } else {
                                                selectedQuantities.remove(product.id)
                                            }
                                        },
                                        enabled = isSelected,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Deduct", modifier = Modifier.size(16.dp))
                                    }

                                    Text(
                                        text = selectedQty.toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    IconButton(
                                        onClick = {
                                            if (selectedQty < product.stock) {
                                                selectedQuantities[product.id] = selectedQty + 1
                                            } else {
                                                val limitErr = tNonCompose(isEnglish, "স্টকের চেয়ে বেশি দেওয়া সম্ভব নয়!", "Cannot exceed available stock!")
                                                Toast.makeText(context, limitErr, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        enabled = product.stock > selectedQty,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (product.stock > selectedQty) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                                RoundedCornerShape(8.dp)
                                            )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            // Manual inputs when selected
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom Price Manual Field
                                    var priceInputVal by remember(product.id) {
                                        mutableStateOf(customPrices[product.id]?.toString() ?: product.price.toString())
                                    }
                                    OutlinedTextField(
                                        value = priceInputVal,
                                        onValueChange = { newValue ->
                                            if (newValue.isEmpty() || newValue.toDoubleOrNull() != null || newValue == ".") {
                                                priceInputVal = newValue
                                                val parsed = newValue.toDoubleOrNull()
                                                if (parsed != null && parsed >= 0.0) {
                                                    customPrices[product.id] = parsed
                                                }
                                            }
                                        },
                                        label = { Text(if (isEnglish) "Price (৳)" else "দাম (৳)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )

                                    // Custom Quantity Manual Field (Robust fix to avoid collapsing keyboard on clear)
                                    var qtyInputVal by remember(product.id) {
                                        mutableStateOf(selectedQty.toString())
                                    }
                                    
                                    LaunchedEffect(selectedQty) {
                                        if (selectedQty > 0 && qtyInputVal.toIntOrNull() != selectedQty) {
                                            qtyInputVal = selectedQty.toString()
                                        }
                                    }

                                    OutlinedTextField(
                                        value = qtyInputVal,
                                        onValueChange = { newValue ->
                                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                                qtyInputVal = newValue
                                                val parsed = newValue.toIntOrNull()
                                                if (parsed != null && parsed > 0) {
                                                    if (parsed <= product.stock) {
                                                        selectedQuantities[product.id] = parsed
                                                    } else {
                                                        val limitErr = tNonCompose(isEnglish, "স্টকের চেয়ে বেশি দেওয়া সম্ভব নয়!", "Cannot exceed available stock!")
                                                        Toast.makeText(context, limitErr, Toast.LENGTH_SHORT).show()
                                                        selectedQuantities[product.id] = product.stock
                                                        qtyInputVal = product.stock.toString()
                                                    }
                                                } else if (newValue.isEmpty()) {
                                                    // Keep selection active as 1 to avoid focus loss or keyboard closing
                                                    selectedQuantities[product.id] = 1
                                                }
                                            }
                                        },
                                        label = { Text(if (isEnglish) "Qty (pcs)" else "পরিমাণ (টি)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                        )
                                    )

                                    // Quick Unselect Action
                                    IconButton(
                                        onClick = {
                                            selectedQuantities.remove(product.id)
                                        },
                                        modifier = Modifier.padding(top = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = if (isEnglish) "Remove Item" else "আইটেম বাদ দিন",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Persistent Invoice Bottom Summary panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Bill display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Total Items: ${selectedItemsList.size}" else "মোট আইটেম: ${selectedItemsList.size} টি",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isEnglish) "Total Bill: ৳${String.format("%,.2f", totalAmount)}" else "মোট বিল: ৳${String.format("%,.2f", totalAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Payment input & dues summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = paidAmountStr,
                        onValueChange = { paidAmountStr = it },
                        label = { Text(if (isEnglish) "Received Amount" else "আদায়কৃত টাকা") },
                        placeholder = { Text(if (isEnglish) "৳0.00" else "৳০.০০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("order_paid_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(if (isEnglish) "Remaining Due" else "অবশিষ্ট বকেয়া", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "৳${String.format("%,.2f", dueAmount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dueAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                        )
                    }
                }



                // Confirm action button (Triggers the Review Dialog first!)
                Button(
                    onClick = {
                        val shop = selectedShop
                        if (shop == null) {
                            val shopErr = tNonCompose(isEnglish, "দয়া করে প্রথমে দোকান সিলেক্ট করুন!", "Please select a shop first!")
                            Toast.makeText(context, shopErr, Toast.LENGTH_LONG).show()
                        } else if (selectedItemsList.isEmpty()) {
                            val prodErr = tNonCompose(isEnglish, "দয়া করে অন্ততঃ ১টি প্রোডাক্ট সিলেক্ট করুন!", "Please select at least 1 product!")
                            Toast.makeText(context, prodErr, Toast.LENGTH_LONG).show()
                        } else {
                            // Show review before final confirm
                            showReviewDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_order_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.ReceiptLong, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEnglish) "Review & Confirm Order" else "অর্ডার রিভিউ ও নিশ্চিত করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    // Gorgeous Order Review Modal Dialog
    if (showReviewDialog) {
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.RateReview, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(if (isEnglish) "Review Order" else "অর্ডার রিভিউ", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Shop: ${selectedShop?.name}" else "দোকান: ${selectedShop?.name}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp
                    )
                    
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    
                    Text(if (isEnglish) "Selected Items (You can adjust quantities here):" else "আইটেম তালিকা (এখান থেকে পরিবর্তন করতে পারবেন):", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    
                    // Display list of selected items with quick controllers
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(selectedItemsList, key = { it.productId }) { item ->
                            val product = products.find { it.id == item.productId }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${item.productName} (${item.unit})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("৳${String.format("%,.2f", item.price)} x ${if (isEnglish) "${item.quantity} ${item.unit}" else "${item.quantity} ${item.unit}"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text((if (isEnglish) "Total: ৳" else "মোট: ৳") + String.format("%,.2f", item.totalLinePrice), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                    
                                    // Quick controllers inside review dialog
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // Decrement
                                        IconButton(
                                            onClick = {
                                                if (item.quantity > 1) {
                                                    selectedQuantities[item.productId] = item.quantity - 1
                                                } else {
                                                    selectedQuantities.remove(item.productId)
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "Deduct", modifier = Modifier.size(14.dp))
                                        }
                                        
                                        Text(
                                            text = item.quantity.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        // Increment
                                        IconButton(
                                            onClick = {
                                                if (product != null && item.quantity < product.stock) {
                                                    selectedQuantities[item.productId] = item.quantity + 1
                                                } else {
                                                    val maxStockErr = tNonCompose(isEnglish, "স্টকের চেয়ে বেশি নেই!", "Out of stock!")
                                                    Toast.makeText(context, maxStockErr, Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                                        }
                                        
                                        // Remove entirely
                                        IconButton(
                                            onClick = {
                                                selectedQuantities.remove(item.productId)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    
                    // Live Bill details inside review
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isEnglish) "Total Bill:" else "মোট বিল:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("৳${String.format("%,.2f", totalAmount)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isEnglish) "Received Amount:" else "আদায়কৃত টাকা:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("৳${String.format("%,.2f", paidAmount)}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isEnglish) "Remaining Due:" else "অবশিষ্ট বকেয়া:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            text = "৳${String.format("%,.2f", dueAmount)}",
                            fontWeight = FontWeight.Bold,
                            color = if (dueAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val shop = selectedShop
                        if (shop != null && selectedItemsList.isNotEmpty()) {
                            viewModel.createOrder(
                                shopId = shop.id,
                                shopName = shop.name,
                                items = selectedItemsList,
                                totalAmount = totalAmount,
                                paidAmount = paidAmount
                            )
                            val orderSuccessMsg = tNonCompose(isEnglish, "বিল সফলভাবে সংরক্ষণ ও স্টক আপডেট করা হয়েছে!", "Order placed successfully and stock updated!")
                            Toast.makeText(context, orderSuccessMsg, Toast.LENGTH_LONG).show()
                            showReviewDialog = false
                            onOrderPlacedSuccessfully()
                        } else {
                            val emptyItemsMsg = tNonCompose(isEnglish, "কোনো আইটেম সিলেক্ট করা নেই!", "No items selected!")
                            Toast.makeText(context, emptyItemsMsg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (isEnglish) "Confirm Order" else "অর্ডার নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showReviewDialog = false }
                ) {
                    Text(if (isEnglish) "Edit" else "সম্পাদনা করুন")
                }
            }
        )
    }

    if (showShopSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showShopSelectionDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Select Shop / Buyer" else "দোকান বা ক্রেতা সিলেক্ট করুন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            showShopSelectionDialog = false
                            showAddShopQuickDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add New Shop",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search bar
                    OutlinedTextField(
                        value = shopSearchQuery,
                        onValueChange = { shopSearchQuery = it },
                        placeholder = { Text(if (isEnglish) "Search shop..." else "দোকান খুঁজুন...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (shopSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { shopSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    val filteredShops = remember(shops, shopSearchQuery) {
                        if (shopSearchQuery.isBlank()) {
                            shops
                        } else {
                            shops.filter {
                                it.name.contains(shopSearchQuery, ignoreCase = true) ||
                                        it.ownerName.contains(shopSearchQuery, ignoreCase = true) ||
                                        it.phone.contains(shopSearchQuery, ignoreCase = true)
                            }
                        }
                    }

                    if (filteredShops.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEnglish) "No shops found!" else "কোনো দোকান পাওয়া যায়নি!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredShops) { shop ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedShop = shop
                                            showShopSelectionDialog = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedShop?.id == shop.id) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    ),
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (selectedShop?.id == shop.id) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        }
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (!shop.imageUri.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = shop.imageUri,
                                                    contentDescription = "Shop Image",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Store,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = shop.name,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (shop.ownerName.isNotBlank() || shop.phone.isNotBlank()) {
                                                val ownerText = listOfNotNull(
                                                    shop.ownerName.takeIf { it.isNotBlank() },
                                                    shop.phone.takeIf { it.isNotBlank() }
                                                ).joinToString(" • ")
                                                Text(
                                                    text = ownerText,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
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
                TextButton(
                    onClick = { showShopSelectionDialog = false }
                ) {
                    Text(if (isEnglish) "Close" else "বন্ধ করুন")
                }
            }
        )
    }

    if (showAddShopQuickDialog) {
        var newShopName by remember { mutableStateOf("") }
        var newShopOwner by remember { mutableStateOf("") }
        var newShopPhone by remember { mutableStateOf("") }
        var newShopAddress by remember { mutableStateOf("") }
        var newShopImageUri by remember { mutableStateOf<String?>(null) }
        var isFormError by remember { mutableStateOf(false) }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                val resolver = context.contentResolver
                try {
                    resolver.openInputStream(uri)?.use { inputStream ->
                        val filename = "shop_${System.currentTimeMillis()}.jpg"
                        val file = File(context.filesDir, filename)
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                        newShopImageUri = file.absolutePath
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddShopQuickDialog = false },
            title = {
                Text(
                    text = if (isEnglish) "Quick Add Shop" else "দ্রুত দোকান যোগ করুন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!newShopImageUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = newShopImageUri,
                                    contentDescription = "Shop Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        Column {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Text(text = if (isEnglish) "Select Photo" else "ছবি যুক্ত করুন", fontSize = 11.sp)
                            }
                            if (!newShopImageUri.isNullOrBlank()) {
                                Text(
                                    text = if (isEnglish) "Remove" else "বাদ দিন",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.sp,
                                    modifier = Modifier
                                        .clickable { newShopImageUri = null }
                                        .padding(vertical = 2.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newShopName,
                        onValueChange = { newShopName = it; isFormError = false },
                        label = { Text(if (isEnglish) "Shop Name *" else "দোকানের নাম *") },
                        isError = isFormError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newShopOwner,
                        onValueChange = { newShopOwner = it },
                        label = { Text(if (isEnglish) "Owner Name" else "মালিকের নাম") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newShopPhone,
                        onValueChange = { newShopPhone = it },
                        label = { Text(if (isEnglish) "Phone Number" else "ফোন নম্বর") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newShopAddress,
                        onValueChange = { newShopAddress = it },
                        label = { Text(if (isEnglish) "Address" else "ঠিকানা") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newShopName.isBlank()) {
                            isFormError = true
                        } else {
                            viewModel.addShop(
                                name = newShopName,
                                ownerName = newShopOwner,
                                phone = newShopPhone,
                                address = newShopAddress,
                                imageUri = newShopImageUri
                            )
                            Toast.makeText(context, if (isEnglish) "Shop added!" else "দোকান যুক্ত হয়েছে!", Toast.LENGTH_SHORT).show()
                            showAddShopQuickDialog = false
                            showShopSelectionDialog = true
                        }
                    }
                ) {
                    Text(if (isEnglish) "Add Shop" else "দোকান যোগ করুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddShopQuickDialog = false
                        showShopSelectionDialog = true
                    }
                ) {
                    Text(if (isEnglish) "Cancel" else "বাতিল")
                }
            }
        )
    }
}
