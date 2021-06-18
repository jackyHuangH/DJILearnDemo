package com.jacky.support.utils

/**
 * Created by Hzj on 2017/8/15.
 * 编码相关工具类
 */
object CodeUtil {

    // 根据Unicode编码判断中文汉字和符号
    private fun isChinese(c: Char): Boolean {
        val ub = Character.UnicodeBlock.of(c)
        return ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                ub === Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
                ub === Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
                ub === Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
                ub === Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS ||
                ub === Character.UnicodeBlock.GENERAL_PUNCTUATION
    }

    // 判断中文汉字和符号
    fun isChinese(strName: String): Boolean {
        val ch = strName.toCharArray()
        for (i in ch.indices) {
            val c = ch[i]
            if (isChinese(c)) {
                return true
            }
        }
        return false
    }

    /**
     * 1.判断字符串是否仅为数字:
     *
     * @param str
     * @return
     */
    fun isNumeric(str: String): Boolean {
        var i = str.length
        while (--i >= 0) {
            if (!Character.isDigit(str[i])) {
                return false
            }
        }
        return true
    }
}