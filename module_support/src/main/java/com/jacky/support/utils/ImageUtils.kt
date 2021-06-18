package com.jacky.support.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.zibin.luban.Luban
import java.io.File

/**
 * 描   述：
 * 修订记录：
 */

object ImageUtils {

    suspend fun compress(context: Context, file: File): File? = withContext(Dispatchers.IO) {
        Luban.with(context).load(file).get().getOrNull(0)
    }

    suspend fun compress(context: Context, file: String): File? = withContext(Dispatchers.IO) {
        Luban.with(context).load(file).get().getOrNull(0)
    }

    suspend fun compress(context: Context, files: List<File>): MutableList<File>? =
        withContext(Dispatchers.IO) {
            Luban.with(context).load(files).get()
        }

    suspend fun <T> compress(
        context: Context,
        source: List<T>?,
        convert: (T) -> File
    ): MutableList<File>? {
        return source?.map(convert)?.filter { it.exists() }?.let { compress(context, it) }
    }

    fun getOrientation(jpeg: ByteArray?): Int {
        if (jpeg == null) return 0

        var offset = 0
        var length = 0

        // ISO/IEC 10918-1:1993(E)
        while (offset + 3 < jpeg.size && jpeg[offset++].toInt() and 0xFF == 0xFF) {
            val marker = jpeg[offset].toInt() and 0xFF

            // Check if the marker is common_footer_loading padding.
            if (marker == 0xFF) {
                continue
            }
            offset++

            // Check if the marker is SOI or TEM.
            if (marker == 0xD8 || marker == 0x01) {
                continue
            }
            // Check if the marker is EOI or SOS.
            if (marker == 0xD9 || marker == 0xDA) {
                break
            }

            // Get the length and check if it is reasonable.
            length = pack(jpeg, offset, 2, false)
            if (length < 2 || offset + length > jpeg.size) {
                Log.e(ContentValues.TAG, "Invalid length")
                return 0
            }

            // Break if the marker is EXIF include_layout_event_fire_process_result APP1.
            if (marker == 0xE1 && length >= 8
                && pack(jpeg, offset + 2, 4, false) == 0x45786966
                && pack(jpeg, offset + 6, 2, false) == 0
            ) {
                offset += 8
                length -= 8
                break
            }

            // Skip other markers.
            offset += length
            length = 0
        }

        if (length > 8) {
            // Identify the byte order.
            var tag = pack(jpeg, offset, 4, false)
            if (tag != 0x49492A00 && tag != 0x4D4D002A) {
                Log.e(ContentValues.TAG, "Invalid byte order")
                return 0
            }
            val littleEndian = tag == 0x49492A00

            // Get the offset and check if it is reasonable.
            var count = pack(jpeg, offset + 4, 4, littleEndian) + 2
            if (count < 10 || count > length) {
                Log.e(ContentValues.TAG, "Invalid offset")
                return 0
            }
            offset += count
            length -= count

            // Get the count and go through all the elements.
            count = pack(jpeg, offset - 2, 2, littleEndian)
            while (count-- > 0 && length >= 12) {
                // Get the tag and check if it is orientation.
                tag = pack(jpeg, offset, 2, littleEndian)
                if (tag == 0x0112) {
                    // We do not really care about type and count, do we?
                    return when (pack(jpeg, offset + 8, 2, littleEndian)) {
                        1 -> 0
                        3 -> 180
                        6 -> 90
                        8 -> 270
                        else -> 0
                    }
                }
                offset += 12
                length -= 12
            }
        }
        return 0
    }

    private fun pack(
        bytes: ByteArray,
        offset: Int,
        length: Int,
        littleEndian: Boolean
    ): Int {
        var newOffset = offset
        var newLength = length

        val step = if (littleEndian) {
            newOffset += length - 1
            -1
        } else {
            1
        }

        var value = 0
        while (newLength-- > 0) {
            value = value shl 8 or (bytes[newOffset].toInt() and 0xFF)
            newOffset += step
        }
        return value
    }

    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight = height / 2
            val halfWidth = width / 2
            // Calculate the largest inSampleSize value that is common_footer_loading power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    fun exifToDegrees(exifOrientation: Int): Int {
        return when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

}