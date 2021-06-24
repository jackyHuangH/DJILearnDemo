package com.zenchn.common.utils

import com.zenchn.common.ext.safelyRun
import org.jetbrains.annotations.Nullable
import java.io.*
import java.nio.charset.Charset

/**
 * 作   者：wangr on 2019/11/11 13:20
 * 描   述：
 * 修订记录：
 */

object FileUtils {

    private const val FILE_EXTENSION_SEPARATOR = "."

    /**
     * 打开文件（或文件夹）
     *
     *  @param path
     */
    fun open(@Nullable path: String?): File? =
        path.takeUnless { it.isNullOrEmpty() }?.let { File(it) }

    /**
     * 判断文件（非文件夹）是否存在
     *
     * @param path
     * @return
     */
    fun isFileExist(@Nullable path: String?): Boolean = open(path)?.isFileExist() ?: false

    /**
     * 判断文件夹（非文件）是否存在
     *
     * @param path
     * @return
     */
    fun isFolderExist(@Nullable path: String?): Boolean = open(path)?.isFolderExist() ?: false

    /**
     * 创建文件夹
     *
     * @param path
     * @return
     */
    fun mkdirs(path: String?): Boolean = open(path)?.mkdirs() ?: false

    /**
     * 删除文件
     *
     * @param path
     * @return
     */
    fun rm(path: String?): Boolean = open(path)?.rm() ?: false

    /**
     * 删除空文件夹
     *
     * @param path
     * @return
     */
    fun rmdir(path: String?): Boolean = open(path)?.rmdir() ?: false

    /**
     * 强制删除文件（文件夹）
     *
     * @param path
     * @return
     */
    fun sudorm(path: String?): Boolean = open(path)?.sudorm() ?: false

    /**
     * 获取文件夹名称
     *
     * getFolderName(null)               =   null
     * getFolderName("")                 =   ""
     * getFolderName("   ")              =   ""
     * getFolderName("common_footer_loading.mp3")            =   ""
     * getFolderName("common_footer_loading.b.rmvb")         =   ""
     * getFolderName("abc")              =   ""
     * getFolderName("c:\\")              =   "c:"
     * getFolderName("c:\\common_footer_loading")             =   "c:"
     * getFolderName("c:\\common_footer_loading.b")           =   "c:"
     * getFolderName("c:common_footer_loading.txt\\common_footer_loading")        =   "c:common_footer_loading.txt"
     * getFolderName("c:common_footer_loading\\b\\c\\d.txt")    =   "c:common_footer_loading\\b\\c"
     * getFolderName("/home/admin")      =   "/home"
     * getFolderName("/home/admin/common_footer_loading.txt/b.mp3")  =   "/home/admin/common_footer_loading.txt"
     *
     * @param path
     * @return
     */
    fun getFolderName(path: String?): String? = path?.takeIf { it.isNotEmpty() }?.apply {
        lastIndexOf(File.separator).takeIf { it > 0 }?.let {
            substring(0, it)
        }
    }

    /**
     * 获取文件名（带文件后缀）
     *
     * getFileName(null)               =   null
     * getFileName("")                 =   ""
     * getFileName("   ")              =   "   "
     * getFileName("common_footer_loading.mp3")            =   "common_footer_loading.mp3"
     * getFileName("common_footer_loading.b.rmvb")         =   "common_footer_loading.b.rmvb"
     * getFileName("abc")              =   "abc"
     * getFileName("c:\\")              =   ""
     * getFileName("c:\\common_footer_loading")             =   "common_footer_loading"
     * getFileName("c:\\common_footer_loading.b")           =   "common_footer_loading.b"
     * getFileName("c:common_footer_loading.txt\\common_footer_loading")        =   "common_footer_loading"
     * getFileName("/home/admin")      =   "admin"
     * getFileName("/home/admin/common_footer_loading.txt/b.mp3")  =   "b.mp3"
     *
     * @param path
     * @return file name from path, include suffix
     */
    fun getFileName(path: String?): String? = path?.takeIf { it.isNotEmpty() }?.apply {
        lastIndexOf(File.separator).takeIf { it > 0 }?.let {
            substring(it + 1)
        }
    }

    /**
     * 获取文件的后缀名
     *
     * getFileExtension(null)               =   ""
     * getFileExtension("")                 =   ""
     * getFileExtension("   ")              =   "   "
     * getFileExtension("common_footer_loading.mp3")            =   "mp3"
     * getFileExtension("common_footer_loading.b.rmvb")         =   "rmvb"
     * getFileExtension("abc")              =   ""
     * getFileExtension("c:\\")              =   ""
     * getFileExtension("c:\\common_footer_loading")             =   ""
     * getFileExtension("c:\\common_footer_loading.b")           =   "b"
     * getFileExtension("c:common_footer_loading.txt\\common_footer_loading")        =   ""
     * getFileExtension("/home/admin")      =   ""
     * getFileExtension("/home/admin/common_footer_loading.txt/b")  =   ""
     * getFileExtension("/home/admin/common_footer_loading.txt/b.mp3")  =   "mp3"
     *
     * @param path
     * @return
     */
    fun getFileExtension(path: String?): String? = path?.takeIf { it.isNotEmpty() }?.apply {
        lastIndexOf(FILE_EXTENSION_SEPARATOR).takeIf { it > 0 }
            ?.takeIf { lastIndexOf(File.separator) < it }
            ?.let { substring(it + 1) }
    }
}

/**
 * 删除文件
 */
fun File.rm(): Boolean = safelyRun { takeIf { it.isFileExist() }?.delete() } ?: false

/**
 * 删除空文件夹
 */
fun File.rmdir(): Boolean =
    safelyRun {
        takeIf { it.isFolderExist() && it.list()?.isNotEmpty() ?: false }?.delete()
    } ?: false

/**
 * 强制删除文件（文件夹）
 */
fun File.sudorm(): Boolean = safelyRun {
    when {
        isDirectory -> listFiles()?.forEach { file -> file.delete() }
        isFile -> delete()
    }
    delete()
} ?: false

/**
 * 判断文件（非文件夹）是否存在
 *
 * @return
 */
fun File.isFileExist(): Boolean = safelyRun { exists() && isFile } ?: false

/**
 * 判断文件夹（非文件）是否存在
 *
 * @return
 */
fun File.isFolderExist(): Boolean = safelyRun { exists() && isDirectory } ?: false

/**
 * 获取文件（或文件夹）大小
 *
 * @return
 */
fun File.getSize(): Long {
    var size: Long = -1
    if (exists()) {
        if (isDirectory) {
            listFiles()?.forEach { childFile ->
                size += childFile.getSize()
            }
        } else if (isFile) {
            size += length()
        }
    }
    return size
}

/**
 * 将输入流写入到文件（可选是否追加）
 *
 * @param inputStream
 * @param append
 * @return
 */
fun File.write(inputStream: InputStream, append: Boolean = true): Boolean = safelyRun {
    if (!isFileExist()) createNewFile()
    FileOutputStream(this, append).apply {
        val data = ByteArray(1024)
        var length: Int
        do {
            length = inputStream.read(data)
            if (length == -1) break
            write(data, 0, length)
            flush()
        } while (true)
        close()
        inputStream.close()
    }
    true
} ?: false


/**
 * 将字符串写入到文件（可选是否追加）
 *
 * @param content
 * @param append
 * @return
 */
fun File.write(content: String, append: Boolean = true): Boolean = safelyRun {
    if (!isFileExist()) createNewFile()
    FileWriter(this, append).apply {
        write(content)
        flush()
        close()
    }
    true
} ?: false

/**
 * 将集合写入到文件（可选是否追加）
 *
 * @param contentList
 * @param append
 * @return
 */
fun File.write(contentList: List<String>, append: Boolean = true): Boolean = safelyRun {
    if (!isFileExist()) createNewFile()
    FileWriter(this, append).apply {
        for ((i, line) in contentList.withIndex()) {
            if (i > 0) write("\r\n")
            write(line)
            flush()
        }
        close()
    }
    true
} ?: false

/**
 * 读取文件
 *
 * @param charset
 * @return if file not exist, return null, else return content of file
 */
fun File.read(charset: Charset = Charset.defaultCharset()): StringBuilder? = safelyRun {
    StringBuilder().apply {
        BufferedReader(InputStreamReader(FileInputStream(this@read), charset)).apply {
            var line: String?
            var pos = 0
            do {
                line = readLine()
                if (line == null) break
                else if (pos > 0) append("\r\n")
                append(line)
                pos++
            } while (true)
            close()
        }
    }
}

/**
 * 复制文件
 *
 * @param destPath
 * @return
 * @throws RuntimeException if an error occurs while operator FileOutputStream
 */
fun File.copy(destPath: String): Boolean = copy(File(destPath))

fun File.copy(destFile: File): Boolean = safelyRun {
    destFile.write(FileInputStream(this))
} ?: false

/**
 * 移动文件
 *
 * @param destPath
 */
fun File.move(destPath: String) = move(File(destPath))

fun File.move(destFile: File) {
    if (!renameTo(destFile)) {
        copy(destFile)
        rm()
    }
}

