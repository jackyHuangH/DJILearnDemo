package com.zenchn.common

import android.os.Environment

/**
 * @author:Hzj
 * @date  :2020/9/15
 * desc  ：
 * record：
 */
object SupportConfig {
    const val DEFAULT_TAG = "zenchn"

    // #crash 是否收集报错日志
    const val isReport = true
    val FILE_PATH = Environment.getExternalStorageDirectory().path + "/zenchn/library/log/"
    const val FILE_NAME_PREFIX = "crash"
    const val FILE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
    const val FILE_NAME_SUFFIX = ".log"
}


//全局的日志前缀
internal const val GLOBAL_LOG_TAG = "AJYJ"

//# 默认时间格式
internal const val SIMPLE_DATE_FORMAT_TEMPLATE = "yyyy-MM-dd HH:mm:ss"

//硬盘缓存的文件夹
internal const val DEFAULT_CACHE_DIR = " /zenchn/files"