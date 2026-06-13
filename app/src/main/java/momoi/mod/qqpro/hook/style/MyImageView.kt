package momoi.mod.qqpro.hook.style

import android.content.Context
import android.content.res.Resources
import android.widget.ImageView

class MyImageView(context: Context) : ImageView(context) {
    init {
        adjustViewBounds = true
        scaleType = ScaleType.FIT_CENTER
        maxHeight = (heightLimit * 1.5f).toInt()
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        maxHeight = (heightLimit * 1.5f).toInt()
        maxWidth = drawable?.takeIf { it.intrinsicHeight > 0 }?.let {
            (heightLimit / it.intrinsicHeight * it.intrinsicWidth).toInt()
        } ?: Int.MAX_VALUE
        if (maxWidth < maxHeight) maxWidth = maxHeight
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST)
        )
    }
}