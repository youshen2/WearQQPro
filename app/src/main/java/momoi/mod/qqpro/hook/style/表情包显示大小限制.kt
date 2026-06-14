package momoi.mod.qqpro.hook.style

import android.content.Context
import android.content.res.Resources
import com.tencent.watch.aio_impl.ui.widget.RoundBubbleImageView
import me.jessyan.autosize.AutoSizeConfig
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Settings

val heightLimit: Float get() = Resources.getSystem().displayMetrics.heightPixels * Settings.picMaxHeightRatio.value

@Mixin
class 表情包显示大小限制(context: Context) : RoundBubbleImageView(context) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = drawable
        val iw = d?.intrinsicWidth ?: 0
        val ih = d?.intrinsicHeight ?: 0
        val box = maxOf(layoutParams?.width ?: 0, layoutParams?.height ?: 0)
        if (iw > 0 && ih > 0 && box > 0) {
            var w: Int
            var h: Int
            if (iw >= ih) {
                w = box
                h = (box.toFloat() / iw * ih).toInt()
            } else {
                h = box
                w = (box.toFloat() / ih * iw).toInt()
            }
            val cap = heightLimit.toInt()
            if (h > cap) {
                w = (w.toFloat() * cap / h).toInt()
                h = cap
            }
            setMeasuredDimension(w.coerceAtLeast(1), h.coerceAtLeast(1))
            return
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}