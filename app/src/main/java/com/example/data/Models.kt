package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "products")
@JsonClass(generateAdapter = true)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val stock: Int = 0,
    val description: String = "",
    val unit: String = "Pcs"
)

@Entity(tableName = "shops")
@JsonClass(generateAdapter = true)
data class Shop(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val ownerName: String = "",
    val phone: String = "",
    val address: String = "",
    val imageUri: String? = null
)

@JsonClass(generateAdapter = true)
data class OrderItem(
    val productId: Int,
    val productName: String,
    val price: Double,
    val quantity: Int,
    val unit: String = "Pcs"
) {
    val totalLinePrice: Double
        get() = price * quantity
}

@Entity(tableName = "orders")
@JsonClass(generateAdapter = true)
data class Order(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopId: Int,
    val shopName: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val paidAmount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val isPaid: Boolean = false,
    val remarks: String = ""
) {
    val dueAmount: Double
        get() = totalAmount - paidAmount
}
