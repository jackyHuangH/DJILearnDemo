package com.zenchn.djilearndemo.model.entity

/**
 * @author:Hzj
 * @date  :2020/7/6
 * desc  ：添加证件数据模型
 * record：
 */

/**
 * 图片上传返回结果数据实体
 */

data class UpPhotoEntity(
    val `file`: String,
    val filepath: String,
    val id: Any,
    val name: String,
    val preview_url: String,
    val url: String
)