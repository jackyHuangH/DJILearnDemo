package com.zenchn.djilearndemo.ui

/**
 * @author:Hzj
 * @date  :2021/6/17
 * desc  ：播放回放页面
 * record：
 */
import android.app.Application
import com.zenchn.djilearndemo.R
import com.zenchn.djilearndemo.base.*
import com.zenchn.widget.viewClickListenerExt


class PlaybackActivity : BaseVMActivity<PlaybackViewModel>() {
    companion object {
        const val EXTRA_MEDIA_FILE = "EXTRA_MEDIA_FILE"
    }

    override fun getLayoutId(): Int = R.layout.activity_playback

    override fun initWidget() {
        initListener()
    }

    private fun initListener() {
        viewClickListenerExt(R.id.ibt_back) { onBackPressed() }
    }


    override fun onDestroy() {
        super.onDestroy()

    }

    override val onViewModelStartup: PlaybackViewModel.() -> Unit = {

    }
}

class PlaybackViewModel(application: Application) : BaseViewModel(application) {

}