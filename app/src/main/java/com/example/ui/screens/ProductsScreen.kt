package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Product
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.t
import com.example.ui.tNonCompose

@Composable
fun ProductsScreen(
    viewModel: AppViewModel
) {
    val products by viewModel.products.collectAsState()
    val isEnglish by viewModel.isEnglish.collectAsState()
    val showOnlyLowStock by viewModel.showOnlyLowStockInProducts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedProductForEdit by remember { mutableStateOf<Product?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf<Product?>(null) }

    val filteredProducts = remember(products, searchQuery, showOnlyLowStock) {
        var list = if (searchQuery.isBlank()) {
            products
        } else {
            products.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
        
        if (showOnlyLowStock) {
            list = list.filter { it.stock <= 5 }
        }
        
        list.sortedWith(compareBy<Product> { it.stock > 5 }.thenBy { it.stock }.thenBy { it.name })
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("products_screen")
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
                label = { Text(t(viewModel, "প্রোডাক্ট খুঁজুন (Search Products)", "Search Products")) },
                placeholder = { Text(t(viewModel, "প্রোডাক্টের নাম লিখুন...", "Enter product name...")) },
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
                    .testTag("product_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = showOnlyLowStock,
                    onClick = { viewModel.setShowOnlyLowStockInProducts(!showOnlyLowStock) },
                    label = {
                        Text(
                            text = if (isEnglish) "Only Low Stock (≤ 5)" else "শুধু লো স্টক (≤ ৫)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        if (showOnlyLowStock) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Products Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = t(viewModel, "মোট প্রোডাক্ট: ${filteredProducts.size} টি", "Total Products: ${filteredProducts.size}"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Products List
            if (filteredProducts.isEmpty()) {
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
                            imageVector = Icons.Outlined.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) {
                                t(viewModel, "কোনো প্রোডাক্ট পাওয়া যায়নি!", "No products found!")
                            } else {
                                t(viewModel, "কোনো প্রোডাক্ট এন্ট্রি করা হয়নি!", "No products entered yet!")
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = t(viewModel, "নতুন প্রোডাক্ট যোগ করতে নিচের '+' বাটনে চাপুন।", "Press the '+' button below to add a new product."),
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
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductItemRow(
                            product = product,
                            isEnglish = isEnglish,
                            onEditClick = {
                                selectedProductForEdit = product
                                showAddEditDialog = true
                            },
                            onDeleteClick = {
                                showDeleteConfirmation = product
                            },
                            onQuickStockUpdate = { quantityDiff ->
                                val newStock = (product.stock + quantityDiff).coerceAtLeast(0)
                                viewModel.updateProduct(product.copy(stock = newStock))
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Add Product
        FloatingActionButton(
            onClick = {
                selectedProductForEdit = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_product_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Outlined.Add, contentDescription = t(viewModel, "প্রোডাক্ট যোগ করুন", "Add Product"))
        }
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        AddEditProductDialog(
            product = selectedProductForEdit,
            isEnglish = isEnglish,
            onDismiss = { showAddEditDialog = false },
            onConfirm = { name, price, stock, description, unit ->
                if (selectedProductForEdit == null) {
                    viewModel.addProduct(name, price, stock, description, unit)
                } else {
                    viewModel.updateProduct(
                        selectedProductForEdit!!.copy(
                            name = name,
                            price = price,
                            stock = stock,
                            description = description,
                            unit = unit
                        )
                    )
                }
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    showDeleteConfirmation?.let { product ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text(t(viewModel, "প্রোডাক্ট মুছে ফেলার সতর্কতা", "Delete Product Warning")) },
            text = { Text(t(viewModel, "'${product.name}' মুছে ফেললে এটি আর পুনরুদ্ধার করা যাবে না। আপনি কি নিশ্চিত?", "Deleting '${product.name}' is permanent. Are you sure?")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProduct(product)
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
}

@Composable
fun ProductItemRow(
    product: Product,
    isEnglish: Boolean,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onQuickStockUpdate: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name & Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (product.description.isNotBlank()) {
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
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

            // Body: Price, Stock, and Quick Incrementor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isEnglish) "Price: ৳${String.format("%,.2f", product.price)}" else "মূল্য: ৳${String.format("%,.2f", product.price)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val isLowStock = product.stock <= 5
                    val isDark = isSystemInDarkTheme()
                    val stockBg = if (product.stock == 0) {
                        if (isDark) Color(0xFF8C1D18).copy(alpha = 0.2f) else Color(0xFFF9DEDC)
                    } else if (isLowStock) {
                        if (isDark) Color(0xFFE65100).copy(alpha = 0.2f) else Color(0xFFFFF3E0)
                    } else {
                        if (isDark) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFD2E8D1)
                    }
                    val stockColor = if (product.stock == 0) {
                        if (isDark) Color(0xFFF9DEDC) else Color(0xFF410E0B)
                    } else if (isLowStock) {
                        if (isDark) Color(0xFFFFD180) else Color(0xFFE65100)
                    } else {
                        if (isDark) Color(0xFFC8E6C9) else Color(0xFF0A210B)
                    }
                    val stockText = if (product.stock == 0) {
                        if (isEnglish) "Out of Stock" else "স্টক শেষ"
                    } else {
                        if (isEnglish) "Stock: ${product.stock} pcs" else "স্টক: ${product.stock} টি"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(stockBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stockText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = stockColor
                        )
                    }
                }

                // Quick Stock adjustment buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(onClick = { onQuickStockUpdate(-1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Remove, contentDescription = "Deduct Stock", modifier = Modifier.size(16.dp))
                    }
                    Text(
                        text = if (isEnglish) "Update Stock" else "স্টক আপডেট",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = { onQuickStockUpdate(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Add Stock", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    product: Product?,
    isEnglish: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Int, String, String) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var priceStr by remember { mutableStateOf(product?.price?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var stockStr by remember { mutableStateOf(product?.stock?.let { if (it == 0) "" else it.toString() } ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    
    val units = listOf("Pcs", "Packet", "Sack", "Kg", "Gram")
    var selectedUnit by remember { mutableStateOf(product?.unit ?: units[0]) }
    var expanded by remember { mutableStateOf(false) }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (product == null) {
                    if (isEnglish) "Add New Product" else "নতুন প্রোডাক্ট যোগ করুন"
                } else {
                    if (isEnglish) "Edit Product Details" else "প্রোডাক্টের তথ্য এডিট করুন"
                }
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isEnglish) "Product Name *" else "প্রোডাক্টের নাম *") },
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text(if (isEnglish) "Selling Price (TK) *" else "বিক্রয় মূল্য (টাকা) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("product_price_input"),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text(if (isEnglish) "Stock *" else "স্টক *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_stock_input"),
                        singleLine = true
                    )
                    
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isEnglish) "Unit" else "একক") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true },
                            colors = OutlinedTextFieldDefaults.colors()
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            units.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        selectedUnit = unit
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isEnglish) "Product Description (Optional)" else "প্রোডাক্টের বিবরণ (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                if (isError) {
                    Text(
                        text = if (isEnglish) "Please fill in all marked (*) fields correctly." else "অনুগ্রহ করে সব তারকা (*) চিহ্নিত ঘরগুলো সঠিকভাবে পূরণ করুন।",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = priceStr.toDoubleOrNull() ?: 0.0
                    val parsedStock = stockStr.toIntOrNull() ?: 0
                    if (name.isBlank() || parsedPrice <= 0 || parsedStock < 0) {
                        isError = true
                    } else {
                        onConfirm(name, parsedPrice, parsedStock, description, selectedUnit)
                    }
                },
                modifier = Modifier.testTag("product_dialog_confirm")
            ) {
                Text(if (isEnglish) "Save" else "সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Cancel" else "বাতিল")
            }
        }
    )
}
