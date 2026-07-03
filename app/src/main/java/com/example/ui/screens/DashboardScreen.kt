package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.R
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
    onProfileClick: () -> Unit
) {
    val stats by viewModel.stats.collectAsState()
    val recentOrders by viewModel.orders.collectAsState()
    val userNameVal by viewModel.userName.collectAsState()
    
    val initials = remember(userNameVal) {
        val parts = userNameVal.trim().split(Regex("\\s+"))
        if (parts.size >= 2) {
            "${parts[0].firstOrNull() ?: ""}${parts[1].firstOrNull() ?: ""}"
        } else {
            "${userNameVal.trim().firstOrNull() ?: "U"}"
        }
    }.uppercase()
    
    val formattedSales = String.format("৳%,.2f", stats.totalSales)
    val formattedCollected = String.format("৳%,.2f", stats.totalCollected)
    val formattedDue = String.format("৳%,.2f", stats.totalDue)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F9FF))
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mockup Header Area
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ডিস্ট্রো-বুক",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF001D36),
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "আজকের সরবরাহ ড্যাশবোর্ড",
                        fontSize = 13.sp,
                        color = Color(0xFF42474E),
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF0061A4))
                        .clickable { onProfileClick() }
                        .testTag("dashboard_profile_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Key Stats Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0061A4),
                    contentColor = Color.White
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
                                text = "আজকের মোট বিক্রি",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.85f),
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
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color.White,
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
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "অর্ডার: ${recentOrders.size}টি",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "দোকান: ${stats.activeShops}টি",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Quick Action Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.Receipt,
                    label = "নতুন অর্ডার",
                    containerColor = Color(0xFFD1E4FF),
                    contentColor = Color(0xFF001D36),
                    iconBgColor = Color(0xFF0061A4),
                    iconColor = Color.White,
                    modifier = Modifier.weight(1.0f).testTag("action_create_order"),
                    onClick = onCreateOrderClick
                )
                QuickActionButton(
                    icon = Icons.Default.Inventory,
                    label = "প্রোডাক্ট লিস্ট",
                    containerColor = Color.White,
                    contentColor = Color(0xFF42474E),
                    iconBgColor = Color(0xFFF0F4F8),
                    iconColor = Color(0xFF42474E),
                    border = BorderStroke(1.dp, Color(0xFFC2C7CF)),
                    modifier = Modifier.weight(1.0f).testTag("action_manage_products"),
                    onClick = onManageProductsClick
                )
                QuickActionButton(
                    icon = Icons.Default.Storefront,
                    label = "দোকান ও বকেয়া",
                    containerColor = Color.White,
                    contentColor = Color(0xFF42474E),
                    iconBgColor = Color(0xFFF0F4F8),
                    iconColor = Color(0xFF42474E),
                    border = BorderStroke(1.dp, Color(0xFFC2C7CF)),
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Large primary due card (since due is highly crucial for supplier)
                MetricCard(
                    title = "মোট বকেয়া (Due Amount)",
                    value = formattedDue,
                    icon = Icons.Default.AccountBalanceWallet,
                    containerColor = Color(0xFFF9DEDC),
                    contentColor = Color(0xFF410E0B)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "মোট আদায়",
                        value = formattedCollected,
                        icon = Icons.Default.CheckCircle,
                        containerColor = Color(0xFFD2E8D1),
                        contentColor = Color(0xFF0A210B),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "স্টক সতর্কবার্তা",
                        value = "${stats.lowStockCount} টি প্রোডাক্ট",
                        icon = Icons.Default.Warning,
                        containerColor = if (stats.lowStockCount > 0) Color(0xFFFFF3E0) else Color(0xFFECEFF1),
                        contentColor = if (stats.lowStockCount > 0) Color(0xFFE65100) else Color(0xFF42474E),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Recent Orders Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "সাম্প্রতিক বিলিং",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
            }
        }

        // Recent Orders List (Max 3)
        val listToShow = recentOrders.take(3)
        if (listToShow.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = Color(0xFF72777F),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "কোনো বিল এখনো তৈরি করা হয়নি",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF72777F)
                        )
                    }
                }
            }
        } else {
            items(listToShow) { order ->
                RecentOrderRow(order = order, onClick = { onOrderClick(order) })
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
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
    iconColor: Color = Color(0xFF0061A4),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC2C7CF)),
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
                    color = Color(0xFF42474E),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
            }
        }
    }
}

@Composable
fun RecentOrderRow(
    order: Order,
    onClick: () -> Unit
) {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateStr = formatter.format(Date(order.timestamp))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E2EC))
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
                // Circular icon placeholder matching Tailwind
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE0E2EC)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Color(0xFF42474E),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column {
                    Text(
                        text = order.shopName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1C1E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${order.items.size}টি আইটেম • $dateStr",
                        fontSize = 11.sp,
                        color = Color(0xFF72777F)
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = String.format("৳%,.0f", order.totalAmount),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0061A4)
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
