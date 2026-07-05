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

      MyApplicationTheme(darkTheme = isDarkMode) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          MainAppScreen()
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

  // Set Navigation Bar Color
  val view = LocalView.current
  val navBarColor = MaterialTheme.colorScheme.primaryContainer.toArgb()
  SideEffect {
    val window = (view.context as Activity).window
    window.navigationBarColor = navBarColor
    // Optionally set status bar color
    window.statusBarColor = navBarColor
  }

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
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
          )
        },
        navigationIcon = {
          if (currentRoute == "profile") {
            IconButton(onClick = { navController.popBackStack() }) {
              Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = if (isEnglish) "Back" else "ফিরে যান",
                tint = MaterialTheme.colorScheme.onPrimaryContainer
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
              tint = MaterialTheme.colorScheme.onPrimaryContainer
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
              tint = MaterialTheme.colorScheme.onPrimaryContainer
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
                tint = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 8.dp,
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
            label = { Text(item.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
          }
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
