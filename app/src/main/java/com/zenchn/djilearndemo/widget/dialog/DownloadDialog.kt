package com.zenchn.djilearndemo.widget.dialog

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.jacky.support.setOnAntiShakeClickListener
import com.zenchn.djilearndemo.R
import com.zenchn.djilearndemo.base.childViewExt

/**
 * @author:Hzj
 * @date  :2021/6/4
 * desc  ：文件下载
 * record：
 */
class DownloadDialog(private val onCancelListener: () -> Unit) : BaseDialogFragment() {
    private lateinit var mProgress: ProgressBar
    private lateinit var mTvProgress: TextView

    override val layoutId: Int = R.layout.dialog_file_download

    fun updateProgress(progress: Int) {
        mProgress.progress = progress
        mTvProgress.text = "${progress}%"
    }

    override fun initViews(view: View) {
        view.apply {
            mProgress = findViewById<ProgressBar>(R.id.pb)
            mTvProgress = findViewById<TextView>(R.id.tv_progress)
            childViewExt<TextView>(R.id.tv_cancel) {
                setOnAntiShakeClickListener { onCancelListener.invoke() }
            }
        }
        dialog?.apply {
            setOnShowListener {
                updateProgress(0)
            }
            setOnCancelListener {
                onCancelListener.invoke()
            }
        }
    }
}