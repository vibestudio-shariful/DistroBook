package com.example.data

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val listType = Types.newParameterizedType(List::class.java, OrderItem::class.java)
    private val adapter = moshi.adapter<List<OrderItem>>(listType)

    @TypeConverter
    fun fromOrderItemList(items: List<OrderItem>?): String? {
        return items?.let { adapter.toJson(it) }
    }

    @TypeConverter
    fun toOrderItemList(json: String?): List<OrderItem>? {
        return json?.let { adapter.fromJson(it) }
    }
}
