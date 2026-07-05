package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed class ReportFilter {
    object AllTime : ReportFilter()
    object Today : ReportFilter()
    data class SpecificDate(val date: Date) : ReportFilter()
    data class SpecificMonth(val year: Int, val month: Int) : ReportFilter() // month is 0-indexed (0..11)
}

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val products: List<Product>,
    val shops: List<Shop>,
    val orders: List<Order>,
    val userName: String,
    val businessName: String,
    val userPhone: String,
    val userEmail: String,
    val userAddress: String
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(
        database.productDao(),
        database.shopDao(),
        database.orderDao()
    )

    // User Profile persistence
    private val sharedPrefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    
    val userName = MutableStateFlow(sharedPrefs.getString("user_name", "আপনার নাম") ?: "আপনার নাম")
    val businessName = MutableStateFlow(sharedPrefs.getString("business_name", "আপনার প্রতিষ্ঠানের নাম") ?: "আপনার প্রতিষ্ঠানের নাম")
    val userPhone = MutableStateFlow(sharedPrefs.getString("user_phone", "০১৭xxxxxxxx") ?: "০১৭xxxxxxxx")
    val userEmail = MutableStateFlow(sharedPrefs.getString("user_email", "ইমেইল বা সোশ্যাল প্রোফাইল") ?: "ইমেইল বা সোশ্যাল প্রোফাইল")
    val userAddress = MutableStateFlow(sharedPrefs.getString("user_address", "আপনার ঠিকানা") ?: "আপনার ঠিকানা")
    val userAvatarPath = MutableStateFlow(sharedPrefs.getString("user_avatar_path", null))

    // Theme state
    val isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("is_dark_mode", false))

    fun setDarkMode(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("is_dark_mode", enabled).apply()
        isDarkMode.value = enabled
    }

    // Language state: true for English, false for Bangla (default)
    val isEnglish = MutableStateFlow(sharedPrefs.getBoolean("is_english", false))

    val historySelectedTab = MutableStateFlow(0) // 0 = All, 1 = Due, 2 = Paid

    fun setLanguage(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("is_english", enabled).apply()
        isEnglish.value = enabled
    }

    // Save Avatar Photo to Private Storage
    fun saveUserAvatar(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val avatarFile = File(context.filesDir, "user_avatar.jpg")
                    avatarFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    val path = avatarFile.absolutePath
                    sharedPrefs.edit().putString("user_avatar_path", path).apply()
                    userAvatarPath.value = path
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteUserAvatar() {
        sharedPrefs.edit().remove("user_avatar_path").apply()
        userAvatarPath.value = null
    }

    // Export Data to JSON Uri (Storage Access Framework)
    fun exportBackupToUri(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val prodList = products.value
                val shopList = shops.value
                val orderList = orders.value

                val payload = BackupPayload(
                    products = prodList,
                    shops = shopList,
                    orders = orderList,
                    userName = userName.value,
                    businessName = businessName.value,
                    userPhone = userPhone.value,
                    userEmail = userEmail.value,
                    userAddress = userAddress.value
                )

                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(BackupPayload::class.java)
                val jsonString = adapter.toJson(payload)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.bufferedWriter().use { it.write(jsonString) }
                } ?: throw Exception("আউটপুট স্ট্রিম ওপেন করা সম্ভব হয়নি")

                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "ব্যাকআপ সেভ করা সম্ভব হয়নি")
            }
        }
    }

    // Export Data to JSON and Share
    fun exportBackup(context: Context, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val prodList = products.value
                val shopList = shops.value
                val orderList = orders.value

                val payload = BackupPayload(
                    products = prodList,
                    shops = shopList,
                    orders = orderList,
                    userName = userName.value,
                    businessName = businessName.value,
                    userPhone = userPhone.value,
                    userEmail = userEmail.value,
                    userAddress = userAddress.value
                )

                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(BackupPayload::class.java)
                val jsonString = adapter.toJson(payload)

                // Save locally to external files dir (documents area)
                val backupFile = File(context.getExternalFilesDir(null), "distro_book_backup.json")
                backupFile.writeText(jsonString)

                // Share intent
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "ডিস্ট্রো-বুক ডেটা ব্যাকআপ (Distro-Book Backup)")
                    putExtra(Intent.EXTRA_TEXT, jsonString)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "ব্যাকআপ ডেটা সেভ বা শেয়ার করুন").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })

                onSuccess(backupFile.absolutePath)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "ব্যাকআপ তৈরি করা সম্ভব হয়নি")
            }
        }
    }

    // Import Data from JSON Uri
    fun importBackup(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                } ?: throw Exception("ফাইলটি পড়া সম্ভব হয়নি")

                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(BackupPayload::class.java)
                val payload = adapter.fromJson(jsonString) ?: throw Exception("ভুল ফাইল ফরম্যাট")

                // Insert elements into the Database
                payload.products.forEach { repository.insertProduct(it) }
                payload.shops.forEach { repository.insertShop(it) }
                payload.orders.forEach { repository.insertOrder(it) }
                
                // Restore profile
                saveUserProfile(payload.userName, payload.businessName, payload.userPhone, payload.userEmail, payload.userAddress)

                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "রিস্টোর করা সম্ভব হয়নি")
            }
        }
    }

    fun saveUserProfile(name: String, business: String, phone: String, email: String, address: String) {
        sharedPrefs.edit().apply {
            putString("user_name", name)
            putString("business_name", business)
            putString("user_phone", phone)
            putString("user_email", email)
            putString("user_address", address)
            apply()
        }
        userName.value = name
        businessName.value = business
        userPhone.value = phone
        userEmail.value = email
        userAddress.value = address
    }

    // Data streams
    val products: StateFlow<List<Product>> = repository.allProducts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shops: StateFlow<List<Shop>> = repository.allShops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<Order>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Date/Month Filtering Helper Functions
    fun isSameDay(timestamp: Long, targetDate: Date): Boolean {
        val cal1 = Calendar.getInstance()
        cal1.timeInMillis = timestamp
        val cal2 = Calendar.getInstance()
        cal2.time = targetDate
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun isSameMonth(timestamp: Long, year: Int, month: Int): Boolean {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        return cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
    }

    fun isToday(timestamp: Long): Boolean {
        return isSameDay(timestamp, Date())
    }

    val dashboardFilter = MutableStateFlow<ReportFilter>(ReportFilter.AllTime)

    fun setDashboardFilter(filter: ReportFilter) {
        dashboardFilter.value = filter
    }

    val showOnlyLowStockInProducts = MutableStateFlow(false)

    fun setShowOnlyLowStockInProducts(value: Boolean) {
        showOnlyLowStockInProducts.value = value
    }

    // Dashboard Statistics
    val stats = combine(products, shops, orders, dashboardFilter) { prodList, shopList, orderList, filter ->
        val filteredOrders = when (filter) {
            is ReportFilter.AllTime -> orderList
            is ReportFilter.Today -> orderList.filter { isToday(it.timestamp) }
            is ReportFilter.SpecificDate -> orderList.filter { isSameDay(it.timestamp, filter.date) }
            is ReportFilter.SpecificMonth -> orderList.filter { isSameMonth(it.timestamp, filter.year, filter.month) }
        }

        val totalSales = filteredOrders.sumOf { it.totalAmount }
        val totalCollected = filteredOrders.sumOf { it.paidAmount }
        val totalDue = filteredOrders.sumOf { it.dueAmount }
        val activeShops = shopList.size
        val lowStockProducts = prodList.count { it.stock <= 5 }
        
        DashboardStats(
            totalSales = totalSales,
            totalCollected = totalCollected,
            totalDue = totalDue,
            activeShops = activeShops,
            lowStockCount = lowStockProducts
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardStats()
    )

    // Filtered orders for dashboard display
    val dashboardOrders = combine(orders, dashboardFilter) { orderList, filter ->
        when (filter) {
            is ReportFilter.AllTime -> orderList
            is ReportFilter.Today -> orderList.filter { isToday(it.timestamp) }
            is ReportFilter.SpecificDate -> orderList.filter { isSameDay(it.timestamp, filter.date) }
            is ReportFilter.SpecificMonth -> orderList.filter { isSameMonth(it.timestamp, filter.year, filter.month) }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // Product actions
    fun addProduct(name: String, price: Double, stock: Int, description: String = "") {
        viewModelScope.launch {
            repository.insertProduct(Product(name = name, price = price, stock = stock, description = description))
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.updateProduct(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // Shop actions
    fun addShop(name: String, ownerName: String, phone: String, address: String, imageUri: String? = null) {
        viewModelScope.launch {
            repository.insertShop(Shop(name = name, ownerName = ownerName, phone = phone, address = address, imageUri = imageUri))
        }
    }

    fun updateShop(shop: Shop) {
        viewModelScope.launch {
            repository.updateShop(shop)
        }
    }

    fun deleteShop(shop: Shop) {
        viewModelScope.launch {
            repository.deleteShop(shop)
        }
    }

    // Order actions
    fun createOrder(shopId: Int, shopName: String, items: List<OrderItem>, totalAmount: Double, paidAmount: Double) {
        viewModelScope.launch {
            val isPaid = paidAmount >= totalAmount
            val order = Order(
                shopId = shopId,
                shopName = shopName,
                items = items,
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                isPaid = isPaid,
                remarks = ""
            )
            repository.insertOrder(order)
        }
    }

    fun updateOrderPayment(order: Order, additionalPayment: Double) {
        viewModelScope.launch {
            val newPaidAmount = (order.paidAmount + additionalPayment).coerceAtMost(order.totalAmount)
            val updatedOrder = order.copy(
                paidAmount = newPaidAmount,
                isPaid = newPaidAmount >= order.totalAmount
            )
            repository.updateOrder(updatedOrder)
        }
    }

    fun collectAllShopDues(shopId: Int) {
        viewModelScope.launch {
            val shopOrders = repository.allOrders.first().filter { it.shopId == shopId && !it.isPaid }
            shopOrders.forEach { order ->
                val updatedOrder = order.copy(
                    paidAmount = order.totalAmount,
                    isPaid = true
                )
                repository.updateOrder(updatedOrder)
            }
        }
    }

    fun deleteOrder(order: Order) {
        viewModelScope.launch {
            repository.deleteOrder(order)
        }
    }
}

data class DashboardStats(
    val totalSales: Double = 0.0,
    val totalCollected: Double = 0.0,
    val totalDue: Double = 0.0,
    val activeShops: Int = 0,
    val lowStockCount: Int = 0
)
