package com.zenchn.djilearndemo.ui

/**
 * @author:Hzj
 * @date  :2021/6/17
 * desc  ：播放回放页面
 * record：
 */
import android.app.AlertDialog
import android.app.Application
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import com.zenchn.djilearndemo.R
import com.zenchn.djilearndemo.app.ApplicationKit
import com.zenchn.djilearndemo.base.*
import dji.sdk.media.MediaFile
import dji.sdk.media.MediaManager


class PlaybackActivity : BaseVMActivity<PlaybackViewModel>() {
    companion object {
        const val EXTRA_MEDIA_FILE = "EXTRA_MEDIA_FILE"
    }

    override fun getLayoutId(): Int = R.layout.activity_playback

    override fun initWidget() {
        initListener()
    }

    private fun initListener() {
        viewClickListener(R.id.ibt_back) { onBackPressed() }

    }



    override val startObserve: PlaybackViewModel.() -> Unit = {

    }

    override fun onDestroy() {
        super.onDestroy()

    }
}

class PlaybackViewModel(application: Application) : BaseViewModel(application) {

}