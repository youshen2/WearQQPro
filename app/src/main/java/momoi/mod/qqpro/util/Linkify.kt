package momoi.mod.qqpro.util

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.widget.TextView
import momoi.mod.qqpro.Settings
import moye.wear.hook.LinkText
import java.util.WeakHashMap

private data class LinkifyState(val text: String, val wide: Boolean)
private val linkifyCache = WeakHashMap<TextView, LinkifyState>()

fun TextView.linkify() {
    val current = text?.toString().orEmpty()
    val wide = Settings.wideUrlMatch.value
    val cached = linkifyCache[this]
    if (cached != null && cached.text == current && cached.wide == wide) {
        return
    }

    val spannable = SpannableStringBuilder(text)
    val existingSpans = spannable.getSpans(0, spannable.length, URLSpan::class.java)
    existingSpans.forEach { spannable.removeSpan(it) }
    val links = LinkText.ranges(spannable)
    links.reversed().forEach { range ->
        val start = range.first
        val end = range.last + 1
        val url = spannable.substring(start, end)

        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    Utils.openUrl(LinkText.withScheme(url))
                }
            },
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    text = spannable
    movementMethod = LinkMovementMethod.getInstance()
    linkifyCache[this] = LinkifyState(current, wide)
}
