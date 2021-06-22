@file:Suppress("unused")

package com.zenchn.widget

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.ColorRes
import androidx.annotation.IntRange
import androidx.annotation.StringRes
import com.bigkoo.pickerview.listener.OnOptionsSelectListener
import com.bigkoo.pickerview.listener.OnTimeSelectListener
import com.bigkoo.pickerview.view.TimePickerView
import com.zenchn.common.utils.ResolveUtils
import com.zenchn.common.utils.toCalendar
import java.util.*

interface IPickerView

fun IPickerView.showOptionPicker(
        context: Context,
        @ArrayRes optionsLabelItems: Int,
        @ArrayRes optionsValueItems: Int? = null,
        value: String? = null,
        @StringRes titleRes: Int = R.string.common_picker_title,
        title: String? = null,
        @ColorRes titleColorRes: Int = R.color.color_PickerViewTitle,
        @StringRes cancelRes: Int = R.string.common_picker_cancel,
        cancel: String? = null,
        @ColorRes cancelColorRes: Int = R.color.color_PickerViewCancel,
        @StringRes submitRes: Int = R.string.common_picker_submit,
        submit: String? = null,
        @ColorRes submitColorRes: Int = R.color.color_PickerViewSubmit,
        cancelCallback: (() -> Unit)? = null,
        submitCallback: (Int, String?, String?) -> Unit
) {

    showOptionPicker(
            context = context,
            options1Items = context.resources.getStringArray(optionsLabelItems).toMutableList(),
            position1 = value?.let { keyword ->
                optionsValueItems?.let {
                    context.resources.getStringArray(it).indexOfFirst { keyword == value }
                }
            },
            titleRes = titleRes,
            title = title,
            titleColorRes = titleColorRes,
            cancelRes = cancelRes,
            cancel = cancel,
            cancelColorRes = cancelColorRes,
            submitRes = submitRes,
            submit = submit,
            submitColorRes = submitColorRes,
            cancelCallback = cancelCallback,
            submitCallback = { i: Int, s: String?, _: Int, _: String?, _: Int, _: String? ->
                optionsValueItems?.let { context.resources.getStringArray(it).getOrNull(i) }?.let {
                    submitCallback.invoke(i, s, it)
                }
            }
    )
}

fun <T> IPickerView.showOptionPicker(
        context: Context,
        options1Items: List<T>,
        position1: Int? = null,
        options2Items: List<List<T>>? = null,
        position2: Int? = null,
        options3Items: List<List<List<T>>>? = null,
        position3: Int? = null,
        @StringRes titleRes: Int = R.string.common_picker_title,
        title: String? = null,
        @ColorRes titleColorRes: Int = R.color.color_PickerViewTitle,
        @StringRes cancelRes: Int = R.string.common_picker_cancel,
        cancel: String? = null,
        @ColorRes cancelColorRes: Int = R.color.color_PickerViewCancel,
        @StringRes submitRes: Int = R.string.common_picker_submit,
        submit: String? = null,
        @ColorRes submitColorRes: Int = R.color.color_PickerViewSubmit,
        cancelCallback: (() -> Unit)? = null,
        submitCallback: (Int, T?, Int, T?, Int, T?) -> Unit
) {
    OptionsPickerBuilder(
            context,
            options1Items,
            position1,
            options2Items,
            position2,
            options3Items,
            position3,
            cancelCallback,
            submitCallback
    ).apply {
        setTitleText(title ?: ResolveUtils.resolveString(context, titleRes)?.toString())
        setTitleColor(ResolveUtils.resolveColor(context, titleColorRes))
        setSubmitText(submit ?: ResolveUtils.resolveString(context, submitRes)?.toString())
        setSubmitColor(ResolveUtils.resolveColor(context, submitColorRes))
        setCancelText(cancel ?: ResolveUtils.resolveString(context, cancelRes)?.toString())
        setCancelColor(ResolveUtils.resolveColor(context, cancelColorRes))
        show()
    }
}

fun IPickerView.showTimePicker(
        context: Context,
        @IntRange(from = 0x1, to = 0x3f) dateType: Int = ymdhms,
        outSideCancelable: Boolean = true,
        date: Date? = null,
        startDate: Date? = null,
        endDate: Date? = null,
        @StringRes titleRes: Int = R.string.common_picker_title,
        title: String? = null,
        @ColorRes titleColorRes: Int = R.color.color_PickerViewTitle,
        @StringRes cancelRes: Int = R.string.common_picker_cancel,
        cancel: String? = null,
        @ColorRes cancelColorRes: Int = R.color.color_PickerViewCancel,
        @StringRes submitRes: Int = R.string.common_picker_submit,
        submit: String? = null,
        @ColorRes submitColorRes: Int = R.color.color_PickerViewSubmit,
        cancelCallback: (() -> Unit)? = null,
        submitCallback: (Date) -> Unit
) {
    TimePickerBuilder(context, cancelCallback, submitCallback).apply {
        setType(format(dateType))
        setOutSideCancelable(outSideCancelable)
        setTitleText(title ?: ResolveUtils.resolveString(context, titleRes)?.toString())
        setTitleColor(ResolveUtils.resolveColor(context, titleColorRes))
        setSubmitText(submit ?: ResolveUtils.resolveString(context, submitRes)?.toString())
        setSubmitColor(ResolveUtils.resolveColor(context, submitColorRes))
        setCancelText(cancel ?: ResolveUtils.resolveString(context, cancelRes)?.toString())
        setCancelColor(ResolveUtils.resolveColor(context, cancelColorRes))
        setRangDate(startDate?.toCalendar(), endDate?.toCalendar())
    }.show {
        setDate(date?.toCalendar())
    }
}

private val format = { type: Int ->
    booleanArrayOf(
            type and YEAR == YEAR,
            type and MONTH == MONTH,
            type and DAY == DAY,
            type and HOUR == HOUR,
            type and MINUTE == MINUTE,
            type and SECOND == SECOND
    )
}

const val ymdhms = 0x3f
const val ymdhm = 0x3e
const val ymd = 0x38
const val hm = 0x6
const val hms = 0x7

const val YEAR = 1 shl (5)
const val MONTH = 1 shl (4)
const val DAY = 1 shl (3)
const val HOUR = 1 shl (2)
const val MINUTE = 1 shl (1)
const val SECOND = 1

internal class OptionsPickerBuilder<T>(
        context: Context,
        private val options1Items: List<T>,
        position1: Int? = null,
        private val options2Items: List<List<T>>? = null,
        position2: Int? = null,
        private val options3Items: List<List<List<T>>>? = null,
        position3: Int? = null,
        cancelCallback: (() -> Unit)? = null,
        submitCallback: (Int, T?, Int, T?, Int, T?) -> Unit
) : com.bigkoo.pickerview.builder.OptionsPickerBuilder(
        context,
        OnOptionsSelectListener { option1, option2, option3, _ ->
            submitCallback.invoke(
                    option1,
                    options1Items.getOrNull(option1),
                    option2,
                    options2Items?.getOrNull(option1)?.getOrNull(option2),
                    option3,
                    options3Items?.getOrNull(option1)?.getOrNull(option2)?.getOrNull(option3)
            )
        }) {

    init {
        cancelCallback?.let { callback -> addOnCancelClickListener { callback.invoke() } }
        setSelectOptions(position1 ?: 0, position2 ?: 0, position3 ?: 0)
    }

    fun show() {
        build<T>().apply {
            setPicker(options1Items, options2Items, options3Items)
            show()
        }
    }
}

internal class TimePickerBuilder(
        context: Context,
        cancelCallback: (() -> Unit)? = null,
        submitCallback: (Date) -> Unit
) : com.bigkoo.pickerview.builder.TimePickerBuilder(context, OnTimeSelectListener { date, _ ->
    submitCallback(date)
}) {

    init {
        cancelCallback?.let { callback -> addOnCancelClickListener { callback.invoke() } }
    }

    fun show(option: TimePickerView.() -> Unit) {
        build().apply(option).show()
    }

}

