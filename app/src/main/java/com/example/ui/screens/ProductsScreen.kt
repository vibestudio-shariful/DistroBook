package com.example.ui.screens

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

            var isInitialAdVisible by remember { mutableStateOf(false) }
            AdBanner(onVisibilityChanged = { isInitialAdVisible = it })
            if (isInitialAdVisible) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Products List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (filteredProducts.isEmpty()) {
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
                                    imageVector = Icons.Outlined.Inventory,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Text(
                                    text = if (searchQuery.isNotEmpty()) {
                                        t(viewModel, "কোনো প্রোডাক্ট পাওয়া যায়নি!", "No products found!")
                                    } else {
                                        t(viewModel, "কোনো প্রোডাক্ট এন্ট্রি করা হয়নি!", "No products entered yet!")
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = t(viewModel, "নতুন প্রোডাক্ট যোগ করতে নিচের '+' বাটনে চাপুন।", "Press the '+' button below to add a new product."),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                } else {
                    filteredProducts.forEachIndexed { index, product ->
                        item(key = "product_${product.id}") {
                            Column {
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
                                
                                if ((index + 1) % 10 == 0 && index < filteredProducts.size - 1) {
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
            onConfirm = { name, price, purchasePrice, stock, description, unit ->
                if (selectedProductForEdit == null) {
                    viewModel.addProduct(name, price, stock, description, unit, purchasePrice)
                } else {
                    viewModel.updateProduct(
                        selectedProductForEdit!!.copy(
                            name = name,
                            price = price,
                            purchasePrice = purchasePrice,
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
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (product.description.isNotBlank()) {
                            Text(
                                text = product.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Quick Stock adjustment buttons
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(onClick = { onQuickStockUpdate(-1) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Deduct Stock",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Text(
                            text = product.stock.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = { onQuickStockUpdate(1) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Stock",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "৳${String.format("%,.2f", product.price)} / ${product.unit}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
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
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
    onConfirm: (String, Double, Double, Int, String, String) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var priceStr by remember { mutableStateOf(product?.price?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var purchasePriceStr by remember { mutableStateOf(product?.purchasePrice?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var stockStr by remember { mutableStateOf(product?.stock?.let { if (it == 0) "" else it.toString() } ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    
    val units = listOf("Pcs", "Packet", "Sack", "Kg", "Gram", "Liter", "Dozen")
    var selectedUnit by remember { mutableStateOf(product?.unit ?: units[0]) }
    
    var expanded by remember { mutableStateOf(false) }

    var isError by remember { mutableStateOf(false) }

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
                    imageVector = if (product == null) Icons.Default.ShoppingCart else Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = if (product == null) {
                        if (isEnglish) "Add New Product" else "নতুন প্রোডাক্ট যোগ করুন"
                    } else {
                        if (isEnglish) "Edit Product Details" else "প্রোডাক্টের তথ্য এডিট করুন"
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isEnglish) "Product Name *" else "প্রোডাক্টের নাম *") },
                    modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text(if (isEnglish) "Selling Price (TK) *" else "বিক্রয় মূল্য (টাকা) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("product_price_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = purchasePriceStr,
                    onValueChange = { purchasePriceStr = it },
                    label = { Text(if (isEnglish) "Purchase Price (TK) (Optional)" else "ক্রয় মূল্য (টাকা) (ঐচ্ছিক)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().testTag("product_purchase_price_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text(if (isEnglish) "Stock *" else "স্টক *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_stock_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(1.5f)
                    ) {
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
                                .menuAnchor(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            units.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        selectedUnit = unit
                                        expanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
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
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                if (isError) {
                    Text(
                        text = if (isEnglish) "Please fill in all marked (*) fields correctly." else "অনুগ্রহ করে সব তারকা (*) চিহ্নিত ঘরগুলো সঠিকভাবে পূরণ করুন।",
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
                    val parsedPrice = priceStr.toDoubleOrNull() ?: 0.0
                    val parsedPurchasePrice = purchasePriceStr.toDoubleOrNull() ?: 0.0
                    val parsedStock = stockStr.toIntOrNull() ?: 0
                    if (name.isBlank() || parsedPrice <= 0 || parsedStock < 0) {
                        isError = true
                    } else {
                        onConfirm(name, parsedPrice, parsedPurchasePrice, parsedStock, description, selectedUnit)
                    }
                },
                modifier = Modifier.testTag("product_dialog_confirm"),
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
