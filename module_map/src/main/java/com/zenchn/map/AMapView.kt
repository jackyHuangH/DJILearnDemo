package com.zenchn.map

import androidx.lifecycle.LifecycleOwner
import com.amap.api.maps.MapView
import com.zenchn.widget.ILayout
import com.zenchn.widget.LifecycleViewHolder

/**
 * @author:Hzj
 * @date  :2020/7/7
 * desc  ：高德地图初始化抽取
 * record：
 */

interface IAMapView : ILayout {
    val aMapViewHolder: ViewHolder

    abstract class ViewHolder : LifecycleViewHolder<MapView>() {

        override fun onResume(owner: LifecycleOwner) {
            view?.onResume()
        }

        override fun onPause(owner: LifecycleOwner) {
            super.onPause(owner)
            view?.onPause()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            //在activity执行onDestroy时执行mMapView.onDestroy()，销毁地图
            view?.onDestroy()
        }
    }
}

fun IAMapView.initMapView(block: (MapView.() -> Unit)? = null) = with(aMapViewHolder) {
    hostLifecycle()?.addObserver(this)
    view = findViewWithId<MapView>(getViewId())?.apply {
        block?.invoke(this)
    }
}