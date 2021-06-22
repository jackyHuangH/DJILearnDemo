package com.zenchn.api.frame

import com.google.gson.Gson
import com.zenchn.api.entity.ResponseModel
import com.zenchn.common.ext.checkNotNull
import com.zenchn.common.utils.FileUtils
import com.zenchn.common.utils.isFileExist
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.MultipartBody.Companion.FORM
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.annotations.NotNull
import java.io.File

/**
 * Gson转成json String
 */
fun Any.toJSONString(): String? {
    return try {
        Gson().toJson(this)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


/**
 *  表单上传添加文件
 */
fun MultipartBody.Builder.addFileFormDataPart(
    paramName: String,
    filePath: String? = null,
    file: File? = null,
    mediaType: MediaType = MEDIA_TYPE_FILE
): MultipartBody.Builder = apply {
    if (FileUtils.isFileExist(filePath)) {
        File(filePath.checkNotNull())
    } else {
        file
    }?.takeIf { it.isFileExist() }?.let {
        val requestBody = it.asRequestBody(mediaType)
        addFormDataPart(paramName, it.name, requestBody)
    }
}

/**
 * 添加文件到请求体
 *
 * @param filePaths
 */
fun MultipartBody.Builder.addFileFormDataPart(
    paramName: String,
    vararg filePaths: String,
    mediaType: MediaType = MEDIA_TYPE_FILE
) {
    val multipartBody = RequestBodyBuilder.buildMultipartBody {
        for ((i, filePath) in filePaths.withIndex()) {
            if (FileUtils.isFileExist(filePath)) {
                val rawFile = File(filePath)
                val fileName = rawFile.name
                RequestBodyBuilder.build(rawFile, mediaType)?.let {
                    addFormDataPart(paramName + i, fileName, it)
                }
            }
        }
    }
    addFormDataPart(paramName, "multiFile", multipartBody)
}

/**
 * 添加文件到请求体
 *
 * @param files
 */
fun MultipartBody.Builder.addFileFormDataPart(
    paramName: String,
    vararg files: File,
    mediaType: MediaType = MEDIA_TYPE_FILE
) {
    val multipartBody = RequestBodyBuilder.buildMultipartBody {
        for ((i, rawFile) in files.withIndex()) {
            if (rawFile.isFileExist()) {
                val fileName = rawFile.name
                RequestBodyBuilder.build(rawFile, mediaType)?.let {
                    addFormDataPart(paramName + i, fileName, it)
                }
            }
        }
    }
    addFormDataPart(paramName, "multiFile", multipartBody)
}

internal val MEDIA_TYPE_JSON: MediaType = "application/json".toMediaType()
internal val MEDIA_TYPE_FILE: MediaType = "file/*".toMediaType()
internal val MEDIA_TYPE_IMAGE: MediaType = "image/*".toMediaType()

object RequestBodyBuilder {

    @Throws(IllegalStateException::class)
    fun create(@NotNull func: MutableMap<String, Any>.() -> Unit): RequestBody {
        return LinkedHashMap<String, Any>(8).apply(func).toJSONString()?.let {
            build(it)
        } ?: throw IllegalStateException("Error , Create requestBody failed !")
    }

    fun buildMultipartBody(@NotNull multipartOptions: MultipartBody.Builder.() -> Unit): RequestBody {
        return MultipartBody.Builder().setType(FORM).apply {
            multipartOptions(this)
        }.build()
    }

    fun buildFormBody(@NotNull partOptions: FormBody.Builder.() -> Unit): RequestBody {
        return FormBody.Builder().apply {
            partOptions(this)
        }.build()
    }

    /**
     * 创建一个json请求体
     *
     * @return
     */
    fun build(jsonStr: String?): RequestBody? {
        return jsonStr?.toRequestBody(MEDIA_TYPE_JSON)
    }

    /**
     * 创建一个文件请求体
     *
     * @return
     */
    fun build(file: File?, mediaType: MediaType = MEDIA_TYPE_FILE): RequestBody? {
        return file?.let {
            return if (it.exists() && it.isFile) {
                it.asRequestBody(mediaType)
            } else {
                null
            }
        }
    }

}

private suspend fun <T> tryCatchHttpRequest(
    @NotNull tryBlock: suspend CoroutineScope.() -> ResponseModel<T>,
    @NotNull catchBlock: suspend CoroutineScope.(Throwable) -> ResponseModel<T>
): ResponseModel<T> = coroutineScope {
    withContext(Dispatchers.IO) {
        try {
            tryBlock(this)
        } catch (e: Exception) {
            catchBlock(this, e)
        }
    }
}

