package moye.wear.span

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Spanned
import android.text.style.ReplacementSpan

/**
 * Draws a single placeholder char as a blue chip showing [displayText]
 * (e.g. "@nickname" or "[图片]"). Ported from old_src moye.wearqq.span.AtSpan.
 */
class AtSpan(private val displayText: String) : ReplacementSpan() {

    private val color: Int = Color.parseColor("#4D8DFF")

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        if (text is Spanned && start != text.getSpanStart(this)) return 0
        return paint.measureText(displayText).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        if (text is Spanned && start != text.getSpanStart(this)) return
        val saved = paint.color
        paint.color = color
        canvas.drawText(displayText, x, y.toFloat(), paint)
        paint.color = saved
    }
}
