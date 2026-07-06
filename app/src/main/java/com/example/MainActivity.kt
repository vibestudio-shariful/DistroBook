package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.delay
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val viewModel: AppViewModel = viewModel()
      val isDarkMode by viewModel.isDarkMode.collectAsState()
      val isEnglish by viewModel.isEnglish.collectAsState()
      var showSplashScreen by remember { mutableStateOf(true) }

      MyApplicationTheme(darkTheme = isDarkMode) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          if (showSplashScreen) {
            SplashScreen(
              isDarkMode = isDarkMode,
              isEnglish = isEnglish,
              onTimeout = { showSplashScreen = false }
            )
          } else {
            MainAppScreen()
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
  val viewModel: AppViewModel = viewModel()
  val navController = rememberNavController()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  val isEnglish by viewModel.isEnglish.collectAsState()
  val isDarkMode by viewModel.isDarkMode.collectAsState()

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = when (currentRoute) {
              "dashboard" -> if (isEnglish) "Dashboard" else "ড্যাশবোর্ড"
              "create_order" -> if (isEnglish) "Create Bill" else "নতুন মেমো / বিল তৈরি"
              "history" -> if (isEnglish) "Memo History" else "মেমো / বিলের তালিকা"
              "products" -> if (isEnglish) "Products & Stock" else "প্রোডাক্ট ও স্টক"
              "shops" -> if (isEnglish) "Shops & Dues" else "দোকান ও বকেয়া"
              "profile" -> if (isEnglish) "Profile & Settings" else "প্রোফাইল ও সেটিংস"
              else -> if (isEnglish) "Shop Supply" else "দোকান সাপ্লাই"
            },
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onPrimary
          )
        },
        navigationIcon = {
          if (currentRoute == "profile") {
            IconButton(onClick = { navController.popBackStack() }) {
              Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = if (isEnglish) "Back" else "ফিরে যান",
                tint = MaterialTheme.colorScheme.onPrimary
              )
            }
          }
        },
        actions = {
          // Language switch button
          IconButton(
            onClick = { viewModel.setLanguage(!isEnglish) },
            modifier = Modifier.testTag("action_toggle_language")
          ) {
            Icon(
              imageVector = Icons.Default.Translate,
              contentDescription = if (isEnglish) "Switch to Bangla" else "ইংরেজিতে পরিবর্তন করুন",
              tint = MaterialTheme.colorScheme.onPrimary
            )
          }
          // Theme toggle button
          IconButton(
            onClick = { viewModel.setDarkMode(!isDarkMode) },
            modifier = Modifier.testTag("action_toggle_theme")
          ) {
            Icon(
              imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = if (isEnglish) "Toggle Theme" else "থিম পরিবর্তন করুন",
              tint = MaterialTheme.colorScheme.onPrimary
            )
          }
          // Settings button (only if not on profile screen)
          if (currentRoute != "profile") {
            IconButton(
              onClick = { navController.navigate("profile") },
              modifier = Modifier.testTag("action_go_to_profile")
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = if (isEnglish) "Settings" else "সেটিংস",
                tint = MaterialTheme.colorScheme.onPrimary
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primary,
          titleContentColor = MaterialTheme.colorScheme.onPrimary,
          actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
          navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 0.dp,
        modifier = Modifier.testTag("bottom_nav")
      ) {
        val isEnglish by viewModel.isEnglish.collectAsState()
        val items = listOf(
          NavigationItem("dashboard", if (isEnglish) "Home" else "হোম", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
          NavigationItem("create_order", if (isEnglish) "Create Bill" else "বিল তৈরি", Icons.Default.PostAdd, Icons.Outlined.PostAdd),
          NavigationItem("history", if (isEnglish) "History" else "ইতিহাস", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong),
          NavigationItem("products", if (isEnglish) "Products" else "প্রোডাক্ট", Icons.Default.Inventory, Icons.Outlined.Inventory),
          NavigationItem("shops", if (isEnglish) "Shops" else "দোকান", Icons.Default.Storefront, Icons.Outlined.Storefront)
        )

        items.forEach { item ->
          val selected = currentRoute == item.route
          NavigationBarItem(
            selected = selected,
            onClick = {
              if (currentRoute != item.route) {
                navController.navigate(item.route) {
                  popUpTo("dashboard") {
                    inclusive = false
                  }
                  launchSingleTop = true
                }
              }
            },
            icon = {
              Icon(
                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label
              )
            },
            label = { Text(item.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = Color.White,
              selectedTextColor = Color.White,
              indicatorColor = Color.White.copy(alpha = 0.2f),
              unselectedIconColor = Color.White.copy(alpha = 0.6f),
              unselectedTextColor = Color.White.copy(alpha = 0.6f)
            ),
            modifier = Modifier.testTag("nav_item_${item.route}")
          )
        }
      }
    }
  ) { innerPadding ->
    NavHost(
      navController = navController,
      startDestination = "dashboard",
      modifier = Modifier.padding(innerPadding)
    ) {
      composable("dashboard") {
        DashboardScreen(
          viewModel = viewModel,
          onCreateOrderClick = { navController.navigate("create_order") },
          onManageProductsClick = { navController.navigate("products") },
          onManageShopsClick = { navController.navigate("shops") },
          onOrderClick = { order ->
            navController.navigate("history")
          },
          onProfileClick = { navController.navigate("profile") },
          onNavigateToHistory = { navController.navigate("history") }
        )
      }
      composable("create_order") {
        CreateOrderScreen(
          viewModel = viewModel,
          onOrderPlacedSuccessfully = {
            navController.navigate("history") {
              popUpTo("dashboard") { inclusive = false }
            }
          },
          onAddProductClick = { navController.navigate("products") }
        )
      }
      composable("history") {
        OrderHistoryScreen(viewModel = viewModel)
      }
      composable("products") {
        ProductsScreen(viewModel = viewModel)
      }
      composable("shops") {
        ShopsScreen(viewModel = viewModel)
      }
      composable("profile") {
        ProfileScreen(
          viewModel = viewModel,
          onBackClick = { navController.popBackStack() }
        )
      }
    }
  }
}

data class NavigationItem(
  val route: String,
  val label: String,
  val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
  val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun SplashScreen(isDarkMode: Boolean, isEnglish: Boolean, onTimeout: () -> Unit) {
  val scale = remember { Animatable(0.6f) }
  val alpha = remember { Animatable(0f) }

  LaunchedEffect(key1 = true) {
    launch {
      scale.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
      )
    }
    launch {
      alpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000)
      )
    }
    delay(2000) // Beautiful delay
    onTimeout()
  }

  val gradientColors = if (isDarkMode) {
    listOf(Color(0xFF0F091D), Color(0xFF1F1235))
  } else {
    listOf(Color(0xFFFAF5FF), Color(0xFFF3E8FF))
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Brush.verticalGradient(gradientColors)),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.padding(24.dp)
    ) {
      // Modern circular icon pack logo
      Image(
        painter = painterResource(id = R.drawable.ic_launcher_circle_logo_1783267887935),
        contentDescription = "Logo",
        modifier = Modifier
          .size(130.dp)
          .scale(scale.value)
          .alpha(alpha.value)
          .clip(CircleShape)
      )

      Spacer(modifier = Modifier.height(24.dp))

      // App name
      Text(
        text = if (isEnglish) "DistroBook" else "ডিস্ট্রো-বুক",
        fontSize = 32.sp,
        fontWeight = FontWeight.ExtraBold,
        color = if (isDarkMode) Color.White else Color(0xFF5B21B6),
        modifier = Modifier.alpha(alpha.value)
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Subtitle
      Text(
        text = if (isEnglish) "Distribution Ledger & Memo Manager" else "দোকান সাপ্লাই ও মেমো ডায়েরি",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = if (isDarkMode) Color(0xFFC4B5FD) else Color(0xFF6D28D9),
        textAlign = TextAlign.Center,
        modifier = Modifier.alpha(alpha.value)
      )

      Spacer(modifier = Modifier.height(40.dp))

      // Progress bar
      CircularProgressIndicator(
        color = if (isDarkMode) Color(0xFFA78BFA) else Color(0xFF7C3AED),
        strokeWidth = 3.dp,
        modifier = Modifier
          .size(32.dp)
          .alpha(alpha.value)
      )
    }

    // Bottom branding & version (increased bottom padding to 75.dp so it's not hidden under navigation bar)
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 75.dp)
        .alpha(alpha.value),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = if (isEnglish) "Version 1.2.0" else "সংস্করণ ১.২.০",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isDarkMode) Color(0xFFC4B5FD) else Color(0xFF6D28D9)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "VibeStudio",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = if (isDarkMode) Color(0xFFA78BFA) else Color(0xFF7C3AED)
      )
    }
  }
}
