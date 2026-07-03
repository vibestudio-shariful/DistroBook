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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Order
import com.example.ui.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrderHistoryScreen(
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val orders by viewModel.orders.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = All, 1 = Due, 2 = Paid
    
    var selectedOrderDetails by remember { mutableStateOf<Order?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Order?>(null) }
    var showPaymentUpdateDialog by remember { mutableStateOf<Order?>(null) }

    val filteredOrders = remember(orders, searchQuery, selectedTab) {
        orders.filter { order ->
            val matchQuery = order.shopName.contains(searchQuery, ignoreCase = true) || order.remarks.contains(searchQuery, ignoreCase = true)
            val matchStatus = when (selectedTab) {
                1 -> !order.isPaid // Due
                2 -> order.isPaid  // Paid
                else -> true      // All
            }
            matchQuery && matchStatus
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FF))
            .testTag("orders_screen")
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
                label = { Text("ক্রেতা বা বিল খুঁজুন (Search Bills)") },
                placeholder = { Text("দোকানের নাম বা বিলের মন্তব্য...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("order_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("সব বিল", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val duesCount = orders.count { !it.isPaid }
                        Text(if (duesCount > 0) "বকেয়া ($duesCount)" else "বকেয়া", fontWeight = FontWeight.Bold)
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("পরিশোধিত", fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bills Header
            Text(
                text = "মোট বিলের সংখ্যা: ${filteredOrders.size} টি",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Orders List
            if (filteredOrders.isEmpty()) {
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
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "কোনো বিল পাওয়া যায়নি!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredOrders, key = { it.id }) { order ->
                        RecentOrderRow(
                            order = order,
                            onClick = { selectedOrderDetails = order }
                        )
                    }
                }
            }
        }
    }

    // Bill Invoice Receipt Dialog
    selectedOrderDetails?.let { order ->
        val formatter = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale.getDefault())
        val dateStr = formatter.format(Date(order.timestamp))

        AlertDialog(
            onDismissRequest = { selectedOrderDetails = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "মেমো / ইনভয়েস", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = { selectedOrderDetails = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Receipt Header Slip
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "মেমো নং: #${order.id}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "ক্রেতা: ${order.shopName}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(text = "তারিখ: $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            if (order.remarks.isNotBlank()) {
                                Text(text = "নোট: ${order.remarks}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Itemized Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "প্রোডাক্টের নাম", modifier = Modifier.weight(1.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(text = "মূল্য", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(text = "পরিমাণ", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(text = "মোট", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    // Itemized Items Rows
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    ) {
                        items(order.items) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = item.productName, modifier = Modifier.weight(1.8f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(text = "৳${String.format("%,.0f", item.price)}", modifier = Modifier.weight(0.8f), fontSize = 12.sp, textAlign = TextAlign.End)
                                Text(text = "${item.quantity} টি", modifier = Modifier.weight(0.8f), fontSize = 12.sp, textAlign = TextAlign.End)
                                Text(text = "৳${String.format("%,.0f", item.totalLinePrice)}", modifier = Modifier.weight(1f), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            }
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }

                    // Totals Breakdowns
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "মোট বিল (Total Bill):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "৳${String.format("%,.2f", order.totalAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "টাকা আদায় (Collected):", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            Text(text = "৳${String.format("%,.2f", order.paidAmount)}", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Black)
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val isDue = order.dueAmount > 0
                            val col = if (isDue) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                            Text(text = "বকেয়া (Outstanding Due):", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = col)
                            Text(text = "৳${String.format("%,.2f", order.dueAmount)}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = col)
                        }
                    }

                    // Action buttons inside Dialog
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (order.dueAmount > 0) {
                            Button(
                                onClick = {
                                    showPaymentUpdateDialog = order
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("বকেয়া আদায়", fontSize = 12.sp)
                            }
                        }
                        
                        OutlinedButton(
                            onClick = {
                                showDeleteConfirmation = order
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("বিল ডিলিট", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedOrderDetails = null }) {
                    Text("মেমো বন্ধ করুন")
                }
            }
        )
    }

    // Cash Collection Update Dialog
    showPaymentUpdateDialog?.let { order ->
        var collectStr by remember { mutableStateOf("") }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPaymentUpdateDialog = null },
            title = { Text("বকেয়া টাকা আদায় আপডেট করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "দোকানের নাম: ${order.shopName}", fontWeight = FontWeight.Bold)
                    Text(text = "বিলের মোট বকেয়া: ৳${String.format("%,.2f", order.dueAmount)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = collectStr,
                        onValueChange = { collectStr = it },
                        label = { Text("আদায়কৃত টাকার পরিমাণ *") },
                        placeholder = { Text("যেমন- ৫০০.০০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("due_collect_input"),
                        singleLine = true
                    )

                    if (isError) {
                        Text(
                            text = "অনুগ্রহ করে বকেয়া টাকা বা তার চেয়ে কম পরিমাণ সঠিক সংখ্যা লিখুন।",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = collectStr.toDoubleOrNull() ?: 0.0
                        if (amt <= 0 || amt > order.dueAmount) {
                            isError = true
                        } else {
                            viewModel.updateOrderPayment(order, amt)
                            Toast.makeText(context, "বকেয়া আদায় আপডেট সম্পন্ন হয়েছে!", Toast.LENGTH_SHORT).show()
                            showPaymentUpdateDialog = null
                            selectedOrderDetails = null // close receipt so it refreshes or let it close
                        }
                    },
                    modifier = Modifier.testTag("collect_confirm")
                ) {
                    Text("আদায় নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentUpdateDialog = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Delete Confirmation
    showDeleteConfirmation?.let { order ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("বিল মুছে ফেলার সতর্কতা") },
            text = { Text("মেমো নং #${order.id} চিরতরে ডিলিট করতে চান? এটি আর দেখা যাবে না।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteOrder(order)
                        showDeleteConfirmation = null
                        selectedOrderDetails = null // close invoice view too
                        Toast.makeText(context, "বিল মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
