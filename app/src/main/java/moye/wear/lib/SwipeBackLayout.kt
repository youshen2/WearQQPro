package moye.wear.lib

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import momoi.mod.qqpro.Settings
import kotlin.math.abs

class SwipeBackLayout(context: Context) : FrameLayout(context) {

    var onSwipeBack: (() -> Unit)? = null

    var ignoreDisableSetting = false

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var tracking = false

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!ignoreDisableSetting && Settings.disableSwipeBack.value) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.rawX
                downY = ev.rawY
                tracking = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY
                if (dx > touchSlop && dx > abs(dy) * 1.5f) {
                    tracking = true
                    return true
                }
            }
        }
        return false
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_MOVE -> {
                if (tracking) {
                    translationX = (ev.rawX - downX).coerceAtLeast(0f)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (tracking) {
                    tracking = false
                    val dx = ev.rawX - downX
                    if (ev.actionMasked == MotionEvent.ACTION_UP && dx > width * 0.3f) {
                        onSwipeBack?.invoke()
                    } else {
                        animate().translationX(0f).setDuration(150).start()
                    }
                }
            }
        }
        return tracking
    }
}
