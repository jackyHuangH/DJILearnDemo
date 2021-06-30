package com.zenchn.djilearndemo.base

import androidx.lifecycle.MutableLiveData
import com.zenchn.api.entity.LiveFlowInfo

/**
 * @author:Hzj
 * @date  :2020/9/23
 * desc  ：全局共享数据管理
 * record：
 */

//--------------------全局共享的数据------------------------------
/**
 * 直播推流url
 */
val BaseViewModel.shareLiveFlowInfo: MutableLiveData<LiveFlowInfo> by lazy { MutableLiveData() }