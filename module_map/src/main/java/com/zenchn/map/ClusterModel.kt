package com.zenchn.map

import android.graphics.drawable.Drawable
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.Marker


/**
 * @author:Hzj
 * @date  :2020/7/9
 * desc  ：高德地图点聚合功能实现
 * record：一个聚合点，包含聚合点marker和聚合内部点的集合
 */
class Cluster(private val latLng: LatLng) {
    private val mClusterItems: MutableList<ClusterItem> = ArrayList()
    private var mMarker: Marker? = null

    fun addClusterItem(clusterItem: ClusterItem) {
        mClusterItems.add(clusterItem)
    }

    fun getClusterCount(): Int {
        return mClusterItems.size
    }

    fun getCenterLatLng(): LatLng {
        return latLng
    }

    fun setMarker(marker: Marker) {
        mMarker = marker
    }

    fun getMarker(): Marker? {
        return mMarker
    }

    fun getClusterItems(): MutableList<ClusterItem> {
        return mClusterItems
    }
}


/**
 * 返回聚合元素的地理位置
 * 实例：class RegionItem(private val latLng: LatLng, val title: String) : ClusterItem {
 *          override fun getPosition(): LatLng {
 *              return latLng
 *          }
 *       }
 */
interface ClusterItem {
    fun getPosition(): LatLng
}

/**
 * 根据聚合点的元素数目返回渲染背景样式
 */
interface ClusterRender {
    fun getDrawable(clusterItems: MutableList<ClusterItem>): Drawable?
}