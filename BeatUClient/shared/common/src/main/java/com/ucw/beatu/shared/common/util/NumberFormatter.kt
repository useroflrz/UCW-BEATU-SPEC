package com.ucw.beatu.shared.common.util

/**
 * 数字格式化工具类
 * 用于将大数字简化为易读的格式（如：5.6万、1.2亿）
 * 
 * 格式化规则：
 * - >= 1亿：显示为 "X.X亿"
 * - >= 1万：显示为 "X.X万"
 * - >= 1千：显示为 "X.X千"
 * - < 1千：显示原始数字
 */
object NumberFormatter {
    /**
     * 格式化数字显示（如：5.6万、1.2亿）
     * 
     * @param count 要格式化的数字
     * @return 格式化后的字符串
     */
    fun formatCount(count: Long): String {
        return when {
            count >= 100000000 -> String.format("%.1f亿", count / 100000000.0)
            count >= 10000 -> String.format("%.1f万", count / 10000.0)
            count >= 1000 -> String.format("%.1f千", count / 1000.0)
            else -> count.toString()
        }
    }
    
    /**
     * 格式化数字显示（Int 版本）
     * 
     * @param count 要格式化的数字
     * @return 格式化后的字符串
     */
    fun formatCount(count: Int): String {
        return formatCount(count.toLong())
    }
}

