package momoi.mod.qqpro.util

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import moye.wear.hook.LinkText
import moye.wear.hook.openLinkWithConfirm

fun TextView.linkify() {
    val spannable = SpannableStringBuilder(text)
    val existingSpans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
    existingSpans.forEach { spannable.removeSpan(it) }
    // 链接区间的识别交给 LinkText 统一处理：是否识别无前缀链接由设置项控制
    val links = LinkText.ranges(spannable)
    links.reversed().forEach { range ->
        val start = range.first
        val end = range.last + 1
        val url = spannable.substring(start, end)

        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // 走带确认的统一入口，裸链接会在打开时补全协议头
                    widget.openLinkWithConfirm(url)
                }
            },
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    text = spannable
    movementMethod = LinkMovementMethod.getInstance()
}
