package com.jacky.support.gallery

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bm.library.PhotoView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.BitmapImageViewTarget
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.jacky.support.R
import com.jacky.support.base.IActivity
import com.jacky.support.bothSafeRun
import com.jacky.support.setOnAntiShakeClickListener
import com.jacky.support.utils.UriUtils
import com.luck.picture.lib.PictureSelector
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.engine.ImageEngine
import com.luck.picture.lib.entity.LocalMedia
import com.luck.picture.lib.listener.OnImageCompleteCallback
import com.luck.picture.lib.tools.MediaUtils
import com.luck.picture.lib.widget.longimage.ImageViewState
import com.luck.picture.lib.widget.longimage.SubsamplingScaleImageView
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

//文件路径
internal const val FILE_DIR_TEP = "/tmp"
internal const val FILE_DIR_CAMERA = "/camera"
const val REQUEST_CODE_GALLERY = 6666

interface IGallery : IActivity

//大图浏览
fun IGallery.previewPicture(
    @NotNull source: ImageSource,
    @IntRange(from = 0) position: Int = 0
) = previewPicture(arrayListOf(source), position)

fun <T> IGallery.previewPicture(
    @NotNull source: List<T>,
    converter: (T) -> ImageSource,
    @IntRange(from = 0) position: Int = 0
) = when (this) {
    is Activity -> FrameWrapper.preview(
        this,
        source.map(converter),
        position
    )
    is Fragment -> FrameWrapper.preview(
        this,
        source.map(converter),
        position
    )
    else -> throw IllegalArgumentException("IGallery Error :  just support Activity and Fragment .")
}

fun IGallery.previewPicture(
    @NotNull source: List<ImageSource>,
    @IntRange(from = 0) position: Int = 0
) = when (this) {
    is Activity -> FrameWrapper.preview(
        this,
        source,
        position
    )
    is Fragment -> FrameWrapper.preview(
        this,
        source,
        position
    )
    else -> throw IllegalArgumentException("IGallery Error :  just support Activity and Fragment .")
}

fun IGallery.selectPicture(
    @IntRange(from = 1) maxSelectCount: Int = 1,
    vararg source: LocalImageSource,
    @NotNull requestCode: Int = REQUEST_CODE_GALLERY
) = when (this) {
    is Activity -> FrameWrapper.select(
        this,
        maxSelectCount,
        source.toList(),
        requestCode
    )
    is Fragment -> FrameWrapper.select(
        this,
        maxSelectCount,
        source.toList(),
        requestCode
    )
    else -> throw IllegalArgumentException("IGallery Error :  just support Activity and Fragment .")
}

fun IGallery.obtainMultiResultFormGallery(
    data: Intent?,
    result: (List<LocalImageSource>?) -> Unit
) = result.invoke(
    data?.let {
        FrameWrapper.obtainPictureResult(
            it
        )
    }
)

fun IGallery.obtainSingleResultFormGallery(data: Intent?, result: (LocalImageSource?) -> Unit) =
    obtainMultiResultFormGallery(data) {
        result.invoke(it?.getOrNull(0))
    }

object FrameWrapper {

    fun preview(
        @NotNull fragment: Fragment,
        @NotNull source: List<ImageSource>,
        @IntRange(from = 0) position: Int = 0
    ) {
        fragment.activity?.let {
            preview(
                it,
                source,
                position
            )
        }
    }

    fun preview(
        @NotNull activity: Activity,
        @NotNull source: List<ImageSource>,
        @IntRange(from = 0) position: Int = 0
    ) {
        //查看大图
        PhotoBrowserActivity.launch(
            activity,
            ArrayList(source),
            position
        )
    }


    fun select(
        @NotNull fragment: Fragment,
        @IntRange(from = 1) maxSelectCount: Int = 1,
        @Nullable source: List<ImageSource>? = null,
        @NotNull requestCode: Int
    ) {
        bothSafeRun(fragment.context, source) { ctx, list ->
            val medias =
                transformMedia(ctx, list)
            PictureSelector.create(fragment).select(maxSelectCount, requestCode, ctx, medias)
        }
    }

    fun select(
        @NotNull activity: Activity,
        @IntRange(from = 1) maxSelectCount: Int = 1,
        @Nullable source: List<ImageSource>? = null,
        @NotNull requestCode: Int
    ) {
        val medias = source?.let {
            transformMedia(
                activity,
                it
            )
        }
        PictureSelector.create(activity).select(maxSelectCount, requestCode, activity, medias)
    }

    private fun PictureSelector.select(
        @IntRange(from = 0) maxSelectCount: Int = 0,
        @NotNull requestCode: Int,
        @NotNull context: Context,
        @Nullable medias: List<LocalMedia>? = null
    ) {
        openGallery(PictureMimeType.ofImage()).apply {
            loadImageEngine(GlideEngine.INSTANCE)
            forResult(requestCode)//结果回调onActivityResult co
            imageSpanCount(4)// 每行显示个数 int
            maxSelectNum(maxSelectCount)// 最大图片选择数量 int
            isCamera(true)//显示拍照按钮
            imageFormat(PictureMimeType.PNG)// 拍照保存图片格式后缀,默认jpeg
            //cameraFileName(PictureMimeType.PNG)// 拍照保存图片格式后缀,默认jpeg
            previewImage(true)// 是否可预览图片 true or false
            cutOutQuality(90)// 裁剪输出质量 默认100
            minimumCompressSize(100)// 小于100kb的图片不压缩
            isZoomAnim(true)// 图片列表点击 缩放效果 默认true
            selectionMedia(medias)// 是否传入已选图片 List<LocalMedia> ic_list
            enableCrop(false)// 是否裁剪 true or false
            compress(false)// 是否压缩 true or false
            isAndroidQTransform(true)

            //切记放在最后一句
            forResult(requestCode)//结果回调onActivityResult co
        }
    }

    fun obtainPictureResult(data: Intent): List<LocalImageSource>? =
        PictureSelector.obtainMultipleResult(data)?.let {
            transformSource(it)
        }

    private fun LocalMedia.toImageSource() =
        LocalImageSource(getRealImagePath())

    private fun ImageSource.toLocalMedia(context: Context): LocalMedia {
        val source = get()
        val path = when (type()) {
            ImageSource.Type.PATH -> source as? String
            ImageSource.Type.FILE -> (source as? File)?.absolutePath
            ImageSource.Type.URI -> (source as? Uri)?.let { UriUtils.getFilePathByUri(context, it) }
            else -> null
        }
        return LocalMedia(path, 0,false,0,1, PictureMimeType.ofImage())
    }

    private fun transformMedia(context: Context, source: List<ImageSource>): List<LocalMedia> {
        return source.map {
            it.toLocalMedia(context)
        }
    }

    private fun transformSource(source: List<LocalMedia>): List<LocalImageSource> {
        return source.map {
            it.toImageSource()
        }
    }

    private fun LocalMedia.getRealImagePath(): String = with(this) {
        return if (isCompressed) {
            compressPath
        } else {
            //如果裁剪了，以取裁剪路径为准
            if (isCut) cutPath
            else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) androidQToPath
                else path
            }
        }
    }

}

internal class GlideEngine private constructor() : ImageEngine {

    /**
     * 加载图片
     *
     * @param context
     * @param url
     * @param imageView
     */
    override fun loadImage(context: Context, url: String, imageView: ImageView) {
        val drawableCrossFadeFactory =
            DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        Glide.with(context)
            .load(url)
            .transition(DrawableTransitionOptions.withCrossFade(drawableCrossFadeFactory))
            .into(imageView)
    }

    /**
     * 加载图片
     *
     * @param context
     * @param url
     * @param imageView
     */
    override fun loadImage(
        context: Context,
        url: String,
        imageView: ImageView,
        longImageView: SubsamplingScaleImageView,
        callback: OnImageCompleteCallback?
    ) {
        Glide.with(context)
            .asBitmap()
            .load(url)
            .into<ImageViewTarget<Bitmap>>(object : ImageViewTarget<Bitmap>(imageView) {
                override fun onLoadStarted(placeholder: Drawable?) {
                    super.onLoadStarted(placeholder)
                    callback?.onShowLoading()
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    callback?.onHideLoading()
                }

                override fun setResource(resource: Bitmap?) {
                    callback?.onHideLoading()
                    if (resource != null) {
                        val eqLongImage = MediaUtils.isLongImg(resource.width, resource.height)
                        longImageView.visibility = if (eqLongImage) View.VISIBLE else View.GONE
                        imageView.visibility = if (eqLongImage) View.GONE else View.VISIBLE
                        if (eqLongImage) {
                            // 加载长图
                            longImageView.isQuickScaleEnabled = true
                            longImageView.isZoomEnabled = true
                            longImageView.isPanEnabled = true
                            longImageView.setDoubleTapZoomDuration(100)
                            longImageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP)
                            longImageView.setDoubleTapZoomDpi(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
                            longImageView.setImage(
                                com.luck.picture.lib.widget.longimage.ImageSource.bitmap(resource),
                                ImageViewState(0f, PointF(0f, 0f), 0)
                            )
                        } else {
                            // 普通图片
                            imageView.setImageBitmap(resource)
                        }
                    }
                }
            })
    }

    /**
     * 加载网络图片适配长图方案
     * # 注意：此方法只有加载网络图片才会回调
     *
     * @param context
     * @param url
     * @param imageView
     * @param longImageView
     */
    override fun loadImage(
        context: Context,
        url: String,
        imageView: ImageView,
        longImageView: SubsamplingScaleImageView
    ) {
        Glide.with(context)
            .asBitmap()
            .load(url)
            .into<ImageViewTarget<Bitmap>>(object : ImageViewTarget<Bitmap>(imageView) {
                override fun setResource(resource: Bitmap?) {
                    if (resource != null) {
                        val eqLongImage = MediaUtils.isLongImg(resource.width, resource.height)
                        longImageView.visibility = if (eqLongImage) View.VISIBLE else View.GONE
                        imageView.visibility = if (eqLongImage) View.GONE else View.VISIBLE
                        if (eqLongImage) {
                            // 加载长图
                            longImageView.isQuickScaleEnabled = true
                            longImageView.isZoomEnabled = true
                            longImageView.isPanEnabled = true
                            longImageView.setDoubleTapZoomDuration(100)
                            longImageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_CROP)
                            longImageView.setDoubleTapZoomDpi(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
                            longImageView.setImage(
                                com.luck.picture.lib.widget.longimage.ImageSource.bitmap(resource),
                                ImageViewState(0f, PointF(0f, 0f), 0)
                            )
                        } else {
                            // 普通图片
                            imageView.setImageBitmap(resource)
                        }
                    }
                }
            })
    }

    /**
     * 加载相册目录
     *
     * @param context   上下文
     * @param url       图片路径
     * @param imageView 承载图片ImageView
     */
    override fun loadFolderImage(context: Context, url: String, imageView: ImageView) {
        Glide.with(context)
            .asBitmap()
            .load(url)
            .apply(RequestOptions().apply {
                override(180, 180)
                centerCrop()
                sizeMultiplier(0.5f)
                placeholder(R.drawable.picture_image_placeholder)
            })
            .into<BitmapImageViewTarget>(object : BitmapImageViewTarget(imageView) {
                override fun setResource(resource: Bitmap?) {
                    val circularBitmapDrawable =
                        RoundedBitmapDrawableFactory.create(context.resources, resource)
                    circularBitmapDrawable.cornerRadius = 8f
                    imageView.setImageDrawable(circularBitmapDrawable)
                }
            })
    }


    /**
     * 加载gif
     *
     * @param context   上下文
     * @param url       图片路径
     * @param imageView 承载图片ImageView
     */
    override fun loadAsGifImage(
        context: Context, url: String,
        imageView: ImageView
    ) {
        Glide.with(context)
            .asGif()
            .load(url)
            .into(imageView)
    }

    /**
     * 加载图片列表图片
     *
     * @param context   上下文
     * @param url       图片路径
     * @param imageView 承载图片ImageView
     */
    override fun loadGridImage(context: Context, url: String, imageView: ImageView) {
        // * other https://www.jianshu.com/p/28f5bcee409f
        val drawableCrossFadeFactory =
            DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()
        Glide.with(context)
            .load(url)
            .apply(RequestOptions().apply {
                override(200, 200)
                diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                centerCrop()
                placeholder(R.drawable.picture_image_placeholder)
            })
            .transition(DrawableTransitionOptions.withCrossFade(drawableCrossFadeFactory))
            .into(imageView)
    }

    companion object {
        val INSTANCE: GlideEngine by lazy { GlideEngine() }
    }
}

//===========================大图浏览,支持本地、网络图片======================================
/**
 * 作   者：HZJ on 2019/12/22/022 16:59
 * 描   述：
 * 修订记录：
 */
class PhotoBrowserActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_IMAGE_URLS = "EXTRA_IMAGE_URLS"
        private const val EXTRA_CURRENT_POSITION = "EXTRA_CURRENT_POSITION"

        internal fun launch(
            @NonNull from: Activity,
            @NonNull sources: ArrayList<ImageSource>,
            currentPosition: Int
        ) {
            val intent = Intent(from, PhotoBrowserActivity::class.java).apply {
                putParcelableArrayListExtra(EXTRA_IMAGE_URLS, sources)
                putExtra(EXTRA_CURRENT_POSITION, currentPosition)
            }
            from.startActivity(intent)
        }
    }

    private val imageSourceList by lazy {
        intent.getParcelableArrayListExtra<ImageSource>(
            EXTRA_IMAGE_URLS
        )
    }
    private val currentPosition by lazy { intent.getIntExtra(EXTRA_CURRENT_POSITION, 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pic_browse)
        initWidget()
    }

    private fun initWidget() {
        val adapter =
            imageSourceList?.let { PhotoBrowserAdapter(it) { onBackPressed() } }
        val tvIndex = findViewById<TextView>(R.id.tv_index)
        findViewById<ViewPager>(R.id.viewpager).apply {
            addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
                override fun onPageSelected(position: Int) {
                    tvIndex.text =
                        String.format(Locale.CHINA, "%d/%d", position + 1, imageSourceList?.size)
                }
            })
            setAdapter(adapter)
            currentItem = currentPosition
            adapter?.notifyDataSetChanged()
        }
        tvIndex.text =
            String.format(Locale.CHINA, "%d/%d", currentPosition + 1, imageSourceList?.size)
    }
}

private class PhotoBrowserAdapter(
    val imgSourceList: ArrayList<ImageSource>,
    val onPhotoTabListener: (() -> Unit)? = null
) : PagerAdapter() {

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`

    override fun getCount(): Int = imgSourceList.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val context = container.context
        val imageSourceInfo: ImageSource = imgSourceList[position]

        val view =
            LayoutInflater.from(context).inflate(R.layout.viewpage_item_photoview, container, false)
        val photoView = view.findViewById<PhotoView>(R.id.photoview).apply {
            //开启缩放
            enable()
        }

        //加载图片
        val requestOptions = RequestOptions()
            .fitCenter()
            .dontAnimate()
            .placeholder(R.drawable.ic_common_photo_holder)
            .error(R.drawable.ic_common_photo_holder)
            .diskCacheStrategy(DiskCacheStrategy.NONE)//跳过磁盘缓存
        Glide
            .with(context)
            .load(imageSourceInfo.get())
            .apply(requestOptions)
            .into(photoView)

        photoView.setOnAntiShakeClickListener {
            //释放内存
            Glide.get(context).clearMemory()
            onPhotoTabListener?.invoke()
        }

        //防止v被添加前存在与另一个父容器中
        (photoView.parent as? ViewGroup)?.removeView(photoView)

        container.addView(photoView)
        return photoView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val photoView = `object` as? PhotoView
        photoView?.let { container.removeView(it) }
    }
}

class BrowserViewPager : ViewPager {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        //解决photoView 缩小图时异常崩溃
        return try {
            super.onInterceptTouchEvent(ev)
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            false
        }
    }
}