package com.zenchn.api.entity;

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import java.io.Serializable


@Parcelize
data class TokenEntity(
        @SerializedName("accessToken")
        val accessToken: String,
        @SerializedName("refreshToken")
        val refreshToken: String,
        @SerializedName("scope")
        val scope: String,
        @SerializedName("expiresIn")
        val expiresIn: Long

) : Parcelable

@Parcelize
data class UserEntity(
        val account: String? = null,
        var realName: String? = null,
        val sex: String? = "",
        val email: String? = null,
        val mobileNo: String? = null,
        val warningNoticeStatus: String? = null,
        val avatar: String? = null
) : Parcelable, Serializable
