package com.zenchn.djilearndemo.model.entity

import com.google.gson.annotations.SerializedName

/**
 * @author:Hzj
 * @date  :2019/7/1/001
 * desc  ：api 返回json数据格式封装
 * record：父类泛型对象可以赋值给子类泛型对象，用 in；子类泛型对象可以赋值给父类泛型对象，用 out。
 */
data class BaseResponse<out T>(

    @SerializedName(value = "code")
    val errorCode: Int,
    @SerializedName(value = "msg")
    val errorMsg: String,
    val data: T
)