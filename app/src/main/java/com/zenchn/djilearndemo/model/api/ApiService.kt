package com.zenchn.djilearndemo.model.api

import com.zenchn.djilearndemo.model.entity.BaseResponse
import com.zenchn.djilearndemo.model.entity.UpPhotoEntity
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * @author:Hzj
 * @date  :2019/7/2/002
 * desc  ：WanAndroid Api 接口
 * record：
 */
interface ApiService {
    companion object {
        const val BASE_URL = "http://"
        const val BASE_IMAGE_URL = ""
    }

    /**
     * 上传图片
     */
    @Multipart
    @POST("public/upload")
    suspend fun uploadPhoto(
        @Part("file") body: RequestBody,
        @Part file: MultipartBody.Part
    ): BaseResponse<UpPhotoEntity>
}

/**
 * 上传图片封装
 */
suspend fun ApiService.uploadPhotoByPath(filePath: String): BaseResponse<UpPhotoEntity> {
    return uploadPhoto(
        RequestBodyProvider.provideFormRequestBody("file"),
        RequestBodyProvider.provideMultipartRequestBody(filePath)
    )
}