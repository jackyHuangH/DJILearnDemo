package com.zenchn.djilearndemo.base

import android.os.Bundle
import androidx.annotation.IdRes
import com.amap.api.maps.MapView
import com.amap.api.maps.TextureMapView

/**
 * @author:Hzj
 * @date  :2019/7/29/029
 * desc  ：高德地图 activity 基类
 * record：
 */
interface IAMap {

    @IdRes
    fun getAMapViewIdRes(): Int

    fun initAMapView()
}

abstract class BaseAMapActivity<VM : BaseViewModel> : BaseVMActivity<VM>(), IAMap {
    protected var mMapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mMapView = findViewById<MapView>(getAMapViewIdRes())
        mMapView?.let { mapView ->
            mapView.onCreate(savedInstanceState)
            initAMapView()
        }
    }

    /**
     * 方法必须重写
     */
    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
    }

    /**
     * 方法必须重写
     */
    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
    }

    /**
     * 方法必须重写
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mMapView?.onSaveInstanceState(outState)
    }

    /**
     * 方法必须重写
     */
    override fun onDestroy() {
        super.onDestroy()
        mMapView?.onDestroy()
    }
}


/**
 * @author:Hzj
 * @date  :2019/7/29/029
 * desc  ：高德地图 fragment 基类
 * record：
 */
abstract class BaseAMapFragment<VM : BaseViewModel> : BaseVMFragment<VM>(), IAMap {

    protected var mMapView: TextureMapView? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mMapView = view?.findViewById(getAMapViewIdRes()) as TextureMapView
        mMapView?.let { mapView ->
            mapView.onCreate(savedInstanceState)
            initAMapView()
        }
    }

    /**
     * 方法必须重写
     */
    override fun onResume() {
        super.onResume()
        mMapView?.onResume()
    }

    /**
     * 方法必须重写
     */
    override fun onPause() {
        super.onPause()
        mMapView?.onPause()
    }

    /**
     * 方法必须重写
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mMapView?.onSaveInstanceState(outState)
    }

    /**
     * 方法必须重写
     */
    override fun onDestroy() {
        super.onDestroy()
        mMapView?.onDestroy()
    }
}