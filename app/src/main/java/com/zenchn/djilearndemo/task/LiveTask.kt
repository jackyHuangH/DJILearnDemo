package com.zenchn.djilearndemo.task

import com.zenchn.common.utils.LoggerKit
import java.util.*

/**
 * @author:Hzj
 * @date  :2021/6/22
 * desc  ：TimerTask 上报飞机信息
 * record：
 */
class LiveUploadTask :TimerTask(){
    override fun run() {
        LoggerKit.d("开始上报任务了")
    }
}