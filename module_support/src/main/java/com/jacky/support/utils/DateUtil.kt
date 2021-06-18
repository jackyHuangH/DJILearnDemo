package com.jacky.support.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

/**
 * @author hzj
 */
object TimeFormatUtil {
    const val HHmm = "HH:mm"
    const val yMd = "yyyy-MM-dd"
    const val Md = "MM-dd"
    const val YMDHms = "yyyy-MM-dd HH:mm:ss"
    const val ymdHm = "yyyy/MM/dd HH:mm"
    private const val ELEVEN_TIME_LENGTH = 11

    /**
     * 补齐时间戳，如果时间戳长度小于11，后面补0
     *
     * @param time
     * @return
     */
    private fun makeupTimeLong(time: Long): Long {
        return if (time.toString().length < ELEVEN_TIME_LENGTH) {
            time * 1000L
        } else
            time
    }

    fun getDateByFormat(time: Long, format: String?): String {
        val dateFormat = SimpleDateFormat(format)
        val date =
            Date(makeupTimeLong(time))
        return dateFormat.format(date)
    }

    /**
     * 格式化录制视频的时间 mm:ss
     *
     * @param duration
     * @return
     */
    fun getMinSecTime(duration: Long?): String {
        val format = SimpleDateFormat("mm:ss")
        val date = Date(duration!!)
        return format.format(date)
    }

    fun getHm(time: Long): String {
        val format = SimpleDateFormat(HHmm)
        val date = Date(makeupTimeLong(time))
        return format.format(date)
    }

    fun getRegistrationTime(time: Long): String {
        val format = SimpleDateFormat(ymdHm)
        val date = Date(makeupTimeLong(time))
        return format.format(date)
    }

    fun getMd(time: Long): String {
        val format = SimpleDateFormat(Md)
        val date = Date(makeupTimeLong(time))
        return format.format(date)
    }

    /**
     * 返回 yyyy-MM-dd 格式日期
     *
     * @param time
     * @return
     */
    fun getyMd(time: Long): String {
        val format = SimpleDateFormat(yMd)
        val date = Date(makeupTimeLong(time))
        return format.format(date)
    }

    fun getyMdHm(time: Long): String {
        val format = SimpleDateFormat(ymdHm)
        val date = Date(makeupTimeLong(time))
        return format.format(date)
    }

    fun getyMdHms(time: Long): String {
        val format = SimpleDateFormat(YMDHms)
        val date = Date(makeupTimeLong(time))
        return format.format(date)
    }

    private const val minute = 60 * 1000L // 1分钟
    private const val hour = 60 * minute // 1小时
    private const val day = 24 * hour // 1天
    private const val month = 31 * day // 月
    private const val year = 12 * month // 年

    /**
     * 将日期格式化成友好的字符串：几分钟前、几小时前、几天前、几月前、几年前、刚刚
     *
     * @param previousTime
     * @return
     */
    fun getFormerlyTime(previousTime: String?): String {
        val oldTime = java.lang.Long.valueOf(previousTime!!)
        if (oldTime == 0L) {
            return ""
        }
        val now = System.currentTimeMillis()
        val diff = now - oldTime * 1000
        var r: Long = 0
        if (diff > year) {
            r = diff / year
            return "${r}年前"
        }
        if (diff > month) {
            r = diff / month
            return "${r}个月前"
        }
        if (diff > day) {
            r = diff / day
            return "${r}天前"
        }
        if (diff > hour) {
            r = diff / hour
            return "${r}个小时前"
        }
        if (diff > minute) {
            r = diff / minute
            return "${r}分钟前"
        }
        return "刚刚"
    }

    /**
     * 获取小时
     *
     * @return
     */
    val nowHour: Int
        get() {
            var hour = 0
            try {
                val formatHour = SimpleDateFormat("HH")
                hour = Integer.valueOf(formatHour.format(Date()))
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return hour
        }

    /**
     * 把标准格式yyyy-MM-dd HH:mm日期转成时间戳,单位秒
     *
     * @param time
     * @return long
     */
    fun getTimeToTimeStamp(time: String): Long {
        var timeStamp: Long = 0
        try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm")
            val date = format.parse(time)
            timeStamp = date.time
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return timeStamp / 1000
    }

    /**
     * 计算两个日期之间相差的天数
     *
     * @param startDate 较小的时间
     * @param endDate  较大的时间
     * @return 相差天数
     * @throws ParseException
     */
    @Throws(ParseException::class)
    fun daysBetween(startDate: Date, endDate: Date): Int {
        if ((startDate.time - endDate.time) > 0) {
            return 0
        }
        var smdate = startDate
        var bdate = endDate
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        smdate = sdf.parse(sdf.format(smdate))
        bdate = sdf.parse(sdf.format(bdate))
        val cal = Calendar.getInstance()
        cal.time = smdate
        val time1 = cal.timeInMillis
        cal.time = bdate
        val time2 = cal.timeInMillis
        val betweenDays = (time2 - time1) / (1000 * 3600 * 24)
        return betweenDays.toInt()
    }

    /**
     * 将倒计时的时间格式化成分:秒"mm:ss"的格式
     *
     * @param millisInfuture 剩余倒计时时长,单位:毫秒
     * @return
     */
    fun formatCountTimer(millisInfuture: Long): String {
        var secondStr = ""
        var minStr = ""
        val remainTime = millisInfuture / 1000
        val second = remainTime % 60
        val min = remainTime / 60
        secondStr = if (second < 10) {
            "0$second"
        } else {
            second.toString() + ""
        }
        minStr = if (min < 10) {
            "0$min"
        } else {
            min.toString() + ""
        }
        return "$minStr:$secondStr"
    }
}

fun Date.toCalendar(): Calendar = Calendar.getInstance().apply {
    time = this@toCalendar
}