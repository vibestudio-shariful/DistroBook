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
import com.example.ui.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    viewModel: AppViewModel,
    onOrderPlacedSuccessfully: () -> Unit
) {
    val context = LocalContext.current
    val shops by viewModel.shops.collectAsState()
    val products by viewModel.products.collectAsState()

    // Screen State
    var selectedShop by remember { mutableStateOf<Shop?>(null) }
    var shopDropdownExpanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Map of ProductId -> Selected Quantity
    val selectedQuantities = remember { mutableStateMapOf<Int, Int>() }
    // Map of ProductId -> Custom Price
    val customPrices = remember { mutableStateMapOf<Int, Double>() }
    
    var paidAmountStr by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

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
                quantity = qty
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
            .background(Color(0xFFF7F9FF))
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
                    text = "১. দোকান নির্বাচন করুন * (Select Shop)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(
                    expanded = shopDropdownExpanded,
                    onExpandedChange = { shopDropdownExpanded = !shopDropdownExpanded },
                    modifier = Modifier.fillMaxWidth().testTag("shop_dropdown_box")
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedShop?.name ?: "দোকান সিলেক্ট করুন...",
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shopDropdownExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("shop_dropdown_trigger"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    ExposedDropdownMenu(
                        expanded = shopDropdownExpanded,
                        onDismissRequest = { shopDropdownExpanded = false }
                    ) {
                        if (shops.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("কোনো দোকান পাওয়া যায়নি! প্রথমে দোকান যোগ করুন।") },
                                onClick = { shopDropdownExpanded = false }
                            )
                        } else {
                            shops.forEach { shop ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(shop.name, fontWeight = FontWeight.Bold)
                                            if (shop.address.isNotBlank()) {
                                                Text(shop.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedShop = shop
                                        shopDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Choose Products & Quantities
            item {
                Text(
                    text = "২. প্রোডাক্ট ও পরিমাণ সিলেক্ট করুন * (Select Products)",
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
                        placeholder = { Text("প্রোডাক্ট খুঁজুন... (Search Product)") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF0061A4)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF42474E))
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
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF0061A4),
                            unfocusedBorderColor = Color(0xFFC2C7CF)
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
                            Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                            Text("প্রথমে কিছু প্রোডাক্ট এন্ট্রি করে নিন!", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (filteredProducts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFC2C7CF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFBA1A1A), modifier = Modifier.size(40.dp))
                            Text("এই নামে কোনো প্রোডাক্ট পাওয়া যায়নি!", fontWeight = FontWeight.Bold, color = Color(0xFF42474E))
                        }
                    }
                }
            } else {
                items(filteredProducts, key = { it.id }) { product ->
                    val selectedQty = selectedQuantities[product.id] ?: 0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = selectedQty == 0) {
                                if (product.stock > 0) {
                                    selectedQuantities[product.id] = 1
                                } else {
                                    Toast.makeText(context, "এই প্রোডাক্টটি স্টকে নেই!", Toast.LENGTH_SHORT).show()
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedQty > 0) Color(0xFFE8F0FE) else Color.White
                        ),
                        border = BorderStroke(
                            width = if (selectedQty > 0) 1.5.dp else 1.dp,
                            color = if (selectedQty > 0) Color(0xFF0061A4) else Color(0xFFC2C7CF)
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
                                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    val currentPriceVal = customPrices[product.id] ?: product.price
                                    Text(
                                        text = "৳${String.format("%,.2f", currentPriceVal)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "স্টক: ${product.stock} টি",
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
                                            if (selectedQty > 0) {
                                                selectedQuantities[product.id] = selectedQty - 1
                                            }
                                        },
                                        enabled = selectedQty > 0,
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
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    IconButton(
                                        onClick = {
                                            if (selectedQty < product.stock) {
                                                selectedQuantities[product.id] = selectedQty + 1
                                            } else {
                                                Toast.makeText(context, "স্টকের চেয়ে বেশি দেওয়া সম্ভব নয়!", Toast.LENGTH_SHORT).show()
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
                            if (selectedQty > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom Price Manual Field
                                    var priceInputVal by remember(product.id) {
                                        mutableStateOf(customPrices[product.id]?.toString() ?: product.price.toString())
                                    }
                                    OutlinedTextField(
                                        value = priceInputVal,
                                        onValueChange = { newValue ->
                                            priceInputVal = newValue
                                            val parsed = newValue.toDoubleOrNull()
                                            if (parsed != null && parsed >= 0.0) {
                                                customPrices[product.id] = parsed
                                            }
                                        },
                                        label = { Text("দাম (৳)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedBorderColor = Color(0xFF0061A4),
                                            unfocusedBorderColor = Color(0xFFC2C7CF)
                                        )
                                    )

                                    // Custom Quantity Manual Field
                                    var qtyInputVal by remember(selectedQty) {
                                        mutableStateOf(selectedQty.toString())
                                    }
                                    OutlinedTextField(
                                        value = qtyInputVal,
                                        onValueChange = { newValue ->
                                            qtyInputVal = newValue
                                            val parsed = newValue.toIntOrNull()
                                            if (parsed != null) {
                                                if (parsed <= product.stock) {
                                                    selectedQuantities[product.id] = parsed
                                                } else {
                                                    Toast.makeText(context, "স্টকের চেয়ে বেশি দেওয়া সম্ভব নয়!", Toast.LENGTH_SHORT).show()
                                                    selectedQuantities[product.id] = product.stock
                                                    qtyInputVal = product.stock.toString()
                                                }
                                            } else if (newValue.isEmpty()) {
                                                selectedQuantities[product.id] = 0
                                            }
                                        },
                                        label = { Text("পরিমাণ (টি)", fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedBorderColor = Color(0xFF0061A4),
                                            unfocusedBorderColor = Color(0xFFC2C7CF)
                                        )
                                    )
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFC2C7CF))
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
                        text = "মোট আইটেম: ${selectedItemsList.size} টি",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "মোট বিল: ৳${String.format("%,.2f", totalAmount)}",
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
                        label = { Text("আদায়কৃত টাকা (Received)") },
                        placeholder = { Text("৳০.০০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("order_paid_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("অবশিষ্ট বকেয়া", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "৳${String.format("%,.2f", dueAmount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (dueAmount > 0) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                        )
                    }
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("অন্যান্য মন্তব্য / নোট (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন- গাড়ি ভাড়া ৫০ টাকা") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Confirm action button
                Button(
                    onClick = {
                        val shop = selectedShop
                        if (shop == null) {
                            Toast.makeText(context, "দয়া করে প্রথমে দোকান সিলেক্ট করুন!", Toast.LENGTH_LONG).show()
                        } else if (selectedItemsList.isEmpty()) {
                            Toast.makeText(context, "দয়া করে অন্ততঃ ১টি প্রোডাক্ট সিলেক্ট করুন!", Toast.LENGTH_LONG).show()
                        } else {
                            // Save Order
                            viewModel.createOrder(
                                shopId = shop.id,
                                shopName = shop.name,
                                items = selectedItemsList,
                                totalAmount = totalAmount,
                                paidAmount = paidAmount,
                                remarks = remarks
                            )
                            Toast.makeText(context, "বিল সফলভাবে সংরক্ষণ ও স্টক আপডেট করা হয়েছে!", Toast.LENGTH_LONG).show()
                            onOrderPlacedSuccessfully()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_order_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("অর্ডার নিশ্চিত ও বিল তৈরি করুন", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
