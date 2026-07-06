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
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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
    val userAddress: String,
    val userAvatarBase64: String? = null,
    val shopImagesBase64: Map<String, String>? = null
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

    // Google Drive Integration state
    val googleAccountEmail = MutableStateFlow<String?>(sharedPrefs.getString("google_account_email", null))
    val googleAccountDisplayName = MutableStateFlow<String?>(sharedPrefs.getString("google_account_display_name", null))
    val googleDriveBackups = MutableStateFlow<List<com.example.utils.DriveBackupFile>>(emptyList())
    val isDriveLoading = MutableStateFlow(false)

    fun setGoogleAccount(email: String?, displayName: String?) {
        sharedPrefs.edit().apply {
            putString("google_account_email", email)
            putString("google_account_display_name", displayName)
        }.apply()
        googleAccountEmail.value = email
        googleAccountDisplayName.value = displayName
        if (email != null) {
            loadGoogleDriveBackups()
        } else {
            googleDriveBackups.value = emptyList()
        }
    }

    fun loadGoogleDriveBackups(onAuthRequired: ((Intent) -> Unit)? = null) {
        val email = googleAccountEmail.value ?: return
        viewModelScope.launch {
            isDriveLoading.value = true
            try {
                // Get token, requesting auth recovery if needed and callback is provided
                val token = if (onAuthRequired != null) {
                    getDriveAccessToken(getApplication(), email, onAuthRequired)
                } else {
                    com.example.utils.GoogleDriveHelper.getAccessToken(getApplication(), email)
                }
                if (token != null) {
                    try {
                        val backups = com.example.utils.GoogleDriveHelper.listBackups(token)
                        googleDriveBackups.value = backups
                    } catch (ioe: java.io.IOException) {
                        if (ioe.message == "401 Unauthorized") {
                            try {
                                com.google.android.gms.auth.GoogleAuthUtil.clearToken(getApplication(), token)
                            } catch (clearEx: Exception) {
                                clearEx.printStackTrace()
                            }
                            val freshToken = if (onAuthRequired != null) {
                                getDriveAccessToken(getApplication(), email, onAuthRequired)
                            } else {
                                com.example.utils.GoogleDriveHelper.getAccessToken(getApplication(), email)
                            }
                            if (freshToken != null) {
                                val backups = com.example.utils.GoogleDriveHelper.listBackups(freshToken)
                                googleDriveBackups.value = backups
                            }
                        } else {
                            throw ioe
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isDriveLoading.value = false
            }
        }
    }

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

    // Dedicated app storage directory
    private val appStorageDir: File by lazy {
        val publicDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "DistroBook"
        )
        try {
            if (!publicDir.exists()) {
                publicDir.mkdirs()
            }
            if (publicDir.exists() && publicDir.canWrite()) {
                publicDir
            } else {
                val fallbackDir = File(application.filesDir, "DistroBook")
                if (!fallbackDir.exists()) {
                    fallbackDir.mkdirs()
                }
                fallbackDir
            }
        } catch (e: Exception) {
            val fallbackDir = File(application.filesDir, "DistroBook")
            if (!fallbackDir.exists()) {
                fallbackDir.mkdirs()
            }
            fallbackDir
        }
    }

    private val mediaDir: File by lazy {
        val dir = File(application.filesDir, "Media")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private val backupDir: File by lazy {
        val dir = File(appStorageDir, "Backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    fun saveShopImage(context: Context, uri: Uri, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val resolver = context.contentResolver
                resolver.openInputStream(uri)?.use { inputStream ->
                    val filename = "shop_${System.currentTimeMillis()}.jpg"
                    val file = File(mediaDir, filename)
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    onResult(file.absolutePath)
                } ?: onResult(null)
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(null)
            }
        }
    }

    // Save Avatar Photo to Private Storage
    fun saveUserAvatar(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val oldPath = sharedPrefs.getString("user_avatar_path", null)
                    if (oldPath != null) {
                        try {
                            File(oldPath).delete()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    val avatarFile = File(mediaDir, "user_avatar_${System.currentTimeMillis()}.jpg")
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

    private fun getPhotosBase64(shopsList: List<Shop>): Pair<String?, Map<String, String>> {
        var userAvatarBase64: String? = null
        userAvatarPath.value?.let { path ->
            val file = File(path)
            if (file.exists()) {
                try {
                    val bytes = file.readBytes()
                    userAvatarBase64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val shopImagesBase = mutableMapOf<String, String>()
        shopsList.forEach { shop ->
            shop.imageUri?.let { uriPath ->
                val file = File(uriPath)
                if (file.exists()) {
                    try {
                        val bytes = file.readBytes()
                        val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT)
                        shopImagesBase[file.name] = base64
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return Pair(userAvatarBase64, shopImagesBase)
    }

    private fun restorePhotos(userAvatarBase64Str: String?, shopImagesBase64Map: Map<String, String>?): Map<String, String> {
        val restoredShopImagesMap = mutableMapOf<String, String>()
        
        // 1. Restore User Avatar
        userAvatarBase64Str?.let { base64 ->
            try {
                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                val avatarFile = File(mediaDir, "user_avatar_${System.currentTimeMillis()}.jpg")
                avatarFile.writeBytes(bytes)
                val path = avatarFile.absolutePath
                sharedPrefs.edit().putString("user_avatar_path", path).apply()
                userAvatarPath.value = path
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Restore Shop Images
        shopImagesBase64Map?.forEach { (fileName, base64) ->
            try {
                val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                val restoredFile = File(mediaDir, fileName)
                restoredFile.writeBytes(bytes)
                restoredShopImagesMap[fileName] = restoredFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        return restoredShopImagesMap
    }

    // Export Data to JSON Uri (Storage Access Framework)
    fun exportBackupToUri(context: Context, uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val prodList = products.value
                val shopList = shops.value
                val orderList = orders.value

                val (userAvatarBase64, shopImagesBase) = getPhotosBase64(shopList)

                val payload = BackupPayload(
                    products = prodList,
                    shops = shopList,
                    orders = orderList,
                    userName = userName.value,
                    businessName = businessName.value,
                    userPhone = userPhone.value,
                    userEmail = userEmail.value,
                    userAddress = userAddress.value,
                    userAvatarBase64 = userAvatarBase64,
                    shopImagesBase64 = shopImagesBase
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

                val (userAvatarBase64, shopImagesBase) = getPhotosBase64(shopList)

                val payload = BackupPayload(
                    products = prodList,
                    shops = shopList,
                    orders = orderList,
                    userName = userName.value,
                    businessName = businessName.value,
                    userPhone = userPhone.value,
                    userEmail = userEmail.value,
                    userAddress = userAddress.value,
                    userAvatarBase64 = userAvatarBase64,
                    shopImagesBase64 = shopImagesBase
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

                val restoredShopImagesMap = restorePhotos(payload.userAvatarBase64, payload.shopImagesBase64)

                // Insert elements into the Database
                payload.products.forEach { repository.insertProduct(it) }
                
                payload.shops.forEach { shop ->
                    val correctedShop = if (shop.imageUri != null) {
                        val filename = File(shop.imageUri).name
                        val newPath = restoredShopImagesMap[filename] ?: File(mediaDir, filename).let { 
                            if (it.exists()) it.absolutePath else null 
                        }
                        shop.copy(imageUri = newPath ?: shop.imageUri)
                    } else {
                        shop
                    }
                    repository.insertShop(correctedShop)
                }

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
    fun addProduct(name: String, price: Double, stock: Int, description: String = "", unit: String = "Pcs") {
        viewModelScope.launch {
            repository.insertProduct(Product(name = name, price = price, stock = stock, description = description, unit = unit))
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

    fun backupData(onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val products = repository.allProducts.first()
                val shops = repository.allShops.first()
                val orders = repository.allOrders.first()

                val (userAvatarBase64, shopImagesBase) = getPhotosBase64(shops)
                
                val payload = BackupPayload(
                    products = products,
                    shops = shops,
                    orders = orders,
                    userName = userName.value,
                    businessName = businessName.value,
                    userPhone = userPhone.value,
                    userEmail = userEmail.value,
                    userAddress = userAddress.value,
                    userAvatarBase64 = userAvatarBase64,
                    shopImagesBase64 = shopImagesBase
                )
                
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(BackupPayload::class.java)
                val json = adapter.toJson(payload)
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFile = File(backupDir, "Backup_$timestamp.json")
                backupFile.writeText(json)
                
                onComplete(true, backupFile.absolutePath)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message ?: "Unknown error")
            }
        }
    }

    fun restoreData(file: File, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = file.readText()
                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(BackupPayload::class.java)
                val payload = adapter.fromJson(json) ?: throw Exception("Invalid backup file")
                
                val restoredShopImagesMap = restorePhotos(payload.userAvatarBase64, payload.shopImagesBase64)

                payload.products.forEach { repository.insertProduct(it) }
                
                payload.shops.forEach { shop ->
                    val correctedShop = if (shop.imageUri != null) {
                        val filename = File(shop.imageUri).name
                        val newPath = restoredShopImagesMap[filename] ?: File(mediaDir, filename).let { 
                            if (it.exists()) it.absolutePath else null 
                        }
                        shop.copy(imageUri = newPath ?: shop.imageUri)
                    } else {
                        shop
                    }
                    repository.insertShop(correctedShop)
                }

                payload.orders.forEach { repository.insertOrder(it) }
                
                sharedPrefs.edit().apply {
                    putString("user_name", payload.userName)
                    putString("business_name", payload.businessName)
                    putString("user_phone", payload.userPhone)
                    putString("user_email", payload.userEmail)
                    putString("user_address", payload.userAddress)
                }.apply()
                
                userName.value = payload.userName
                businessName.value = payload.businessName
                userPhone.value = payload.userPhone
                userEmail.value = payload.userEmail
                userAddress.value = payload.userAddress
                
                onComplete(true, "Data restored successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message ?: "Unknown error")
            }
        }
    }

    fun getBackupFiles(): List<File> {
        return backupDir.listFiles()?.filter { it.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    suspend fun getDriveAccessToken(context: Context, email: String, onAuthRequired: (Intent) -> Unit): String? {
        return try {
            com.example.utils.GoogleDriveHelper.getAccessToken(context, email)
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            e.intent?.let { onAuthRequired(it) }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun backupToGoogleDrive(context: Context, onAuthRequired: (Intent) -> Unit, onComplete: (Boolean, String?) -> Unit) {
        val email = googleAccountEmail.value
        if (email == null) {
            onComplete(false, "No Google Account connected")
            return
        }
        viewModelScope.launch {
            isDriveLoading.value = true
            try {
                val token = getDriveAccessToken(context, email, onAuthRequired)
                if (token == null) {
                    isDriveLoading.value = false
                    return@launch
                }

                val products = repository.allProducts.first()
                val shops = repository.allShops.first()
                val orders = repository.allOrders.first()
                val (userAvatarBase64, shopImagesBase) = getPhotosBase64(shops)

                val payload = BackupPayload(
                    products = products,
                    shops = shops,
                    orders = orders,
                    userName = userName.value,
                    businessName = businessName.value,
                    userPhone = userPhone.value,
                    userEmail = userEmail.value,
                    userAddress = userAddress.value,
                    userAvatarBase64 = userAvatarBase64,
                    shopImagesBase64 = shopImagesBase
                )

                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(BackupPayload::class.java)
                val json = adapter.toJson(payload)

                try {
                    val success = com.example.utils.GoogleDriveHelper.uploadBackup(token, json)
                    if (success) {
                        val backups = com.example.utils.GoogleDriveHelper.listBackups(token)
                        googleDriveBackups.value = backups
                        onComplete(true, null)
                    } else {
                        onComplete(false, "Upload failed")
                    }
                } catch (ioe: java.io.IOException) {
                    if (ioe.message == "401 Unauthorized") {
                        try {
                            com.google.android.gms.auth.GoogleAuthUtil.clearToken(context, token)
                        } catch (clearEx: Exception) {
                            clearEx.printStackTrace()
                        }
                        val freshToken = getDriveAccessToken(context, email, onAuthRequired)
                        if (freshToken != null) {
                            val success = com.example.utils.GoogleDriveHelper.uploadBackup(freshToken, json)
                            if (success) {
                                val backups = com.example.utils.GoogleDriveHelper.listBackups(freshToken)
                                googleDriveBackups.value = backups
                                onComplete(true, null)
                            } else {
                                onComplete(false, "Upload failed")
                            }
                        } else {
                            onComplete(false, "Failed to get fresh token")
                        }
                    } else {
                        throw ioe
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message ?: "Unknown error")
            } finally {
                isDriveLoading.value = false
            }
        }
    }

    fun restoreFromGoogleDrive(context: Context, fileId: String, onAuthRequired: (Intent) -> Unit, onComplete: (Boolean, String?) -> Unit) {
        val email = googleAccountEmail.value
        if (email == null) {
            onComplete(false, "No Google Account connected")
            return
        }
        viewModelScope.launch {
            isDriveLoading.value = true
            try {
                val token = getDriveAccessToken(context, email, onAuthRequired)
                if (token == null) {
                    isDriveLoading.value = false
                    return@launch
                }

                var json: String? = null
                try {
                    json = com.example.utils.GoogleDriveHelper.downloadBackup(token, fileId)
                } catch (ioe: java.io.IOException) {
                    if (ioe.message == "401 Unauthorized") {
                        try {
                            com.google.android.gms.auth.GoogleAuthUtil.clearToken(context, token)
                        } catch (clearEx: Exception) {
                            clearEx.printStackTrace()
                        }
                        val freshToken = getDriveAccessToken(context, email, onAuthRequired)
                        if (freshToken != null) {
                            json = com.example.utils.GoogleDriveHelper.downloadBackup(freshToken, fileId)
                        }
                    } else {
                        throw ioe
                    }
                }

                if (json == null) {
                    onComplete(false, "Download failed")
                    return@launch
                }

                val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(BackupPayload::class.java)
                val payload = adapter.fromJson(json) ?: throw Exception("Invalid backup file format")

                val restoredShopImagesMap = restorePhotos(payload.userAvatarBase64, payload.shopImagesBase64)

                payload.products.forEach { repository.insertProduct(it) }
                
                payload.shops.forEach { shop ->
                    val correctedShop = if (shop.imageUri != null) {
                        val filename = File(shop.imageUri).name
                        val newPath = restoredShopImagesMap[filename] ?: File(mediaDir, filename).let { 
                            if (it.exists()) it.absolutePath else null 
                        }
                        shop.copy(imageUri = newPath ?: shop.imageUri)
                    } else {
                        shop
                    }
                    repository.insertShop(correctedShop)
                }

                payload.orders.forEach { repository.insertOrder(it) }
                
                sharedPrefs.edit().apply {
                    putString("user_name", payload.userName)
                    putString("business_name", payload.businessName)
                    putString("user_phone", payload.userPhone)
                    putString("user_email", payload.userEmail)
                    putString("user_address", payload.userAddress)
                }.apply()
                
                userName.value = payload.userName
                businessName.value = payload.businessName
                userPhone.value = payload.userPhone
                userEmail.value = payload.userEmail
                userAddress.value = payload.userAddress

                onComplete(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, e.message ?: "Unknown error")
            } finally {
                isDriveLoading.value = false
            }
        }
    }

    fun deleteGoogleDriveBackup(context: Context, fileId: String, onAuthRequired: (Intent) -> Unit, onComplete: (Boolean) -> Unit) {
        val email = googleAccountEmail.value ?: return
        viewModelScope.launch {
            isDriveLoading.value = true
            try {
                val token = getDriveAccessToken(context, email, onAuthRequired)
                if (token != null) {
                    try {
                        val success = com.example.utils.GoogleDriveHelper.deleteBackup(token, fileId)
                        if (success) {
                            val backups = com.example.utils.GoogleDriveHelper.listBackups(token)
                            googleDriveBackups.value = backups
                        }
                        onComplete(success)
                    } catch (ioe: java.io.IOException) {
                        if (ioe.message == "401 Unauthorized") {
                            try {
                                com.google.android.gms.auth.GoogleAuthUtil.clearToken(context, token)
                            } catch (clearEx: Exception) {
                                clearEx.printStackTrace()
                            }
                            val freshToken = getDriveAccessToken(context, email, onAuthRequired)
                            if (freshToken != null) {
                                val success = com.example.utils.GoogleDriveHelper.deleteBackup(freshToken, fileId)
                                if (success) {
                                    val backups = com.example.utils.GoogleDriveHelper.listBackups(freshToken)
                                    googleDriveBackups.value = backups
                                }
                                onComplete(success)
                            } else {
                                onComplete(false)
                            }
                        } else {
                            throw ioe
                        }
                    }
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            } finally {
                isDriveLoading.value = false
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
