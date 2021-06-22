package com.zenchn.djilearndemo.ui

import android.app.AlertDialog
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdate
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.*
import com.zenchn.common.ext.orNotNullNotEmpty
import com.zenchn.djilearndemo.R
import com.zenchn.djilearndemo.app.ApplicationKit
import com.zenchn.djilearndemo.base.*
import com.zenchn.djilearndemo.event.AircraftConnectEvent
import com.zenchn.map.IAMapView
import com.zenchn.map.initMapView
import com.zenchn.widget.viewClickListenerExt
import dji.common.error.DJIError
import dji.common.mission.waypoint.*
import dji.sdk.flightcontroller.FlightController
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener
import dji.sdk.products.Aircraft
import dji.sdk.sdkmanager.DJISDKManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import kotlin.math.atan2


/**
 * @author:Hzj
 * @date  :2021/6/11
 * desc  ：航点任务页面
 * record：
 */
class WaypointActivity : BaseVMActivity<WaypointViewModel>(), IAMapView, AMap.OnMapClickListener {
    private val TAG = "WayPoint"
    private var droneLocationLat = 30.318596
    private var droneLocationLng: Double = 120.060183
    private var droneMarker: Marker? = null
    private var mFlightController: FlightController? = null
    private var mIsAddMode = false
    private val mMarkers = LinkedList<Marker>()
    private val mPolylines = LinkedList<Polyline>()
    private var mLastWaypoint: LatLng? = null//记录上一个点击点

    private var mAltitude = 100.0f
    private var mSpeed = 10.0f
    private var mHeadingMode = WaypointMissionHeadingMode.AUTO
    private val mWaypointList: MutableList<Waypoint> = mutableListOf()
    private val waypointMissionBuilder by lazy { WaypointMission.Builder() }
    private val waypointMissionOperator by lazy { DJISDKManager.getInstance().missionControl.waypointMissionOperator }
    private var mFinishedAction: WaypointMissionFinishedAction = WaypointMissionFinishedAction.NO_ACTION
    private var mAMap: AMap? = null

    private val eventNotificationListener: WaypointMissionOperatorListener = object : WaypointMissionOperatorListener {
        override fun onDownloadUpdate(downloadEvent: WaypointMissionDownloadEvent) {}
        override fun onUploadUpdate(uploadEvent: WaypointMissionUploadEvent) {}
        override fun onExecutionUpdate(executionEvent: WaypointMissionExecutionEvent) {}
        override fun onExecutionStart() {}
        override fun onExecutionFinish(@Nullable error: DJIError?) {
            showMessage("Execution finished: " + if (error == null) "Success!" else error.description)
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_waypoint

    override val aMapViewHolder: IAMapView.ViewHolder = object : IAMapView.ViewHolder() {
        override fun getViewId(): Int = R.id.map_view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aMapViewHolder.view?.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //在activity执行onSaveInstanceState时执行mMapView.onSaveInstanceState (outState)，保存地图当前的状态
        aMapViewHolder.view?.onSaveInstanceState(outState)
    }

    override fun initWidget() {
        EventBus.getDefault().register(this)
        waypointMissionOperator.addListener(eventNotificationListener)
        initAMapView()
        viewClickListenerExt(R.id.ibt_back) { onBackPressed() }
        viewClickListenerExt(R.id.bt_config) {
            showSettingDialog()
        }
        viewClickListenerExt(R.id.bt_locate) {
            if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                updateDroneLocation()
                cameraUpdate()
            } else {
                showMessage("当前定位坐标无效：lat$droneLocationLat,lng$droneLocationLng")
            }
        }
        viewClickListenerExt(R.id.bt_add) {
            mIsAddMode = mIsAddMode.not()
            (it as? Button)?.text = if (mIsAddMode) "Exit" else "Add"
        }
        viewClickListenerExt(R.id.bt_previous) {
            lifecycleScope.launch(Dispatchers.IO) {
                //后退一步
                mMarkers.pollLast()?.remove()
                mMarkers.peekLast()?.apply {
                    changeMarkerIcon(this, BitmapDescriptorFactory.fromResource(R.drawable.ic_go_now))
                }
                mLastWaypoint = mMarkers.peekLast()?.position
                mPolylines.pollLast()?.remove()
            }
        }
        viewClickListenerExt(R.id.bt_clear) {
            mAMap?.clear()
            mWaypointList.clear()
            mLastWaypoint = null
            mMarkers.clear()
            mPolylines.clear()
            waypointMissionBuilder.waypointList(mWaypointList)
            updateDroneLocation()
        }
        viewClickListenerExt(R.id.bt_upload) { uploadWayPointMission() }
        viewClickListenerExt(R.id.bt_start) { startWaypointMission() }
        viewClickListenerExt(R.id.bt_stop) { stopWaypointMission() }
    }

    fun initAMapView() {
        initMapView {
            mAMap = this.map
            //初始化地图控制器对象
            this.map.apply {
                setOnMapClickListener(this@WaypointActivity)
                //杭州 120.060183,30.318596
                val hangzhou = LatLng(30.308596, 120.060183)
                addMarker(MarkerOptions().position(hangzhou).title("Marker in hangzhou"))
                moveCamera(CameraUpdateFactory.newLatLng(hangzhou))
            }
        }
    }

    override fun onMapClick(point: LatLng) {
        if (mIsAddMode) {
            markWaypoint(point)
            val waypoint = Waypoint(point.latitude, point.longitude, mAltitude)
            //Add Waypoints to Waypoint arraylist;
            mWaypointList.add(waypoint)
            waypointMissionBuilder.waypointList(mWaypointList).waypointCount(mWaypointList.size)
        } else {
            showMessage("Cannot add waypoint")
        }
    }

    //上传航点任务到飞机
    private fun uploadWayPointMission() {
        waypointMissionOperator.uploadMission { error ->
            if (error == null) {
                showMessage("Mission upload successfully!")
            } else {
                showMessage("Mission upload failed, error: " + error.description + " retrying...")
                waypointMissionOperator.retryUploadMission(null)
            }
        }
    }

    private fun startWaypointMission() {
        waypointMissionOperator.startMission { error ->
            showMessage("Mission Start: " + if (error == null) "Successfully" else error.description)
        }
    }

    private fun stopWaypointMission() {
        waypointMissionOperator.stopMission { error ->
            showMessage("Mission Stop: " + if (error == null) "Successfully" else error.description)
        }
    }

    //添加航点marker,并绘制轨迹
    private fun markWaypoint(point: LatLng) {
        lifecycleScope.launch(Dispatchers.IO) {
            //Create MarkerOptions object
            val markerOptions = MarkerOptions().position(point)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_go_now))
            mAMap?.apply {
                val marker: Marker = addMarker(markerOptions)
                mMarkers.peekLast()?.apply {
                    changeMarkerIcon(this, BitmapDescriptorFactory.fromResource(R.drawable.ic_go))
                }
                mMarkers.add(marker)
                mLastWaypoint?.let { lastPoint ->
                    //添加航点轨迹
                    val polyline = addPolyline(
                        PolylineOptions().add(lastPoint, point).width(6F)
                            .color(ContextCompat.getColor(this@WaypointActivity, R.color.colorAccent))
                    )
                    mPolylines.add(polyline)
                    if (mMarkers.size > 2) {
                        //计算2点角度，旋转倒数第二个marker
                        val rotation = rotation(lastPoint, point)
                        Log.d(TAG, "rotation:$rotation")
                        mMarkers[mMarkers.size - 2].rotateAngle = rotation
                    }
                }
            }
            mLastWaypoint = point
        }
    }

    private fun changeMarkerIcon(marker: Marker, descriptor: BitmapDescriptor) {
        marker.options.icon.bitmap.recycle()
        marker.setIcon(descriptor)
        //触发地图立即刷新
        mAMap?.runOnDrawFrame()
    }

    // 计算两点间的角度
    private fun rotation(start: LatLng, end: LatLng): Float {
        val diffX = end.longitude - start.longitude
        val diffY = end.latitude - start.latitude
        return (360 * atan2(diffY, diffX) / (2 * Math.PI) + 90).toFloat() + 180F
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onConnectivityChange(event: AircraftConnectEvent?) {
        event?.let {
            initFlightController()
        }
    }

    private fun initFlightController() {
        val product = ApplicationKit.getProductInstance()
        if (product != null && product.isConnected) {
            if (product is Aircraft) {
                mFlightController = product.flightController
            }
        }
        //获取飞机定位坐标
        mFlightController?.setStateCallback { currentState ->
            if (checkGpsCoordination(currentState.aircraftLocation.latitude, currentState.aircraftLocation.longitude)) {
                droneLocationLat = currentState.aircraftLocation.latitude
                droneLocationLng = currentState.aircraftLocation.longitude
            }
            updateDroneLocation()
        }
    }

    private fun checkGpsCoordination(latitude: Double, longitude: Double): Boolean {
        return latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180 && latitude != 0.0 && longitude != 0.0
    }

    private fun updateDroneLocation() {
        if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
            val pos = LatLng(droneLocationLat, droneLocationLng)
            //Create MarkerOptions object
            val markerOptions =
                MarkerOptions().position(pos).icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft))
            droneMarker?.remove()
            droneMarker = mAMap?.addMarker(markerOptions)
        }
    }

    private fun cameraUpdate() {
        val pos = LatLng(droneLocationLat, droneLocationLng)
        val cu: CameraUpdate = CameraUpdateFactory.newLatLngZoom(pos, 18F)
        mAMap?.moveCamera(cu)
    }

    private fun showSettingDialog() {
        val wayPointSettings: LinearLayout =
            layoutInflater.inflate(R.layout.dialog_waypointsetting, null) as LinearLayout
        val tvWpAltitude: TextView = wayPointSettings.findViewById(R.id.altitude) as TextView
        val rgSpeed: RadioGroup = wayPointSettings.findViewById(R.id.speed) as RadioGroup
        val rgActionAfterFinished: RadioGroup = wayPointSettings.findViewById(R.id.actionAfterFinished) as RadioGroup
        val rgHeading: RadioGroup = wayPointSettings.findViewById(R.id.heading) as RadioGroup
        rgSpeed.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.lowSpeed -> {
                    mSpeed = 3.0f
                }
                R.id.MidSpeed -> {
                    mSpeed = 5.0f
                }
                R.id.HighSpeed -> {
                    mSpeed = 10.0f
                }
            }
        }
        rgActionAfterFinished.setOnCheckedChangeListener { group, checkedId ->
            Log.d(TAG, "Select action action")
            when (checkedId) {
                R.id.finishNone -> {
                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                }
                R.id.finishGoHome -> {
                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                }
                R.id.finishAutoLanding -> {
                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                }
                R.id.finishToFirst -> {
                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                }
            }
        }
        rgHeading.setOnCheckedChangeListener { group, checkedId ->
            Log.d(TAG, "Select heading finish")
            when (checkedId) {
                R.id.headingNext -> {
                    mHeadingMode = WaypointMissionHeadingMode.AUTO
                }
                R.id.headingInitDirec -> {
                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION
                }
                R.id.headingRC -> {
                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER
                }
                R.id.headingWP -> {
                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING
                }
            }
        }

        AlertDialog.Builder(this)
            .setTitle("")
            .setView(wayPointSettings)
            .setPositiveButton("Finish") { dialog, id ->
                val altitudeString = tvWpAltitude.text.toString().orNotNullNotEmpty("0")
                mAltitude = altitudeString.toFloat()
                Log.d(TAG, "altitude $mAltitude")
                Log.d(TAG, "speed $mSpeed")
                Log.d(TAG, "mFinishedAction $mFinishedAction")
                Log.d(TAG, "mHeadingMode $mHeadingMode")
                configWayPointMission()
            }
            .setNegativeButton("Cancel") { dialog, id -> dialog.cancel() }
            .create()
            .show()
    }

    private fun configWayPointMission() {
        waypointMissionBuilder.finishedAction(mFinishedAction)
            .headingMode(mHeadingMode)
            .autoFlightSpeed(mSpeed)
            .maxFlightSpeed(mSpeed)
            .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
        if (waypointMissionBuilder.waypointList.size > 0) {
            for (i in waypointMissionBuilder.waypointList.indices) {
                waypointMissionBuilder.waypointList[i].altitude = mAltitude
            }
            showMessage("Set Waypoint attitude successfully")
        }
        val error = waypointMissionOperator.loadMission(waypointMissionBuilder.build())
        if (error == null) {
            showMessage("loadWaypoint succeeded")
        } else {
            showMessage("loadWaypoint failed " + error.description)
        }
    }

    override val onViewModelStartup: WaypointViewModel.() -> Unit = {

    }


    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
        waypointMissionOperator.removeListener(eventNotificationListener)
    }
}

class WaypointViewModel(application: Application) : BaseViewModel(application) {

}