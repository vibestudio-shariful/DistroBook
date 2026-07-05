package com.example.ui.screens

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
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
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.Order
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.ReportFilter
import com.example.ui.t
import com.example.ui.tNonCompose
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrderHistoryScreen(
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val orders by viewModel.orders.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    val businessNameVal by viewModel.businessName.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    val selectedTab by viewModel.historySelectedTab.collectAsState()
    var historyFilter by remember { mutableStateOf<ReportFilter>(ReportFilter.AllTime) }
    
    var orderToExport by remember { mutableStateOf<Order?>(null) }
    var selectedOrderDetails by remember { mutableStateOf<Order?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Order?>(null) }
    var showPaymentUpdateDialog by remember { mutableStateOf<Order?>(null) }
    
    val filteredOrders = remember(orders, searchQuery, selectedTab, historyFilter) {
        orders.filter { order ->
            val matchQuery = order.shopName.contains(searchQuery, ignoreCase = true) || order.remarks.contains(searchQuery, ignoreCase = true)
            val matchStatus = when (selectedTab) {
                1 -> !order.isPaid // Due
                2 -> order.isPaid  // Paid
                else -> true      // All
            }
            val matchDate = when (val filter = historyFilter) {
                is ReportFilter.AllTime -> true
                is ReportFilter.Today -> viewModel.isToday(order.timestamp)
                is ReportFilter.SpecificDate -> viewModel.isSameDay(order.timestamp, filter.date)
                is ReportFilter.SpecificMonth -> viewModel.isSameMonth(order.timestamp, filter.year, filter.month)
            }
            matchQuery && matchStatus && matchDate
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            val order = orderToExport
            try {
                val pdfDoc = if (order != null) {
                    generateInvoicePdf(order, businessNameVal, isEnglish)
                } else {
                    val reportTitle = getReportTitle(selectedTab, historyFilter, isEnglish)
                    generateReportPdf(filteredOrders, businessNameVal, reportTitle, isEnglish)
                }
                context.contentResolver.openOutputStream(it)?.use { fos ->
                    pdfDoc.writeTo(fos)
                }
                pdfDoc.close()
                Toast.makeText(context, if (isEnglish) "PDF saved successfully" else "পিডিএফ সংরক্ষণ করা হয়েছে", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
                label = { Text(t(viewModel, "ক্রেতা বা বিল খুঁজুন (Search Bills)", "Search Bills")) },
                placeholder = { Text(t(viewModel, "দোকানের নাম বা বিলের মন্তব্য...", "Shop name or bill remarks...")) },
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
                    onClick = { viewModel.historySelectedTab.value = 0 },
                    text = { Text(if (isEnglish) "All Bills" else "সব বিল", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { viewModel.historySelectedTab.value = 1 },
                    text = {
                        val duesCount = orders.count { !it.isPaid }
                        val duesLabel = if (isEnglish) {
                            if (duesCount > 0) "Due ($duesCount)" else "Due"
                        } else {
                            if (duesCount > 0) "বকেয়া ($duesCount)" else "বকেয়া"
                        }
                        Text(duesLabel, fontWeight = FontWeight.Bold)
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { viewModel.historySelectedTab.value = 2 },
                    text = { Text(if (isEnglish) "Paid" else "পরিশোধিত", fontWeight = FontWeight.Bold) }
                )
            }

            var showMonthPicker by remember { mutableStateOf(false) }

            // Trigger standard DatePickerDialog
            val calendar = Calendar.getInstance()
            val datePickerDialog = remember {
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedCal = Calendar.getInstance()
                        selectedCal.set(year, month, dayOfMonth)
                        historyFilter = ReportFilter.SpecificDate(selectedCal.time)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
            }

            if (showMonthPicker) {
                MonthPickerDialog(
                    onDismissRequest = { showMonthPicker = false },
                    onMonthSelected = { year, month ->
                        historyFilter = ReportFilter.SpecificMonth(year, month)
                        showMonthPicker = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // History Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Time Chip
                val isAllSelected = historyFilter == ReportFilter.AllTime
                FilterChip(
                    selected = isAllSelected,
                    onClick = { historyFilter = ReportFilter.AllTime },
                    label = { Text(if (isEnglish) "All Time" else "সব সময়", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1.1f)
                )

                // Today Chip
                val isTodaySelected = historyFilter == ReportFilter.Today
                FilterChip(
                    selected = isTodaySelected,
                    onClick = { historyFilter = ReportFilter.Today },
                    label = { Text(if (isEnglish) "Today" else "আজ", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(0.9f)
                )

                // Specific Date Chip
                val isDateSelected = historyFilter is ReportFilter.SpecificDate
                val dateLabel = if (isDateSelected) {
                    val date = (historyFilter as ReportFilter.SpecificDate).date
                    SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
                } else {
                    if (isEnglish) "Date" else "তারিখ"
                }
                FilterChip(
                    selected = isDateSelected,
                    onClick = { datePickerDialog.show() },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(dateLabel, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1.1f)
                )

                // Specific Month Chip
                val isMonthSelected = historyFilter is ReportFilter.SpecificMonth
                val monthLabel = if (isMonthSelected) {
                    val filter = historyFilter as ReportFilter.SpecificMonth
                    if (isEnglish) {
                        val monthNamesEn = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        "${monthNamesEn[filter.month]} '${filter.year.toString().takeLast(2)}"
                    } else {
                        val monthNames = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রি", "মে", "জুন", "জুলাই", "আগ", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
                        "${monthNames[filter.month]} '${filter.year.toString().takeLast(2)}"
                    }
                } else {
                    if (isEnglish) "Month" else "মাস"
                }
                FilterChip(
                    selected = isMonthSelected,
                    onClick = { showMonthPicker = true },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(monthLabel, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.weight(1.1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bills Header
            Text(
                text = if (isEnglish) "Total Bills: ${filteredOrders.size}" else "মোট বিলের সংখ্যা: ${filteredOrders.size} টি",
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
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (isEnglish) "No bills found!" else "কোনো বিল পাওয়া যায়নি!",
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
        // Floating Action Button to Print Report
        FloatingActionButton(
            onClick = {
                // Generate Report PDF
                orderToExport = null // Indicate it's a report
                val fileName = "Report_${SimpleDateFormat("dd_MM_yyyy_HH_mm", Locale.getDefault()).format(Date())}.pdf"
                pdfLauncher.launch(fileName)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary
        ) {
            Icon(Icons.Outlined.Print, contentDescription = "Print Report")
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
                    Text(text = if (isEnglish) "Memo / Invoice" else "মেমো / ইনভয়েস", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    IconButton(onClick = { selectedOrderDetails = null }) {
                        Icon(Icons.Outlined.Close, contentDescription = if (isEnglish) "Close" else "বন্ধ করুন")
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
                            Text(text = if (isEnglish) "Memo No: #${order.id}" else "মেমো নং: #${order.id}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(text = if (isEnglish) "Shop: ${order.shopName}" else "ক্রেতা: ${order.shopName}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(text = if (isEnglish) "Date: $dateStr" else "তারিখ: $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            if (order.remarks.isNotBlank()) {
                                Text(text = if (isEnglish) "Note: ${order.remarks}" else "নোট: ${order.remarks}", fontSize = 11.sp, fontWeight = FontWeight.Medium)
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
                        Text(text = if (isEnglish) "Product Name" else "প্রোডাক্টের নাম", modifier = Modifier.weight(1.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(text = if (isEnglish) "Price" else "মূল্য", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(text = if (isEnglish) "Qty" else "পরিমাণ", modifier = Modifier.weight(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(text = if (isEnglish) "Total" else "মোট", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onPrimaryContainer)
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
                                Text(text = if (isEnglish) "${item.quantity} pcs" else "${item.quantity} টি", modifier = Modifier.weight(0.8f), fontSize = 12.sp, textAlign = TextAlign.End)
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
                            Text(text = if (isEnglish) "Total Bill:" else "মোট বিল:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(text = "৳${String.format("%,.2f", order.totalAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = if (isEnglish) "Collected:" else "টাকা আদায়:", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            Text(text = "৳${String.format("%,.2f", order.paidAmount)}", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Black)
                        }
                        
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            val isDue = order.dueAmount > 0
                            val col = if (isDue) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                            Text(text = if (isEnglish) "Outstanding Due:" else "বকেয়া:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = col)
                            Text(text = "৳${String.format("%,.2f", order.dueAmount)}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = col)
                        }
                    }

                    Button(
                        onClick = {
                            orderToExport = order
                            val fileName = "${order.shopName}_${SimpleDateFormat("dd_MM_yyyy_HH_mm", Locale.getDefault()).format(Date(order.timestamp))}.pdf"
                            pdfLauncher.launch(fileName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Print,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "Print Invoice" else "মেমো প্রিন্ট করুন",
                            fontWeight = FontWeight.Bold
                        )
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
                                Icon(Icons.Outlined.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEnglish) "Collect Due" else "বকেয়া আদায়", fontSize = 12.sp)
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
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEnglish) "Delete Bill" else "বিল ডিলিট", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedOrderDetails = null }) {
                    Text(if (isEnglish) "Close Memo" else "মেমো বন্ধ করুন")
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
            title = { Text(if (isEnglish) "Collect Outstanding Due" else "বকেয়া টাকা আদায় আপডেট করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = if (isEnglish) "Shop Name: ${order.shopName}" else "দোকানের নাম: ${order.shopName}", fontWeight = FontWeight.Bold)
                    Text(text = if (isEnglish) "Outstanding Due: ৳${String.format("%,.2f", order.dueAmount)}" else "বিলের মোট বকেয়া: ৳${String.format("%,.2f", order.dueAmount)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    
                    OutlinedTextField(
                        value = collectStr,
                        onValueChange = { collectStr = it },
                        label = { Text(if (isEnglish) "Collected Amount *" else "আদায়কৃত টাকার পরিমাণ *") },
                        placeholder = { Text(if (isEnglish) "e.g. 500.00" else "যেমন- ৫০০.০০") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("due_collect_input"),
                        singleLine = true
                    )

                    if (isError) {
                        Text(
                            text = if (isEnglish) "Please enter a valid amount less than or equal to outstanding due." else "অনুগ্রহ করে বকেয়া টাকা বা তার চেয়ে কম পরিমাণ সঠিক সংখ্যা লিখুন।",
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
                            val msg = tNonCompose(isEnglish, "বকেয়া আদায় আপডেট সম্পন্ন হয়েছে!", "Due collection updated successfully!")
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            showPaymentUpdateDialog = null
                            selectedOrderDetails = null // close receipt so it refreshes or let it close
                        }
                    },
                    modifier = Modifier.testTag("collect_confirm")
                ) {
                    Text(if (isEnglish) "Confirm Collection" else "আদায় নিশ্চিত করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentUpdateDialog = null }) {
                    Text(if (isEnglish) "Cancel" else "বাতিল")
                }
            }
        )
    }

    // Delete Confirmation
    showDeleteConfirmation?.let { order ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text(t(viewModel, "বিল মুছে ফেলার সতর্কতা", "Delete Bill Warning")) },
            text = { Text(t(viewModel, "মেমো নং #${order.id} চিরতরে ডিলিট করতে চান? এটি আর দেখা যাবে না।", "Do you want to permanently delete Memo No #${order.id}? This action cannot be undone.")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteOrder(order)
                        showDeleteConfirmation = null
                        selectedOrderDetails = null // close invoice view too
                        val msg = tNonCompose(isEnglish, "বিল মুছে ফেলা হয়েছে!", "Bill deleted successfully!")
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(t(viewModel, "মুছে ফেলুন", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = null }) {
                    Text(t(viewModel, "বাতিল", "Cancel"))
                }
            }
        )
    }
}



private fun generateInvoicePdf(order: Order, businessName: String, isEnglish: Boolean): PdfDocument {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas

    val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val headerPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val boldPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    // 1. Draw Business Name Header
    canvas.drawText(businessName, 40f, 60f, headerPaint)
    
    textPaint.textSize = 10f
    textPaint.color = android.graphics.Color.GRAY
    val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(order.timestamp))
    val subtitle = if (isEnglish) "Tax Invoice / Billing Receipt" else "ট্যাক্স ইনভয়েস / বিক্রয় রশিদ"
    canvas.drawText(subtitle, 40f, 80f, textPaint)
    
    val paint = Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f }
    canvas.drawLine(40f, 95f, 555f, 95f, paint)

    // 2. Metadata Information
    textPaint.color = android.graphics.Color.BLACK
    textPaint.textSize = 11f
    
    val billToLabel = if (isEnglish) "Bill To (Shop):" else "ক্রেতা (দোকান):"
    canvas.drawText(billToLabel, 40f, 120f, boldPaint)
    canvas.drawText(order.shopName, 40f, 138f, textPaint)
    
    val invoiceNoLabel = if (isEnglish) "Invoice No: #${order.id}" else "মেমো নং: #${order.id}"
    val dateLabel = if (isEnglish) "Date: $dateStr" else "তারিখ: $dateStr"
    canvas.drawText(invoiceNoLabel, 350f, 120f, boldPaint)
    canvas.drawText(dateLabel, 350f, 138f, textPaint)

    canvas.drawLine(40f, 160f, 555f, 160f, paint)

    // 3. Grid Table of Items
    var y = 185f
    val itemHeader = if (isEnglish) "Item Name" else "পণ্য"
    val qtyHeader = if (isEnglish) "Qty" else "পরিমাণ"
    val rateHeader = if (isEnglish) "Rate" else "দর"
    val totalHeader = if (isEnglish) "Total" else "মোট"
    
    canvas.drawText(itemHeader, 40f, y, boldPaint)
    canvas.drawText(qtyHeader, 320f, y, boldPaint)
    canvas.drawText(rateHeader, 400f, y, boldPaint)
    canvas.drawText(totalHeader, 480f, y, boldPaint)

    canvas.drawLine(40f, y + 8f, 555f, y + 8f, paint)
    y += 28f

    for (item in order.items) {
        canvas.drawText("${item.productName} (${item.unit})", 40f, y, textPaint)
        canvas.drawText("${item.quantity}", 320f, y, textPaint)
        canvas.drawText(String.format("৳%,.2f", item.price), 400f, y, textPaint)
        canvas.drawText(String.format("৳%,.2f", item.totalLinePrice), 480f, y, textPaint)
        
        y += 20f
    }

    canvas.drawLine(40f, y - 5f, 555f, y - 5f, paint)
    y += 15f

    val totalAmountStr = String.format("৳%,.2f", order.totalAmount)
    val paidAmountStr = String.format("৳%,.2f", order.paidAmount)
    val dueAmountStr = String.format("৳%,.2f", order.dueAmount)

    val totalTxt = if (isEnglish) "Total Amount:" else "সর্বমোট মূল্য:"
    val paidTxt = if (isEnglish) "Paid Amount:" else "পরিশোধিত:"
    val dueTxt = if (isEnglish) "Due Amount:" else "বকেয়া:"

    canvas.drawText(totalTxt, 320f, y, boldPaint)
    canvas.drawText(totalAmountStr, 480f, y, boldPaint)
    y += 20f

    canvas.drawText(paidTxt, 320f, y, textPaint)
    canvas.drawText(paidAmountStr, 480f, y, textPaint)
    y += 20f

    boldPaint.color = if (order.dueAmount > 0) android.graphics.Color.RED else android.graphics.Color.rgb(46, 125, 50)
    canvas.drawText(dueTxt, 320f, y, boldPaint)
    canvas.drawText(dueAmountStr, 480f, y, boldPaint)
    
    y += 40f
    if (order.remarks.isNotBlank()) {
        val remarksHeader = if (isEnglish) "Remarks:" else "মন্তব্য:"
        canvas.drawText(remarksHeader, 40f, y, boldPaint)
        canvas.drawText(order.remarks, 40f, y + 16f, textPaint)
    }

    val thankYouMsg = if (isEnglish) "Thank you for your business!" else "আমাদের সাথে ব্যবসা করার জন্য ধন্যবাদ!"
    textPaint.color = android.graphics.Color.GRAY
    textPaint.textSize = 10f
    canvas.drawText(thankYouMsg, 40f, 780f, textPaint)

    // Footer
    textPaint.textSize = 9f
    textPaint.color = android.graphics.Color.DKGRAY
    canvas.drawText("Generated by Distro Book, For more info: connect.shariful@gmail.com", 40f, 820f, textPaint)

    pdfDocument.finishPage(page)
    return pdfDocument
}

private fun generateReportPdf(orders: List<Order>, businessName: String, reportTitle: String, isEnglish: Boolean): PdfDocument {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas

    val textPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }
    val headerPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val boldPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    canvas.drawText("$businessName - $reportTitle", 40f, 60f, headerPaint)
    
    var y = 100f
    
    val dateHeader = if (isEnglish) "Date" else "তারিখ"
    val shopHeader = if (isEnglish) "Shop" else "দোকান"
    val totalHeader = if (isEnglish) "Total" else "মোট"
    val dueHeader = if (isEnglish) "Due" else "বকেয়া"
    
    canvas.drawText(dateHeader, 40f, y, boldPaint)
    canvas.drawText(shopHeader, 120f, y, boldPaint)
    canvas.drawText(totalHeader, 400f, y, boldPaint)
    canvas.drawText(dueHeader, 500f, y, boldPaint)
    
    y += 20f
    
    val formatter = SimpleDateFormat("dd/MM", Locale.getDefault())
    for (order in orders) {
        canvas.drawText(formatter.format(Date(order.timestamp)), 40f, y, textPaint)
        canvas.drawText(order.shopName.take(15), 120f, y, textPaint)
        canvas.drawText(String.format("৳%,.2f", order.totalAmount), 400f, y, textPaint)
        canvas.drawText(String.format("৳%,.2f", order.dueAmount), 500f, y, textPaint)
        
        y += 20f
        if (y > 780f) {
            pdfDocument.finishPage(page)
            break
        }
    }
    
    // Footer
    textPaint.textSize = 9f
    textPaint.color = android.graphics.Color.DKGRAY
    canvas.drawText("Generated by Distro Book, For more info: connect.shariful@gmail.com", 40f, 820f, textPaint)

    pdfDocument.finishPage(page)
    return pdfDocument
}


private fun getReportTitle(selectedTab: Int, historyFilter: ReportFilter, isEnglish: Boolean): String {
    val tabLabel = when (selectedTab) {
        1 -> if (isEnglish) "Due" else "বকেয়া"
        2 -> if (isEnglish) "Paid" else "পরিশোধিত"
        else -> ""
    }
    val filterLabel = when (val filter = historyFilter) {
        is ReportFilter.AllTime -> if (isEnglish) "All Time" else "সব সময়ের"
        is ReportFilter.Today -> if (isEnglish) "Today" else "দৈনিক"
        is ReportFilter.SpecificDate -> if (isEnglish) "Specific Date" else "নির্দিষ্ট তারিখের"
        is ReportFilter.SpecificMonth -> if (isEnglish) "Monthly" else "মাসিক"
    }
    return if (isEnglish) "$filterLabel $tabLabel Report" else "$filterLabel $tabLabel রিপোর্ট"
}

private fun openPdfFile(context: android.content.Context, file: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Could not open PDF for printing", Toast.LENGTH_SHORT).show()
    }
}
