package com.zenchn.common.ext

import android.text.Html
import android.text.Spanned
import com.zenchn.common.SIMPLE_DATE_FORMAT_TEMPLATE
import com.zenchn.common.utils.CodecUtils
import com.zenchn.common.utils.sdf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import java.io.Closeable
import java.util.*
import kotlin.collections.ArrayList

fun Boolean?.isTrue(defValue: Boolean = false) = this ?: defValue

fun Boolean?.isFalse(defValue: Boolean = false) = !isTrue(defValue)

fun CharSequence?.isNumeric(): Boolean = this?.let {
    forEach { if (!Character.isDigit(it)) return false }
    true
} ?: false

fun CharSequence?.isNotNullAndNotEmpty(): Boolean = !isNullOrEmpty()

fun CharSequence?.encryptPwd(upperCase: Boolean = false): String? = this?.let {
    val encrypt = CodecUtils.encrypt(it.toString())
    if (upperCase) encrypt?.toUpperCase(Locale.getDefault())
    else encrypt?.toLowerCase(Locale.getDefault())
}

fun String?.orNotNullNotEmpty(defValue: String): String {
    return if (isNullOrEmpty()) defValue else this!!
}

@Suppress("DEPRECATION")
fun String.formatHtml(vararg args: Any?): Spanned = java.lang.String.format(this, *args).html()

@Suppress("DEPRECATION")
fun CharSequence.html(): Spanned = Html.fromHtml(toString())

fun String?.parseToDate(pattern: String = SIMPLE_DATE_FORMAT_TEMPLATE, def: Date = Date()): Date {
    return this?.let {
        sdf.safelyRun {
            it.applyPattern(pattern)
            it.parse(this)
        }
    } ?: def
}

fun Long?.dateFormat(pattern: String = SIMPLE_DATE_FORMAT_TEMPLATE, def: String? = ""): String? {
    return this?.also { if (it.toString().length == 13) it / 1000 }
        ?.let { timeMillis ->
            sdf.safelyRun {
                it.applyPattern(pattern)
                it.format(Date(timeMillis))
            }
        } ?: def
}

inline fun <reified T : Enum<T>> enumValueOf(predicate: (T) -> Boolean): T? {
    return enumValues<T>().firstOrNull(predicate)
}

inline fun <reified T : Enum<T>> enumValueOf(ordinal: Int?): T? {
    return if (ordinal != null && ordinal >= 0) {
        enumValueOf<T> {
            it.ordinal == ordinal
        }
    } else null
}

@Throws(IllegalStateException::class)
inline fun <T : Any> T?.checkNotNull(lazyMessage: () -> Any = { "Error , value is empty!" }): T {
    return checkNotNull(this, lazyMessage)
}

@Throws(IllegalStateException::class)
inline fun <T : Any> T?.checkNotNull(
    predicate: (T?) -> Boolean,
    lazyMessage: () -> Any = { "Error , value is not require!" }
): T {
    require(predicate.invoke(this), lazyMessage)
    checkNotNull(this)
    return this
}

inline fun <T> List<T>.reverseForEach(action: (T) -> Unit): Unit {
    for (i in indices.reversed()) {
        action.invoke(get(i))
    }
}

inline fun <T> MutableCollection<T>.removeIf(
    predicate: (Int, T) -> Boolean,
    noinline watcher: ((Int, T) -> Unit)? = null
) {
    iterator().apply {
        var index = 0
        while (hasNext()) {
            val next = next()
            if (predicate.invoke(index, next)) {
                remove()
                watcher?.invoke(index, next)
            }
            index++
        }
    }
}

inline fun <T, P> arrayListOf(vararg elements: T, convent: (T) -> P): ArrayList<P> =
    ArrayList<P>().apply {
        elements.forEach { element ->
            add(convent.invoke(element))
        }
    }

/**
 * 集合复制
 */
fun <E> List<E>.copy(
    srcPos: Int = 0,
    length: Int = size,
    predicate: ((E) -> Boolean)? = null
): List<E> {
    require(srcPos < size) { "Array copy error : srcPos out of src bounds ." }
    require(srcPos + length < size + 1) { "Array copy error : copy length out of src bounds ." }
    return ArrayList<E>(length).apply {
        this@copy.subList(srcPos, srcPos + length).forEach {
            if (predicate?.invoke(it) != false) {
                add(it)
            }
        }
    }
}

/**
 * 在数组的末尾添加一个或多个元素 返回数组新长度
 */
fun <E> MutableList<E>.push(e: E): Int {
    add(e)
    return size
}

/**
 *  在数组的第一项前面添加一个或多个元素，返回数组的长度
 */
fun <E> MutableList<E>.unshift(e: E): Int {
    add(0, e)
    return size
}

/**
 * 移除数组的第一项，返回移除项
 */
fun <E> MutableList<E>.shift(): E? {
    return if (size > 0) removeAt(0) else null
}

/**
 * 移除数组的最后一项，返回移除的项
 */
fun <E> MutableList<E>.pop(): E? {
    return if (size > 0) removeAt(size - 1) else null
}


//fun <T> Collection<T>.getTClass(): Class<T>? {
//    return (javaClass.genericSuperclass as? ParameterizedType)?.actualTypeArguments?.get(0) as? Class<T>
//}

inline fun <R> tryCatchRun(
    noinline catch: ((Exception) -> Unit)? = null,
    noinline finally: (() -> Unit)? = null,
    crossinline runnable: () -> R?
): R? = try {
    runnable.invoke()
} catch (e: Exception) {
    catch?.invoke(e)
    null
} finally {
    finally?.invoke()
}

inline fun <S, R> S.safelyRun(
    noinline catch: ((Exception) -> Unit)? = null,
    noinline finally: (() -> Unit)? = null,
    crossinline runnable: (S) -> R?
): R? = try {
    runnable.invoke(this)
} catch (e: Exception) {
    catch?.invoke(e)
    null
} finally {
    finally?.invoke()
    if (this is Closeable) safelyClose()
}

fun Closeable.safelyClose() {
    try {
        this.close()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

suspend fun <T> coroutineTryCatch(
    tryBlock: suspend CoroutineScope.() -> T,
    catchBlock: (suspend CoroutineScope.(Throwable) -> Unit)? = null,
    finallyBlock: (suspend CoroutineScope.() -> Unit)? = null,
    handleCancellationExceptionManually: Boolean = true
): T? {
    return coroutineScope {
        try {
            tryBlock()
        } catch (e: Exception) {
            if (e !is CancellationException || handleCancellationExceptionManually) {
                catchBlock?.invoke(this, e)
                null
            } else {
                throw e
            }
        } finally {
            finallyBlock?.invoke(this)
        }
    }
}


