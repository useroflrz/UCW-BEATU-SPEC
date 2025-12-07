package com.ucw.beatu.shared.common.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * 时间格式化工具
 * 将时间戳转换为友好的显示格式
 */
object TimeFormatter {

    /**
     * 将时间戳（毫秒）转换为友好的显示格式
     * 格式：相对时间 + 位置，如 "3天前 · 河北"
     * 
     * @param timestamp 时间戳（毫秒）
     * @param location 位置信息（可选），如 "河北"
     * @return 格式化后的时间字符串，如 "3天前 · 河北" 或 "刚刚"
     */
    fun formatRelativeTime(timestamp: Long, location: String? = null): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val relativeTime = when {
            diff < 60000 -> "刚刚" // 1 分钟内
            diff < 3600000 -> "${diff / 60000}分钟前" // 1 小时内
            diff < 86400000 -> "${diff / 3600000}小时前" // 24 小时内
            diff < 604800000 -> "${diff / 86400000}天前" // 7 天内
            diff < 2592000000 -> "${diff / 604800000}周前" // 30 天内
            diff < 31536000000 -> "${diff / 2592000000}个月前" // 1 年内
            else -> {
                // 超过 1 年，显示具体日期
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = timestamp
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                val commentYear = calendar.get(Calendar.YEAR)
                
                if (currentYear == commentYear) {
                    // 同年，显示月-日
                    SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
                } else {
                    // 不同年，显示年-月-日
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp))
                }
            }
        }
        
        return if (location != null && location.isNotBlank()) {
            "$relativeTime · $location"
        } else {
            relativeTime
        }
    }

    /**
     * 将时间戳（毫秒）转换为简单的相对时间格式（不包含位置）
     */
    fun formatSimpleRelativeTime(timestamp: Long): String {
        return formatRelativeTime(timestamp, null)
    }

    /**
     * 将时间戳（毫秒）转换为评论日期格式
     * 格式：今天显示相对时间（刚刚/X分钟前/X小时前），其他显示 MM-dd
     */
    fun formatCommentDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "刚刚" // 1 分钟内
            diff < 3600000 -> "${diff / 60000}分钟前" // 1 小时内
            diff < 86400000 -> "${diff / 3600000}小时前" // 24 小时内
            else -> {
                // 超过 24 小时，显示月-日格式
                SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }
}
