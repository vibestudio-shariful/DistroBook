package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AppRepository(
    private val productDao: ProductDao,
    private val shopDao: ShopDao,
    private val orderDao: OrderDao
) {
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allShops: Flow<List<Shop>> = shopDao.getAllShops()
    val allOrders: Flow<List<Order>> = orderDao.getAllOrders()

    fun getOrdersForShop(shopId: Int): Flow<List<Order>> {
        return orderDao.getOrdersForShop(shopId)
    }

    suspend fun insertProduct(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) {
        productDao.deleteProduct(product)
    }

    suspend fun insertShop(shop: Shop) {
        shopDao.insertShop(shop)
    }

    suspend fun updateShop(shop: Shop) {
        shopDao.updateShop(shop)
    }

    suspend fun deleteShop(shop: Shop) {
        shopDao.deleteShop(shop)
    }

    suspend fun insertOrder(order: Order, deductStock: Boolean = true): Long {
        val orderId = orderDao.insertOrder(order)
        if (deductStock) {
            // Deduct stock for each product in the order
            try {
                val productsList = productDao.getAllProducts().first()
                val items = order.items
                if (items != null) {
                    for (item in items) {
                        // Safe check for type if coming from corrupted restoration
                        if (item is OrderItem) {
                            val matchingProduct = productsList.find { it.id == item.productId }
                            if (matchingProduct != null) {
                                val updatedStock = (matchingProduct.stock - item.quantity).coerceAtLeast(0)
                                productDao.updateStock(matchingProduct.id, updatedStock)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return orderId
    }

    suspend fun updateOrder(order: Order) {
        orderDao.updateOrder(order)
    }

    suspend fun deleteOrder(order: Order) {
        orderDao.deleteOrder(order)
    }
}
