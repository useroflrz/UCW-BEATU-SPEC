package com.ucw.beatu.core.database.converter

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

class Converters {
    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, String::class.java)
    private val adapter = moshi.adapter<List<String>>(listType)

    @TypeConverter
    fun toJson(list: List<String>): String = adapter.toJson(list)

    @TypeConverter
    fun fromJson(json: String): List<String> = adapter.fromJson(json) ?: emptyList()
}
