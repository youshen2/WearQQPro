package momoi.mod.qqpro.drawable

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

fun atIconDrawable(): Drawable = object : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF_FFFFFF.toInt()
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val s = minOf(b.width(), b.height()).toFloat()
        paint.textSize = s * 0.92f
        val fm = paint.fontMetrics
        val y = b.exactCenterY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText("@", b.exactCenterX(), y, paint)
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

class PanelIcons {

    fun gifSearchIconDrawable(): Drawable = object : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF_FFFFFF.toInt()
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        override fun draw(canvas: Canvas) {
            val b = bounds
            val s = minOf(b.width(), b.height()).toFloat()
            paint.textSize = s * 0.52f
            val fm = paint.fontMetrics
            val y = b.exactCenterY() - (fm.ascent + fm.descent) / 2f
            canvas.drawText("GIF", b.exactCenterX(), y, paint)
        }

        override fun setAlpha(alpha: Int) { paint.alpha = alpha }
        override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
        override fun getOpacity() = PixelFormat.TRANSLUCENT
    }

}