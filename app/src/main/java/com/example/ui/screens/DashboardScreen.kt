package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.ui.viewmodel.ReportFilter
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.net.Uri
import com.example.R
import com.example.ui.t
import com.example.ui.tNonCompose
import com.example.ui.viewmodel.AppViewModel
import com.example.data.Order
import com.example.ui.theme.PaidGreenBg
import com.example.ui.theme.PaidGreenText
import com.example.ui.theme.DueRedBg
import com.example.ui.theme.DueRedText
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onCreateOrderClick: () -> Unit,
    onManageProductsClick: () -> Unit,
    onManageShopsClick: () -> Unit,
    onOrderClick: (Order) -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val recentOrders by viewModel.dashboardOrders.collectAsState()
    val shops by viewModel.shops.collectAsState()
    val userNameVal by viewModel.userName.collectAsState()
    val businessNameVal by viewModel.businessName.collectAsState()
    val dashboardFilterVal by viewModel.dashboardFilter.collectAsState()
    
    val isEnglish by viewModel.isEnglish.collectAsState()
    
    val salesCardTitle = when (dashboardFilterVal) {
        is ReportFilter.AllTime -> if (isEnglish) "All-time Total Sales" else "সব সময়ের মোট বিক্রি"
        is ReportFilter.Today -> if (isEnglish) "Today's Total Sales" else "আজকের মোট বিক্রি"
        is ReportFilter.SpecificDate -> {
            val date = (dashboardFilterVal as ReportFilter.SpecificDate).date
            val sdf = SimpleDateFormat("dd MMM yyyy", if (isEnglish) Locale.ENGLISH else Locale.getDefault())
            if (isEnglish) "Total Sales of ${sdf.format(date)}" else "${sdf.format(date)}-এর মোট বিক্রি"
        }
        is ReportFilter.SpecificMonth -> {
            val filter = dashboardFilterVal as ReportFilter.SpecificMonth
            val monthNamesBn = listOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")
            val monthNamesEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
            if (isEnglish) "Total Sales of ${monthNamesEn[filter.month]} ${filter.year}" else "${monthNamesBn[filter.month]} ${filter.year}-এর মোট বিক্রি"
        }
    }

    val formattedSales = String.format("৳%,.2f", stats.totalSales)
    val formattedCollected = String.format("৳%,.2f", stats.totalCollected)
    val formattedDue = String.format("৳%,.2f", stats.totalDue)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Professional User Profile Header Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                border = null
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProfileClick() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Image
                    val userAvatarPath by viewModel.userAvatarPath.collectAsState()
                    val avatarModel = userAvatarPath ?: R.drawable.img_user_avatar
                    AsyncImage(
                        model = avatarModel,
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .border(2.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f), RoundedCornerShape(27.dp)),
                        contentScale = ContentScale.Crop
                    )

                    // User text information
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = t(viewModel, "সরবরাহকারী ড্যাশবোর্ড", "Supplier Dashboard"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userNameVal,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Store,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Text(
                                text = businessNameVal,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Edit Profile icon indicator
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Edit Profile",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Report Filter Section
        item {
            val context = LocalContext.current
            var showMonthPicker by remember { mutableStateOf(false) }

            // Trigger standard DatePickerDialog
            val calendar = Calendar.getInstance()
            val datePickerDialog = remember {
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedCal = Calendar.getInstance()
                        selectedCal.set(year, month, dayOfMonth)
                        viewModel.setDashboardFilter(ReportFilter.SpecificDate(selectedCal.time))
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
                        viewModel.setDashboardFilter(ReportFilter.SpecificMonth(year, month))
                        showMonthPicker = false
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = t(viewModel, "রিপোর্ট ফিল্টার (Filter Reports)", "Report Filter"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (dashboardFilterVal != ReportFilter.AllTime) {
                        Text(
                            text = t(viewModel, "ফিল্টার রিসেট", "Reset Filter"),
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.setDashboardFilter(ReportFilter.AllTime) }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // All Time Chip
                    val isAllSelected = dashboardFilterVal == ReportFilter.AllTime
                    FilterChip(
                        selected = isAllSelected,
                        onClick = { viewModel.setDashboardFilter(ReportFilter.AllTime) },
                        label = { Text(t(viewModel, "সব সময়", "All Time"), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Today Chip
                    val isTodaySelected = dashboardFilterVal == ReportFilter.Today
                    FilterChip(
                        selected = isTodaySelected,
                        onClick = { viewModel.setDashboardFilter(ReportFilter.Today) },
                        label = { Text(t(viewModel, "আজ", "Today"), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(0.8f)
                    )

                    // Specific Date Chip
                    val isDateSelected = dashboardFilterVal is ReportFilter.SpecificDate
                    val dateLabel = if (isDateSelected) {
                        val date = (dashboardFilterVal as ReportFilter.SpecificDate).date
                        SimpleDateFormat("dd MMM", if (isEnglish) Locale.ENGLISH else Locale.getDefault()).format(date)
                    } else {
                        tNonCompose(isEnglish, "তারিখ", "Date")
                    }
                    FilterChip(
                        selected = isDateSelected,
                        onClick = { datePickerDialog.show() },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(dateLabel, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    val isMonthSelected = dashboardFilterVal is ReportFilter.SpecificMonth
                    val monthLabel = if (isMonthSelected) {
                        val filter = dashboardFilterVal as ReportFilter.SpecificMonth
                        val monthNamesBn = listOf("জানু", "ফেব্রু", "মার্চ", "এপ্রি", "মে", "জুন", "জুলাই", "আগ", "সেপ্টে", "অক্টো", "নভে", "ডিসে")
                        val monthNamesEn = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                        val monthStr = if (isEnglish) monthNamesEn[filter.month] else monthNamesBn[filter.month]
                        "$monthStr '${filter.year.toString().takeLast(2)}"
                    } else {
                        tNonCompose(isEnglish, "মাস", "Month")
                    }
                    FilterChip(
                        selected = isMonthSelected,
                        onClick = { showMonthPicker = true },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(monthLabel, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            }
        }

        // Key Stats Summary Card
        item {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.historySelectedTab.value = 0
                        onNavigateToHistory()
                    },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = salesCardTitle,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formattedSales,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = t(viewModel, "অর্ডার: ${recentOrders.size}টি", "Orders: ${recentOrders.size}"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = t(viewModel, "দোকান: ${stats.activeShops}টি", "Shops: ${stats.activeShops}"),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Shortcuts
        item {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Outlined.PostAdd,
                    label = t(viewModel, "নতুন অর্ডার", "New Order"),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    iconBgColor = MaterialTheme.colorScheme.primary,
                    iconColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1.0f).testTag("action_create_order"),
                    onClick = onCreateOrderClick
                )
                QuickActionButton(
                    icon = Icons.Outlined.Inventory,
                    label = t(viewModel, "প্রোডাক্ট লিস্ট", "Products"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1.0f).testTag("action_manage_products"),
                    onClick = {
                        viewModel.setShowOnlyLowStockInProducts(false)
                        onManageProductsClick()
                    }
                )
                QuickActionButton(
                    icon = Icons.Outlined.Storefront,
                    label = t(viewModel, "দোকান ও বকেয়া", "Shops & Dues"),
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1.0f).testTag("action_manage_shops"),
                    onClick = onManageShopsClick
                )
            }
        }

        // Divider
        item {
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Stats Grid
        item {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    title = t(viewModel, "মোট বকেয়া (Due Amount)", "Total Dues"),
                    value = formattedDue,
                    icon = Icons.Outlined.AccountBalanceWallet,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        viewModel.historySelectedTab.value = 1
                        onNavigateToHistory()
                    }
                )

                MetricCard(
                    title = t(viewModel, "মোট আদায়", "Total Collected"),
                    value = formattedCollected,
                    icon = Icons.Outlined.CheckCircle,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        viewModel.historySelectedTab.value = 2
                        onNavigateToHistory()
                    }
                )
                
                MetricCard(
                    title = t(viewModel, "স্টক সতর্কবার্তা", "Stock Warning"),
                    value = t(viewModel, "${stats.lowStockCount} টি প্রোডাক্ট", "${stats.lowStockCount} Products"),
                    icon = Icons.Outlined.ProductionQuantityLimits,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = {
                        viewModel.setShowOnlyLowStockInProducts(true)
                        onManageProductsClick()
                    }
                )
            }
        }

        // Recent Orders Header
        val recentBillsTitle = when (dashboardFilterVal) {
            is ReportFilter.AllTime -> if (isEnglish) "Recent Billing" else "সাম্প্রতিক বিলিং"
            is ReportFilter.Today -> if (isEnglish) "Today's Billing" else "আজকের বিলিং"
            is ReportFilter.SpecificDate -> if (isEnglish) "Billing of Selected Date" else "তারিখের বিলিং"
            is ReportFilter.SpecificMonth -> if (isEnglish) "Billing of Selected Month" else "মাসের বিলিং"
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = recentBillsTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Recent Orders List (Max 3)
        val listToShow = recentOrders.take(3)
        if (listToShow.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = t(viewModel, "কোনো বিল এখনো তৈরি করা হয়নি", "No bills have been created yet"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(listToShow, key = { it.id }) { order ->
                val shop = shops.find { it.id == order.shopId }
                RecentOrderRow(
                    order = order,
                    shopImageUri = shop?.imageUri,
                    onClick = { onOrderClick(order) }
                )
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    iconBgColor: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(104.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = border,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun MiniInfoCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun RecentOrderRow(
    order: Order,
    shopImageUri: String? = null,
    onClick: () -> Unit
) {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateStr = formatter.format(Date(order.timestamp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular icon or Shop image
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    val isImageValid = remember(shopImageUri) {
                        if (!shopImageUri.isNullOrBlank()) {
                            if (shopImageUri.startsWith("content://") || shopImageUri.startsWith("http://") || shopImageUri.startsWith("https://")) {
                                true
                            } else {
                                try {
                                    java.io.File(shopImageUri).exists()
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
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(Uri.parse(shopImageUri!!))
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                
                Column {
                    Text(
                        text = order.shopName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${order.items.size}টি আইটেম • $dateStr",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("৳%,.0f", order.totalAmount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                val statusText = if (order.isPaid) "পেইড" else "বকেয়া"
                val statusColor = if (order.isPaid) PaidGreenText else DueRedText
                val statusBg = if (order.isPaid) PaidGreenBg else DueRedBg
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        fontSize = 9.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
fun MonthPickerDialog(
    onDismissRequest: () -> Unit,
    onMonthSelected: (year: Int, month: Int) -> Unit
) {
    val currentCalendar = Calendar.getInstance()
    var selectedYear by remember { mutableStateOf(currentCalendar.get(Calendar.YEAR)) }
    
    val monthNames = listOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text("মাস সিলেক্ট করুন", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Year Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { selectedYear -= 1 }) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous Year")
                    }
                    Text(
                        text = "$selectedYear সাল",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    IconButton(onClick = { selectedYear += 1 }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next Year")
                    }
                }
                
                // Months Grid (4x3 grid)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 3) {
                                val monthIdx = row * 3 + col
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            onMonthSelected(selectedYear, monthIdx)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = monthNames[monthIdx],
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("বাতিল")
            }
        }
    )
}
