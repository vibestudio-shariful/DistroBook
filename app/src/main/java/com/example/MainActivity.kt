package com.example

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      if (currentRoute != "profile") {
        TopAppBar(
          title = {
            Text(
              text = when (currentRoute) {
                "dashboard" -> "ড্যাশবোর্ড (Dashboard)"
                "create_order" -> "নতুন মেমো / বিল তৈরি"
                "history" -> "মেমো / বিলের তালিকা"
                "products" -> "প্রোডাক্ট ও স্টক"
                "shops" -> "দোকান ও বকেয়া"
                else -> "দোকান সাপ্লাই"
              },
              fontWeight = FontWeight.Bold,
              fontSize = 20.sp
            )
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
          )
        )
      }
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("bottom_nav")
      ) {
        val items = listOf(
          NavigationItem("dashboard", "হোম", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
          NavigationItem("create_order", "বিল তৈরি", Icons.Default.PostAdd, Icons.Outlined.PostAdd),
          NavigationItem("history", "ইতিহাস", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong),
          NavigationItem("products", "প্রোডাক্ট", Icons.Default.Inventory, Icons.Outlined.Inventory),
          NavigationItem("shops", "দোকান", Icons.Default.Storefront, Icons.Outlined.Storefront)
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
          onProfileClick = { navController.navigate("profile") }
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
