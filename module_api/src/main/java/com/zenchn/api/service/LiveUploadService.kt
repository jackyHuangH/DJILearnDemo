package com.zenchn.api.service

import android.content.Context
import com.zenchn.api.ApiManager
import com.zenchn.api.entity.AircraftLiveInfoEntity
import com.zenchn.api.entity.AircraftPhotoUploadEntity
import com.zenchn.api.entity.LiveFlowInfo
import com.zenchn.api.entity.ResponseModel
import com.zenchn.api.frame.RequestBodyBuilder
import com.zenchn.api.frame.toJSONString
import com.zenchn.common.utils.FileUtils
import com.zenchn.common.utils.LoggerKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import top.zibin.luban.Luban

/**
 * @author:Hzj
 * @date  :2021/6/23
 * desc  ：直播和文件上传接口
 * record：
 */
val liveUploadService by lazy { ApiManager.create(LiveUploadService::class.java) }

interface LiveUploadService {

    /**
     * 获取直播推流地址
     */
    @GET("equipment/live/askPushFlow")
    suspend fun askPushFlowInfo(@Query("equipmentFactoryCode") equipmentFactoryCode: String): ResponseModel<LiveFlowInfo>

    /**
     * 推流轨迹信息提交
     */
    @POST("equipment/live/pushFlowTrackSubmit")
    suspend fun submitFlowTrackInfo(@Body body: RequestBody): ResponseModel<Any>

    /**
     * 推流照片提交
     */
    @POST("equipment/live/pushFlowPhotoSubmit")
    suspend fun submitFlowPhoto(@Body body: RequestBody): ResponseModel<Any>
}

suspend fun LiveUploadService.submitLiveInfo(
    liveInfoEntity: AircraftLiveInfoEntity
): ResponseModel<Any> = withContext(Dispatchers.IO) {
    RequestBodyBuilder.build(liveInfoEntity.toJSONString()).let {
        liveUploadService.submitFlowTrackInfo(it)
    }
}


suspend fun LiveUploadService.submitLivePhoto(
    fileUploadList: List<AircraftPhotoUploadEntity>?,
    pushFlowId: String
): ResponseModel<Any> = withContext(Dispatchers.IO) {
    //若有图片先上传图片
    val fileParam = mutableMapOf<Long, String>()
    fileUploadList?.forEach { uploadEntity ->
        val fileObjectKey = OssUploadRepository.uploadSingleFile(uploadEntity.file)
        fileParam[uploadEntity.photoTime] = fileObjectKey
    }
    LoggerKit.d("fileParam:$fileParam")
    RequestBodyBuilder.create {
        put("photoMaps", fileParam)
        put("pushFlowId", pushFlowId)
    }.let {
        liveUploadService.submitFlowPhoto(it)
    }
}