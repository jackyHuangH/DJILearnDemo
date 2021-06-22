package com.zenchn.djilearndemo.ui

import android.app.AlertDialog
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.zenchn.common.utils.DisplayUtils
import com.zenchn.common.utils.LoggerKit
import com.zenchn.djilearndemo.R
import com.zenchn.djilearndemo.app.ApplicationKit
import com.zenchn.djilearndemo.base.*
import com.zenchn.djilearndemo.ui.adapter.FileListAdapter
import com.zenchn.djilearndemo.widget.dialog.DownloadDialog
import com.zenchn.widget.viewClickListenerExt
import com.zenchn.widget.viewExt
import com.zenchn.widget.viewInVisibleExt
import com.zenchn.widget.viewVisibleExt
import dji.common.camera.SettingsDefinitions
import dji.common.error.DJICameraError
import dji.common.error.DJIError
import dji.common.util.CommonCallbacks
import dji.common.util.CommonCallbacks.CompletionCallback
import dji.common.util.CommonCallbacks.CompletionCallbackWithTwoParam
import dji.sdk.media.*
import dji.sdk.media.FetchMediaTask
import dji.sdk.media.MediaManager.*
import kotlinx.coroutines.*
import java.io.File
import java.util.*


/**
 * @author:Hzj
 * @date :2021/6/3
 * desc  ：管理视频、图片媒体文件
 * record：
 */
class MediaManageActivity : BaseActivity() {
    companion object {
        const val TAG = "MediaManageActivity"
    }

    private val mListAdapter: FileListAdapter by lazy { FileListAdapter() }
    private var mediaFileList = mutableListOf<MediaFile>()
    private var mMediaManager: MediaManager? = null
    private var currentFileListState = FileListState.UNKNOWN
    private var scheduler: FetchMediaTaskScheduler? = null
    private var lastClickViewIndex = -1
    private var lastClickView: View? = null
    private var mMediaType: SettingsDefinitions.StorageLocation? = null

    private val mDownloadDialog by lazy {
        DownloadDialog {
            mMediaManager?.exitMediaDownloading()
        }.setWidth(DisplayUtils.dp2px(360))
    }
    private val destDir: File = File(Environment.getExternalStorageDirectory().getPath().toString() + "/DJiMedia/")
    private var currentProgress = -1

    override fun getLayoutId(): Int = R.layout.activity_media_manage

    override fun initWidget() {
        initListener()
        //Init RecyclerView
        viewExt<RecyclerView>(R.id.rv_file_list) {
            val layoutManager = LinearLayoutManager(this@MediaManageActivity)
            setLayoutManager(layoutManager)
            adapter = mListAdapter.apply {
                setOnItemClickListener { adapter, view, position ->
                    //点击缩略图
                    val selectedMedia = adapter.data[position] as? MediaFile
                    if (selectedMedia != null && mMediaManager != null) {
                        addMediaTask(selectedMedia)
                    }
                    updateSelected(position)
                    lastClickViewIndex = position
                    lastClickView = view
                }
            }
        }
        initMediaManager()
    }

    private fun testDownloadDialog() {
        mDownloadDialog.show(supportFragmentManager, "")
        lifecycleScope.launch(Dispatchers.IO) {
            var p = 0
            while (p < 100) {
                p += 10
                launch(Dispatchers.Main) {
                    LoggerKit.d("update P:$p")
                    (mDownloadDialog as? DownloadDialog)?.updateProgress(p)
                    if (p >= 100) {
                        mDownloadDialog.dismiss()
                    }
                }
                delay(1000)
                LoggerKit.d("p:$p")
            }
        }
    }

    private fun initListener() {
        viewClickListenerExt(R.id.back_btn) { onBackPressed() }
        viewClickListenerExt(R.id.delete_btn) {
            if (checkSelectState()) {
                deleteFileByIndex(lastClickViewIndex)
            }
        }
        viewClickListenerExt(R.id.reload_btn) {
            if (ApplicationKit.aircraftIsConnect().not()) {
                showMessage("飞机未连接")
                return@viewClickListenerExt
            }
            lifecycleScope.launch(Dispatchers.IO) {
                showProgress()
                getFileList()
            }
        }
        viewClickListenerExt(R.id.download_btn) {
            if (checkSelectState()) {
                downloadFileByIndex(lastClickViewIndex)
            }
        }
        viewClickListenerExt(R.id.play_btn) {
            playVideo()
        }
        viewClickListenerExt(R.id.resume_btn) {
            mMediaManager?.resume { error ->
                if (null != error) {
                    showMessage("Resume Video Failed" + error.description)
                } else {
                    Log.d(TAG, "Resume Video Success")
                }
            }
        }
        viewClickListenerExt(R.id.pause_btn) {
            mMediaManager?.pause { error ->
                if (null != error) {
                    showMessage("Pause Video Failed" + error.description)
                } else {
                    Log.d(TAG, "Pause Video Success")
                }
            }
        }
        viewClickListenerExt(R.id.stop_btn) {
            //停止回放
            mMediaManager?.stop { error ->
                if (null != error) {
                    showMessage("Stop Video Failed" + error.description)
                } else {
                    Log.d(TAG, "Stop Video Success")
                }
            }
        }
        viewClickListenerExt(R.id.moveTo_btn) {
            moveToPosition()
        }
        viewClickListenerExt(R.id.btn_status) {
            viewExt<ScrollView>(R.id.pointing_drawer_content) {
                visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }
        }
    }

    private fun checkSelectState(): Boolean {
        if (lastClickViewIndex < 0) {
            showMessage("请选择文件")
            return false
        }
        return true
    }

    private val updateFileListStateListener =
        FileListStateListener { state -> currentFileListState = state }

    private fun initMediaManager() {
        if (ApplicationKit.getProductInstance() == null) {
            mListAdapter.data.clear()
            mListAdapter.notifyDataSetChanged()
            Log.d(TAG, "Product disconnected")
            return
        } else {
            ApplicationKit.getCameraInstance()?.let { cameraInstance ->
                if (cameraInstance.isMediaDownloadModeSupported) {
                    mMediaManager = cameraInstance.mediaManager
                    if (null != mMediaManager) {
                        mMediaManager?.addUpdateFileListStateListener(updateFileListStateListener)
                        if (mMediaManager?.isVideoPlaybackSupported == true) {
                            mMediaManager?.addMediaUpdatedVideoPlaybackStateListener(updatedVideoPlaybackStateListener);
                            Log.d(TAG, "Camera support video playback!")
                        } else {
                            showMessage("Camera does not support video playback!")
                        }
                        cameraInstance.getStorageLocation(object :
                            CommonCallbacks.CompletionCallbackWith<SettingsDefinitions.StorageLocation> {
                            override fun onSuccess(storageLocation: SettingsDefinitions.StorageLocation?) {
                                mMediaType = storageLocation
                            }

                            override fun onFailure(p0: DJIError?) {
                            }
                        })
                        LoggerKit.d("cameraInstance.isFlatCameraModeSupported:${cameraInstance.isFlatCameraModeSupported}")
                        cameraInstance.setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD) { error ->
                            if (error == null) {
                                LoggerKit.d("Set camera download Mode success")
                                showProgress()
                                getFileList()
                            } else {
                                showMessage("Set cameraMode failed")
                            }
                        }
                        scheduler = mMediaManager?.scheduler
                    }
                } else {
                    showMessage("Media Download Mode not Supported")
                }
            }
        }
        return
    }

    private fun getFileList() {
        mMediaManager?.apply {
            if (currentFileListState == FileListState.SYNCING || currentFileListState == FileListState.DELETING) {
                Log.d(TAG, "Media Manager is busy.")
            } else {
                this.refreshFileListOfStorageLocation(mMediaType, object : CompletionCallback<DJIError?> {
                    override fun onResult(djiError: DJIError?) {
                        if (null == djiError) {
                            LoggerKit.d("load data suc")
                            hideProgress()
                            //Reset data
                            if (currentFileListState != FileListState.INCOMPLETE) {
                                mediaFileList.clear()
                                lastClickViewIndex = -1
                                lastClickView = null
                            }
                            if (mMediaType == SettingsDefinitions.StorageLocation.SDCARD) {
                                mediaFileList = mMediaManager?.sdCardFileListSnapshot as MutableList<MediaFile>
                            } else if (mMediaType == SettingsDefinitions.StorageLocation.INTERNAL_STORAGE) {
                                mediaFileList =
                                    mMediaManager?.internalStorageFileListSnapshot as MutableList<MediaFile>
                            }
                            Collections.sort(mediaFileList, object : Comparator<MediaFile> {
                                override fun compare(lhs: MediaFile, rhs: MediaFile): Int {
                                    if (lhs.timeCreated < rhs.timeCreated) {
                                        return 1
                                    } else if (lhs.timeCreated > rhs.timeCreated) {
                                        return -1
                                    }
                                    return 0
                                }
                            })
                            scheduler?.resume { error ->
                                if (error == null) {
                                    getThumbnails()
                                }
                            }
                        } else {
                            hideProgress()
                            LoggerKit.e("Get Media File List Failed:" + djiError.description)
                            showMessage("Get Media File List Failed:" + djiError.description)
                        }
                    }
                })
            }
        }
    }


    private fun getThumbnailByIndex(index: Int) {
        val task = FetchMediaTask(mediaFileList[index], FetchMediaTaskContent.THUMBNAIL, taskCallback)
        scheduler?.moveTaskToEnd(task)
    }

    private fun getThumbnails() {
        if (mediaFileList.size <= 0) {
            showMessage("No File info for downloading thumbnails")
            return
        }
        for (i in mediaFileList.indices) {
            getThumbnailByIndex(i)
        }
    }

    private val taskCallback = FetchMediaTask.Callback { file, option, error ->
        if (null == error) {
            if (option == FetchMediaTaskContent.PREVIEW) {
                runOnUiThread {
                    mListAdapter.setNewInstance(mediaFileList)
                    mListAdapter.notifyDataSetChanged()
                }
            }
            if (option == FetchMediaTaskContent.THUMBNAIL) {
                runOnUiThread {
                    mListAdapter.setNewInstance(mediaFileList)
                    mListAdapter.notifyDataSetChanged()
                }
            }
        } else {
            Log.d(TAG, "Fetch Media Task Failed" + error.description)
        }
    }

    private fun addMediaTask(mediaFile: MediaFile) {
        val task = FetchMediaTask(mediaFile, FetchMediaTaskContent.PREVIEW) { mediaFile, fetchMediaTaskContent, error ->
            if (null == error) {
                if (mediaFile.preview != null) {
                    runOnUiThread {
                        viewVisibleExt(R.id.iv_preview, true)
                        viewExt<ImageView>(R.id.iv_preview) {
                            Glide.with(this@MediaManageActivity).load(mediaFile.preview).into(this)
                        }
                    }
                } else {
                    showMessage("null bitmap!")
                }
            } else {
                showMessage("fetch preview image failed: " + error.description)
            }
        }
        mMediaManager?.scheduler?.resume { error ->
            if (error == null) {
                scheduler?.moveTaskToNext(task)
            } else {
                showMessage("resume scheduler failed: " + error.description)
            }
        }
    }

    //根据索引下载对应文件
    private fun downloadFileByIndex(index: Int) {
        if (mediaFileList[index].mediaType == MediaFile.MediaType.PANORAMA
            || mediaFileList[index].mediaType == MediaFile.MediaType.SHALLOW_FOCUS
        ) {
            return
        }
        mediaFileList[index].fetchFileData(destDir, null, object : DownloadListener<String> {
            override fun onFailure(error: DJIError) {
                mDownloadDialog.dismiss()
                showMessage("Download File Failed" + error.description)
                currentProgress = -1
            }

            override fun onProgress(total: Long, current: Long) {}
            override fun onRateUpdate(total: Long, current: Long, persize: Long) {
                val tmpProgress = (1.0 * current / total * 100).toInt()
                if (tmpProgress != currentProgress) {
                    runOnUiThread {
                        (mDownloadDialog as? DownloadDialog)?.updateProgress(tmpProgress)
                        currentProgress = tmpProgress
                    }
                }
            }

            override fun onStart() {
                currentProgress = -1
                mDownloadDialog.show(supportFragmentManager, "download")
            }

            override fun onSuccess(filePath: String) {
                mDownloadDialog.dismiss()
                showMessage("Download File Success:$filePath")
                currentProgress = -1
            }

            override fun onRealtimeDataUpdate(p0: ByteArray?, p1: Long, p2: Boolean) {
            }
        })
    }

    //根据索引删除指定文件
    private fun deleteFileByIndex(index: Int) {
        val fileToDelete = ArrayList<MediaFile>()
        if (mediaFileList.size > index) {
            fileToDelete.add(mediaFileList[index])
            mMediaManager?.deleteFiles(
                fileToDelete,
                object : CompletionCallbackWithTwoParam<List<MediaFile?>?, DJICameraError?> {
                    override fun onSuccess(x: List<MediaFile?>?, y: DJICameraError?) {
                        Log.d(TAG, "Delete file success")
                        runOnUiThread {
                            val file = mListAdapter.data.removeAt(index)
                            //Reset select view
                            lastClickViewIndex = -1
                            lastClickView = null

                            //Update recyclerView
                            mListAdapter.notifyItemRemoved(index)
                        }
                    }

                    override fun onFailure(error: DJIError) {
                        showMessage("Delete file failed")
                    }
                })
        }
    }

    //播放回放
    private fun playVideo() {
        val selectedMediaFile = mediaFileList[lastClickViewIndex]
        if (selectedMediaFile.mediaType == MediaFile.MediaType.MOV || selectedMediaFile.mediaType == MediaFile.MediaType.MP4) {
            viewInVisibleExt(R.id.iv_preview, false)
            mMediaManager?.playVideoMediaFile(selectedMediaFile) { error ->
                if (null != error) {
                    showMessage("Play Video Failed" + error.description)
                } else {
                    Log.d(TAG, "Play Video Success")
                }
            }
        } else {
            showMessage("please choose mp4/mov file to play")
        }
    }

    private fun moveToPosition() {
        val li: LayoutInflater = LayoutInflater.from(this)
        val promptsView: View = li.inflate(R.layout.prompt_input_position, null)
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(promptsView)
        val userInput: EditText = promptsView.findViewById<View>(R.id.editTextDialogUserInput) as EditText
        alertDialogBuilder.setCancelable(false).setPositiveButton(
            "OK"
        ) { dialog, id ->
            val ms: String = userInput.getText().toString()
            mMediaManager?.moveToPosition(ms.toFloat()) { error ->
                if (null != error) {
                    showMessage("Move to video position failed" + error.description)
                } else {
                    Log.d(MediaManageActivity.TAG, "Move to video position successfully.")
                }
            }
        }.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private val updatedVideoPlaybackStateListener =
        MediaManager.VideoPlaybackStateListener { videoPlaybackState -> updateStatusTextView(videoPlaybackState) }

    private fun updateStatusTextView(videoPlaybackState: MediaManager.VideoPlaybackState?) {
        val pushInfo = StringBuffer()
        addLineToSB(pushInfo, "Video Playback State", null)
        if (videoPlaybackState != null) {
            if (videoPlaybackState.playingMediaFile != null) {
                addLineToSB(pushInfo, "media index", videoPlaybackState.playingMediaFile.index)
                addLineToSB(pushInfo, "media size", videoPlaybackState.playingMediaFile.fileSize)
                addLineToSB(
                    pushInfo,
                    "media duration",
                    videoPlaybackState.playingMediaFile.durationInSeconds
                )
                addLineToSB(pushInfo, "media created date", videoPlaybackState.playingMediaFile.dateCreated)
                addLineToSB(
                    pushInfo,
                    "media orientation",
                    videoPlaybackState.playingMediaFile.videoOrientation
                )
            } else {
                addLineToSB(pushInfo, "media index", "None")
            }
            addLineToSB(pushInfo, "media current position", videoPlaybackState.playingPosition)
            addLineToSB(pushInfo, "media current status", videoPlaybackState.playbackStatus)
            addLineToSB(pushInfo, "media cached percentage", videoPlaybackState.cachedPercentage)
            addLineToSB(pushInfo, "media cached position", videoPlaybackState.cachedPosition)
            pushInfo.append("\n")
            setResultToText(pushInfo.toString())
        }
    }

    private fun addLineToSB(sb: StringBuffer?, name: String?, value: Any?) {
        if (sb == null) return
        sb.append(if (name == null || "" == name) "" else "$name: ")
            .append(if (value == null) "" else value.toString() + "").append("\n")
    }

    private fun setResultToText(string: String) {
        runOnUiThread {
            findViewWithId<TextView>(R.id.pointing_push_tv)?.text = string
        }
    }

    override fun onDestroy() {
        lastClickView = null
        mMediaManager?.apply {
            stop(null)
            removeFileListStateCallback(updateFileListStateListener)
            if (isVideoPlaybackSupported) {
                removeMediaUpdatedVideoPlaybackStateListener(updatedVideoPlaybackStateListener)
            }
            exitMediaDownloading()
            scheduler?.removeAllTasks()
        }
        ApplicationKit.getCameraInstance()?.setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO) {}
        mediaFileList.clear()
        super.onDestroy()
    }
}