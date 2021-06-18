package com.zenchn.djilearndemo.ui.adapter

import android.text.format.Formatter
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.zenchn.djilearndemo.R
import dji.sdk.media.MediaFile

/**
 * @author:Hzj
 * @date  :2021/6/4
 * desc  ：
 * record：
 */
class FileListAdapter() :
    BaseQuickAdapter<MediaFile, BaseViewHolder>(layoutResId = R.layout.rv_item_file_list) {
    private var clickViewIndex = -1

    fun updateSelected(position: Int) {
        clickViewIndex = position
        notifyDataSetChanged()
    }

    override fun convert(holder: BaseViewHolder, item: MediaFile) {
        val position = holder.absoluteAdapterPosition
        if (item.getMediaType() != MediaFile.MediaType.MOV && item.getMediaType() != MediaFile.MediaType.MP4) {
            holder.setGone(R.id.filetime, true)
            holder.setGone(R.id.iv_video, true)
        } else {
            holder.setGone(R.id.iv_video, false)
            holder.setGone(R.id.filetime, false)
            holder.setText(R.id.filetime, "${item.durationInSeconds}s")
        }
        holder.setText(R.id.filename, item.getFileName())
        holder.setText(R.id.filetype, item.getMediaType().name)
        val formatFileSize = Formatter.formatFileSize(context, item.getFileSize())
        holder.setText(R.id.fileSize, formatFileSize)
        val ivThumbnail = holder.getView<ImageView>(R.id.iv_thumbnail)
        Glide.with(context).load(item.thumbnail).into(ivThumbnail)

        val rlRoot = holder.getView<View>(R.id.rl_root)
        rlRoot.isSelected = clickViewIndex == position
    }
}