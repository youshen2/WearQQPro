package momoi.mod.qqpro.lib

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop

const val WRAP = WRAP_CONTENT
const val FILL = MATCH_PARENT
fun <T : View> T.layoutParams(
    params: LayoutParams
) = apply {
    layoutParams = params
}
fun <T : View> T.margin(
    left: Int = marginLeft,
    top: Int = marginTop,
    right: Int = marginRight,
    bottom: Int = marginBottom
): T =
    apply {
        val params = layoutParams as? ViewGroup.MarginLayoutParams
        params?.setMargins(left, top, right, bottom)
    }

fun <T : View> T.marginHorizontal(value: Int) = margin(left = value, right = value)
fun <T : View> T.marginVertical(value: Int) = margin(top = value, bottom = value)

fun <T : View> T.padding(left: Int = paddingLeft, top: Int = paddingTop, right: Int = paddingRight, bottom: Int = paddingBottom) = apply {
    setPadding(left, top, right, bottom)
}
fun <T : View> T.paddingHorizontal(value: Int) = padding(left = value, right = value)
fun <T : View> T.paddingVertical(value: Int) = padding(top = value, bottom = value)
fun <T : View> T.padding(value: Int) = padding(value, value, value, value)

fun <T : View> T.background(color: Int) = apply {
    setBackgroundColor(color)
}
fun <T : View> T.background(color: Long) = apply {
    setBackgroundColor(color.toInt())
}
fun <T : View> T.background(drawable: Drawable?) = apply {
    background = drawable
}
fun <T : View> T.size(width: Int = layoutParams.width, height: Int = layoutParams.height) = apply {
    layoutParams.width = width
    layoutParams.height = height
}
fun <T : View> T.size(value: Int) = size(value, value)
fun <T : View> T.width(value: Int) = size(width = value)
fun <T : View> T.height(value: Int) = size(height = value)
fun <T : View> T.id(value: Int) = apply {
    id = value
}

inline fun <T : View> T.clickable(crossinline onClick: ()->Unit) = apply {
    setOnClickListener {
        onClick()
    }
}

inline fun <T : View> T.longClickable(crossinline onLongClick: () -> Unit) = apply {
    setOnLongClickListener {
        onLongClick()
        true
    }
}
