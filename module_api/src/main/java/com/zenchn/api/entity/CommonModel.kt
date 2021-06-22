package com.zenchn.api.entity

import java.io.Serializable

data class ResponseModel<out T>(
        var path: String? = null,
        var status: Int = 0,//成功 默认 200，其他情况另定
        var message: String? = null,
        var throwable: Throwable? = null,
        val data: T? = null
)


data class PageListModel<T>(
        var total: Int = 0, //总数量
        var pages: Int = 0,//总页数
        var pageNum: Int = 0,//当前第几页
        var hasNextPage: Boolean = false,//是否有下一页
        var pageSize: Int = 0,//当前页数量
        var list: MutableList<T>? = null
) : Serializable

data class ListDataEntity<T>(var list: MutableList<T>? = null) : Serializable

//-----------------------------------oss文件实体-----------------------------

/**
 * OSS 文件实体
 */
data class OSSFile(
        val imgUrl: String? = null
)


