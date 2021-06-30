package com.zenchn.api.service

import android.net.Uri
import android.util.Log
import com.alibaba.sdk.android.oss.ClientConfiguration
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.PutObjectResult
import com.zenchn.api.ApiManager
import com.zenchn.api.entity.ResponseModel
import com.zenchn.common.utils.FileUtils
import com.zenchn.common.utils.UriUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import retrofit2.http.GET
import java.io.File
import java.util.*

/**
 * @author:Hzj
 * @date  :2021/6/25
 * desc  ：阿里OSS文件上传服务
 * record：
 */

data class StsTokenInfo(
    val accessKeyId: String,
    val securityToken: String,
    val accessKeySecret: String,
    val expiration: Date,
    val bucketName: String,
    val endPoint: String
)

internal val ossStsService by lazy { ApiManager.create(OssSTSService::class.java) }

interface OssSTSService {
    /**
     * 描    述：获取oss临时令牌
     */
    @GET("alioss/getSTSToken")
    suspend fun getSTSToken(): ResponseModel<StsTokenInfo>
}

/**
 * 阿里云 oss 服务访问 颁发临时凭证
 */
object OssStsRepository {
    /***默认stsToken时效1小时***/
    private const val DEFAULT_STS_TIME_VALIDITY = 60 * 1000

    private var mStsTokenInfoCache: StsTokenInfo? = null

    /**
     * 校验临时凭证是否有效
     */
    private fun checkStsTokenValid(stsInfo: StsTokenInfo?) =
        stsInfo != null && (System.currentTimeMillis() <= stsInfo.expiration.time - DEFAULT_STS_TIME_VALIDITY)

    suspend fun getStsToken(): StsTokenInfo? {
        return if (checkStsTokenValid(mStsTokenInfoCache)) {
            mStsTokenInfoCache
        } else {
            ossStsService
                .getSTSToken().run {
                    mStsTokenInfoCache = this.data
                    this.data
                }
        }
    }
}

object OssUploadRepository {
    private const val TAG = "OssUploadRepository:"

    /***OSS上传路径 */
    private const val OSS_UPLOAD_PATH = "aircraft/"

    /**
     * 上传单个文件,回调在主线程
     */
    @ExperimentalCoroutinesApi
    suspend fun uploadFileWithCallback(
        filePath: String,
        onUploadSuccess: (String) -> Unit,
        onUploadFail: () -> Unit
    ) {
        val file = File(filePath)
        if (file.exists() && file.isFile) {
            uploadFileWithCallback(file, onUploadSuccess, onUploadFail)
        } else {
            onUploadFail()
        }
    }

    @ExperimentalCoroutinesApi
    suspend fun uploadFileWithCallback(file: File, onUploadSuccess: (String) -> Unit, onUploadFail: () -> Unit) =
        withContext(Dispatchers.Main) {//声明回调线程在主线程
            flowOf(uploadFileToOss(file))
                .flowOn(Dispatchers.IO)//指定流执行在io线程
                .catch { e ->
                    Log.e(TAG, "上传失败：${e.message}")
                    onUploadFail()
                }
                .collect { fileKey: String ->
                    Log.d(TAG, "上传成功：$fileKey")
                    onUploadSuccess(fileKey)
                }
        }

    suspend fun uploadSingleFile(filePath: String): String {
        val realFile = File(filePath)
        return uploadSingleFile(realFile)
    }

    suspend fun uploadSingleFile(file: File): String {
        return if (!file.exists() || !file.isFile) {
            throw IllegalStateException("file is not exist or is not a file")
        } else {
            uploadFileToOss(file)
        }
    }

    suspend fun uploadSingleFile(uri: Uri): String {
        return ApiManager.getApplicationContext()?.let { applicationContext ->
            UriUtils.getFilePathByUri(applicationContext, uri)?.run {
                uploadSingleFile(this)
            }
        }.orEmpty()
    }

    /**
     * 上传多个文件
     */
    suspend fun uploadMultiFileByUri(uriList: List<Uri>): List<String> {
        return uriList.map { uri: Uri ->
            uploadSingleFile(uri)
        }
    }

    suspend fun uploadMultiFileByPath(filePaths: List<String>): List<String> {
        return filePaths.map { filePath: String ->
            File(filePath)
        }.let { fileList ->
            uploadMultiFile(fileList)
        }
    }

    suspend fun uploadMultiFile(files: List<File>): List<String> {
        return files.map { file ->
            uploadFileToOss(file)
        }
    }

    /**
     * 上传文件到Oss,返回objectKey
     */
    private suspend fun uploadFileToOss(file: File): String {
        return OssStsRepository.getStsToken()?.let { stsTokenInfo ->
            val bucketName: String = stsTokenInfo.bucketName
            val accessKeyId: String = stsTokenInfo.accessKeyId
            val accessKeySecret: String = stsTokenInfo.accessKeySecret
            val securityToken: String = stsTokenInfo.securityToken
            val endPoint: String = stsTokenInfo.endPoint

            val credentialProvider: OSSCredentialProvider =
                OSSStsTokenCredentialProvider(accessKeyId, accessKeySecret, securityToken)

            val conf = ClientConfiguration()
            conf.connectionTimeout = 20 * 1000 // 连接超时，默认15秒

            conf.socketTimeout = 20 * 1000 // socket超时，默认15秒

            conf.maxConcurrentRequest = 6 // 最大并发请求书，默认5个

            conf.maxErrorRetry = 3 // 失败后最大重试次数，默认2次

            val objectKey = generateImageName(file)
            ApiManager.getApplicationContext()?.let { applicationContext ->
                val oss = OSSClient(applicationContext, endPoint, credentialProvider, conf)

                val put = PutObjectRequest(bucketName, objectKey, file.absolutePath)

                try {
                    val putObjectResult: PutObjectResult? = oss.putObject(put)
                    Log.d(TAG, "ETag:" + putObjectResult?.eTag)
                    Log.d(TAG, "requestId:" + putObjectResult?.requestId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            objectKey
        }.orEmpty()
    }

    /**
     * 生成图片名称
     */
    private fun generateImageName(file: File): String {
        val fileExtension = FileUtils.getFileExtension(file.absolutePath)
        return OSS_UPLOAD_PATH + UUID.randomUUID().toString() + "." + fileExtension
    }
}