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
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.*
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

  // Global Error Dialog State
  val context = LocalContext.current
  var errorDialogState by remember { mutableStateOf<Pair<String, String>?>(null) }
  
  LaunchedEffect(Unit) {
      viewModel.errorEvent.collect { error ->
          errorDialogState = error
      }
  }

  if (errorDialogState != null) {
      val (title, message) = errorDialogState!!
      AlertDialog(
          onDismissRequest = { errorDialogState = null },
          icon = { Icon(Icons.Default.ReportProblem, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
          title = {
              Text(
                  text = title,
                  fontWeight = FontWeight.Black,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.fillMaxWidth()
              )
          },
          text = {
              Column(
                  verticalArrangement = Arrangement.spacedBy(12.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
              ) {
                  Text(
                      text = message,
                      style = MaterialTheme.typography.bodyLarge,
                      textAlign = TextAlign.Center,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                  
                  var showDetails by remember { mutableStateOf(false) }
                  
                  TextButton(onClick = { showDetails = !showDetails }) {
                      Text(if (isEnglish) (if (showDetails) "Hide Log" else "Show Log Details") else (if (showDetails) "লগ লুকান" else "লগ ডিটেইলস দেখুন"))
                  }
                  
                  if (showDetails) {
                      Surface(
                          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                          shape = RoundedCornerShape(12.dp),
                          modifier = Modifier.fillMaxWidth(),
                          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                      ) {
                          Column(modifier = Modifier.padding(12.dp)) {
                              Text(
                                  text = if (isEnglish) "System Log:" else "সিস্টেম লগ:",
                                  style = MaterialTheme.typography.labelSmall,
                                  fontWeight = FontWeight.Bold,
                                  color = MaterialTheme.colorScheme.error
                              )
                              Spacer(modifier = Modifier.height(4.dp))
                              Text(
                                  text = message,
                                  style = MaterialTheme.typography.bodySmall,
                                  fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                  color = MaterialTheme.colorScheme.error
                              )
                          }
                      }
                  }
                  
                  Text(
                      text = if (isEnglish) "Our team is ready to help if this issue persists." else "সমস্যাটি সমাধান না হলে আমাদের সহায়তা টিমের সাথে যোগাযোগ করতে পারেন।",
                      style = MaterialTheme.typography.labelMedium,
                      textAlign = TextAlign.Center,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                  )
              }
          },
          confirmButton = {
              Button(
                  onClick = { errorDialogState = null },
                  colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp)
              ) {
                  Text(if (isEnglish) "Close" else "ঠিক আছে")
              }
          },
          dismissButton = {
              OutlinedButton(
                  onClick = {
                      try {
                          val whatsappIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801768899599?text=DistroBook Error Report: $message"))
                          context.startActivity(whatsappIntent)
                      } catch (e: Exception) {
                          Toast.makeText(context, "WhatsApp not found", Toast.LENGTH_SHORT).show()
                      }
                      errorDialogState = null
                  },
                  modifier = Modifier.fillMaxWidth(),
                  shape = RoundedCornerShape(12.dp),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
              ) {
                  Icon(Icons.Default.Feedback, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(if (isEnglish) "Contact Support" else "সাপোর্টে কথা বলুন")
              }
          },
          shape = RoundedCornerShape(28.dp),
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 8.dp
      )
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
      modifier = Modifier.padding(innerPadding),
      enterTransition = {
          fadeIn(animationSpec = tween(300)) + slideIntoContainer(
              AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)
          )
      },
      exitTransition = {
          fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
              AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)
          )
      },
      popEnterTransition = {
          fadeIn(animationSpec = tween(300)) + slideIntoContainer(
              AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)
          )
      },
      popExitTransition = {
          fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
              AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)
          )
      }
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
  val scale = remember { Animatable(0.8f) }
  val alpha = remember { Animatable(0f) }
  val translateY = remember { Animatable(20f) }

  LaunchedEffect(key1 = true) {
    launch {
      scale.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
      )
    }
    launch {
      alpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000)
      )
    }
    launch {
      translateY.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
      )
    }
    delay(2500)
    onTimeout()
  }

  val gradientColors = if (isDarkMode) {
    listOf(Color(0xFF0D0221), Color(0xFF190E4F))
  } else {
    listOf(Color(0xFFF0F4FF), Color(0xFFE0E7FF))
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Brush.radialGradient(
        colors = gradientColors,
        center = androidx.compose.ui.geometry.Offset.Unspecified,
        radius = Float.POSITIVE_INFINITY
      )),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .padding(24.dp)
        .offset(y = translateY.value.dp)
    ) {
      // Modern circular icon pack logo with subtle glow
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(150.dp)
          .scale(scale.value)
          .alpha(alpha.value)
      ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
        ) {}
        Image(
          painter = painterResource(id = R.drawable.ic_launcher_circle_logo_1783267887935),
          contentDescription = "Logo",
          modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
        )
      }

      Spacer(modifier = Modifier.height(32.dp))

      // App name with letter spacing
      Text(
        text = if (isEnglish) "DISTRO BOOK" else "ডিস্ট্রো-বুক",
        fontSize = 36.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = if (isDarkMode) Color.White else Color(0xFF1E3A8A),
        modifier = Modifier.alpha(alpha.value)
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Subtitle
      Surface(
          color = if (isDarkMode) Color(0xFF312E81) else Color(0xFFDBEAFE),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.alpha(alpha.value)
      ) {
          Text(
            text = if (isEnglish) "Distribution & Ledger Management" else "দোকান সাপ্লাই ও মেমো ডায়েরি",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color(0xFFE0E7FF) else Color(0xFF1E40AF),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )
      }

      Spacer(modifier = Modifier.height(64.dp))

      // Modern dot loading indicator
      Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.alpha(alpha.value)
      ) {
          repeat(3) { index ->
              val dotAlpha = remember { Animatable(0.2f) }
              LaunchedEffect(Unit) {
                  delay(index * 150L)
                  while (true) {
                      dotAlpha.animateTo(1f, tween(500))
                      dotAlpha.animateTo(0.2f, tween(500))
                  }
              }
              Box(
                  modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(if (isDarkMode) Color(0xFF818CF8) else Color(0xFF3B82F6))
                      .alpha(dotAlpha.value)
              )
          }
      }
    }

    // Bottom branding
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 60.dp)
        .alpha(alpha.value),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = if (isEnglish) "POWERED BY" else "পরিচালনায়",
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f)
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "VIBESTUDIO",
        fontSize = 14.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        color = if (isDarkMode) Color(0xFF818CF8) else Color(0xFF2563EB)
      )
    }
  }
}
