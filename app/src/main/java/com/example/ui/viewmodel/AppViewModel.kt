package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(
        database.productDao(),
        database.shopDao(),
        database.orderDao()
    )

    // User Profile persistence
    private val sharedPrefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    
    val userName = MutableStateFlow(sharedPrefs.getString("user_name", "Shariful Islam") ?: "Shariful Islam")
    val businessName = MutableStateFlow(sharedPrefs.getString("business_name", "শরীক ডিস্ট্রিবিউশন") ?: "শরীক ডিস্ট্রিবিউশন")
    val userPhone = MutableStateFlow(sharedPrefs.getString("user_phone", "01768899599") ?: "01768899599")
    val userEmail = MutableStateFlow(sharedPrefs.getString("user_email", "Facebook.com/shariful.uxd") ?: "Facebook.com/shariful.uxd")
    val userAddress = MutableStateFlow(sharedPrefs.getString("user_address", "ঢাকা, বাংলাদেশ") ?: "ঢাকা, বাংলাদেশ")

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

    // Dashboard Statistics
    val stats = combine(products, shops, orders) { prodList, shopList, orderList ->
        val totalSales = orderList.sumOf { it.totalAmount }
        val totalCollected = orderList.sumOf { it.paidAmount }
        val totalDue = orderList.sumOf { it.dueAmount }
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
    fun addShop(name: String, ownerName: String, phone: String, address: String) {
        viewModelScope.launch {
            repository.insertShop(Shop(name = name, ownerName = ownerName, phone = phone, address = address))
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
    fun createOrder(shopId: Int, shopName: String, items: List<OrderItem>, totalAmount: Double, paidAmount: Double, remarks: String = "") {
        viewModelScope.launch {
            val isPaid = paidAmount >= totalAmount
            val order = Order(
                shopId = shopId,
                shopName = shopName,
                items = items,
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                isPaid = isPaid,
                remarks = remarks
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
