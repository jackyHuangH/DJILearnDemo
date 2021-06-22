package com.zenchn.map

/**
 * @author:Hzj
 * @date  :2020/7/9
 * desc  ：
 * record：
 */

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.animation.BounceInterpolator
import android.view.animation.LinearInterpolator
import com.amap.api.maps.AMap
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker
import com.amap.api.maps.model.animation.ScaleAnimation
import com.amap.api.maps.model.animation.TranslateAnimation
import com.zenchn.common.utils.ApkUtils
import com.zenchn.common.utils.DisplayUtils
import java.net.URISyntaxException
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MapAppUtil {

    // 显示名称
    const val BAIDU_MAP_NAME = "百度地图-App导航"
    const val GAODE_MAP_NAME = "高德地图-App导航"

    // URL地址
    const val BAIDU_STORE_URL = "http://mobile.baidu.com/item/appsearch?pid=825114773"
    const val DOWN_LOAD_URL = "http://market.91.com/AndroidKit/1/Search/地图"

    // 距离判断
    const val WALKING_FIRST = 3000
    const val TRANSIT_FIRST = 5000

    // 百度地图导航类型
    const val BD_WALKING_MODE = "walking"
    const val BD_DRIVING_MODE = "driving"
    const val BD_TRANSIT_MODE = "transit"

    // 高德地图导航类型
    const val GD_WALKING_MODE = "4"
    const val GD_DRIVING_MODE = "2"
    const val GD_TRANSIT_MODE = "1"

    private var pi = 3.1415926535897932384626
    private var x_pi = 3.14159265358979324 * 3000.0 / 180.0
    private var a = 6378245.0
    private var ee = 0.00669342162296594323

    /**
     * 打开百度地图导航
     *
     * @param context
     * @param sLatLng
     * @param eLatLng
     */
    fun openBaiDuMap(context: Context, sLatLng: LatLng, eLatLng: LatLng) {

        if (ApkUtils.isAvailable(context, ApkUtils.AppPackage.BAIDU_MAP)) {
            try {
                //先将高德坐标转换成百度坐标
                val start_Bd09 = gcj02_To_Bd09(sLatLng.latitude, sLatLng.longitude)
                val end_Bd09 = gcj02_To_Bd09(eLatLng.latitude, eLatLng.longitude)

                val mode = BD_DRIVING_MODE
                val intent = Intent.parseUri(
                    "intent://map/direction?origin=latlng:${start_Bd09[0]},"
                            + start_Bd09[1] + "|name:我的位置&destination=latlng:" + end_Bd09[0] + ","
                            + end_Bd09[1] + "|name:目标位置&mode=" + mode
                            + "&src=thirdapp.navi.zenchn.electrombile#Intent;scheme=bdapp;package=com.baidu.BaiduMap;end",
                    0
                )

                context.startActivity(intent) // 启动调用

            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }

        }
    }

    /**
     * 打开高德地图导航
     *
     * @param context
     * @param sLatLng
     * @param eLatLng
     */
    fun openGaoDeMap(context: Context, sLatLng: LatLng, eLatLng: LatLng) {
        if (ApkUtils.isAvailable(context, ApkUtils.AppPackage.GAODE_MAP)) {
            val mode = GD_DRIVING_MODE
            val intent = Intent(
                "android.intent.action.VIEW",
                Uri.parse(
                    "androidamap://route?sourceApplication=正呈消防云&slat=" + sLatLng.latitude
                            + "&slon=" + sLatLng.longitude + "&sname=我的位置&dlat=" + eLatLng.latitude + "&dlon="
                            + eLatLng.longitude + "&dname=目标位置&dev=0&m=0&t=" + mode
                )
            )

            intent.setPackage(ApkUtils.AppPackage.GAODE_MAP)
            context.startActivity(intent) // 启动调用
        }
    }


    /**
     * 检测本机安装的地图App
     *
     * @param context
     * @return
     */
    fun checkLocalMapApp(context: Context): List<String> {
        val apps = ArrayList<String>()
        if (ApkUtils.isAvailable(context, ApkUtils.AppPackage.BAIDU_MAP)) {
            apps.add(BAIDU_MAP_NAME)
        }
        if (ApkUtils.isAvailable(context, ApkUtils.AppPackage.GAODE_MAP)) {
            apps.add(GAODE_MAP_NAME)
        }
        return apps
    }

    /**
     * 打开高德或者百度地图导航
     */
    fun openGaoDeOrBaiDuMapForNavigation(
        context: Context,
        startLatLng: LatLng,
        targetLatLng: LatLng,
        notInstallAppCallback: (() -> Unit)? = null
    ) {
        val localMapApp: List<String> = checkLocalMapApp(context)
        if (localMapApp.isNotEmpty()) {
            if (localMapApp.contains(GAODE_MAP_NAME)) {
                //优先打开高德地图导航
                openGaoDeMap(context, startLatLng, targetLatLng)
            } else if (localMapApp.contains(BAIDU_MAP_NAME)) {
                //打开百度地图导航
                openBaiDuMap(context, startLatLng, targetLatLng)
            }
        } else {
            //用户未安装高德和百度地图反馈
            notInstallAppCallback?.invoke()
        }
    }

    //--------------------------坐标系转换-------------------------------------------------

    /**
     * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法
     * 将 GCJ-02 坐标转换成 BD-09 坐标
     *
     * @param lat
     * @param lon
     */
    fun gcj02_To_Bd09(lat: Double, lon: Double): DoubleArray {
        val z = sqrt(lon * lon + lat * lat) + 0.00002 * sin(lat * x_pi)
        val theta = atan2(lat, lon) + 0.000003 * cos(lon * x_pi)
        val tempLon = z * cos(theta) + 0.0065
        val tempLat = z * sin(theta) + 0.006
        return doubleArrayOf(tempLat, tempLon)
    }

    /**
     * * 火星坐标系 (GCJ-02) 与百度坐标系 (BD-09) 的转换算法
     * 将 BD-09 坐标转换成GCJ-02 坐标 * * @param
     * bd_lat * @param bd_lon * @return
     */
    fun bd09_To_Gcj02(lat: Double, lon: Double): DoubleArray {
        val x = lon - 0.0065
        val y = lat - 0.006
        val z = sqrt(x * x + y * y) - 0.00002 * sin(y * x_pi)
        val theta = atan2(y, x) - 0.000003 * cos(x * x_pi)
        val tempLon = z * cos(theta)
        val tempLat = z * sin(theta)
        return doubleArrayOf(tempLat, tempLon)
    }

    //---------------------------------------------------------------------------------------------
    //------------------------------------高德地图 marker 动画----------------------------------------------

    /**
     * marker 跳动动画
     *
     * @param aMap
     * @param screenMarker
     */
    fun markerStartJump(aMap: AMap, screenMarker: Marker?) {
        if (screenMarker != null) {
            val latLng = screenMarker.position
            val point = aMap.projection.toScreenLocation(latLng)
            point.y -= DisplayUtils.dp2px(20F)
            val target = aMap.projection.fromScreenLocation(point)
            //使用TranslateAnimation,填写一个需要移动的目标点
            val animation = TranslateAnimation(target)
            animation.setInterpolator { input ->
                // 模拟重加速度的interpolator
                if (input <= 0.5) {
                    (0.5f - 2.0 * (0.5 - input) * (0.5 - input)).toFloat()
                } else {
                    (0.5f - sqrt(((input - 0.5f) * (1.5f - input)).toDouble())).toFloat()
                }
            }
            //整个移动所需要的时间
            animation.setDuration(1000)
            //设置动画
            screenMarker.setAnimation(animation)
            //开始动画
            screenMarker.startAnimation()
        }
    }

    /**
     * Marker 生长动画
     */
    fun markerStartGrowAnimation(growMarker: Marker?) {
        try {
            if (growMarker != null) {
                val animation = ScaleAnimation(0f, 1f, 0f, 1f)
                animation.setInterpolator(LinearInterpolator())
                //整个移动所需要的时间
                animation.setDuration(300)
                //设置动画
                growMarker.setAnimation(animation)
                //开始动画
                growMarker.startAnimation()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    /**
     * Marker 放大动画
     */
    fun markerZoomAnimation(marker: Marker?) {
        try {
            if (marker != null) {
                val animation = ScaleAnimation(0f, 1.5f, 0f, 1.5f)
                animation.setInterpolator(BounceInterpolator())
                //整个移动所需要的时间
                animation.setDuration(500)
                //设置动画
                marker.setAnimation(animation)
                //开始动画
                marker.startAnimation()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}
