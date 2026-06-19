package momoi.mod.qqpro.hook

import android.content.Context
import android.os.Looper
import android.content.res.Resources
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.ScrollView
import androidx.core.view.MotionEventCompat
import androidx.core.view.ViewConfigurationCompat
import androidx.recyclerview.widget.RecyclerView
import com.tencent.qqlive.module.videoreport.inject.dialog.ReportDialog
import com.tencent.qqnt.watch.mainframe.MainActivity
import com.tencent.richframework.widget.matrix.RFWMatrixImageView
import me.jessyan.autosize.AutoSizeCompat
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.asGroup
import momoi.mod.qqpro.forEachAll
import kotlin.math.roundToInt

private val screenCenterX = Resources.getSystem().displayMetrics.widthPixels / 2
private val point = intArrayOf(Int.MIN_VALUE, 0)

@Mixin
class 滚轮适配配(context: Context) : ReportDialog(context) {
    private var targetView: View? = null
    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (targetView?.isInCenter() != true) {
            targetView = window?.decorView?.let { findTarget(it) }
        }
        val delta =
            -ev.getAxisValue(MotionEventCompat.AXIS_SCROLL) * ViewConfigurationCompat.getScaledVerticalScrollFactor(
                ViewConfiguration.get(context), context
            ) * Settings.encoderScrollSpeed.value
        (targetView as? RecyclerView)?.let {
            if (Settings.enableSmoothScroll.value) {
                it.smoothScrollBy(0, delta.roundToInt())
            } else {
                it.scrollBy(0, delta.roundToInt())
            }
        }
        (targetView as? ScrollView)?.let {
            if (Settings.enableSmoothScroll.value) {
                it.smoothScrollBy(0, delta.roundToInt())
            } else {
                it.scrollBy(0, delta.roundToInt())
            }
        }
        (targetView as? RFWMatrixImageView)?.let {
            it.scale = (it.scale * (1 + 0.001f * delta)).coerceIn(it.minimumScale, it.maximumScale)
        }
        targetView?.scrollBy(0, delta.roundToInt())
        return super.dispatchGenericMotionEvent(ev)
    }
}

@Mixin
class 滚轮适配 : MainActivity() {
    private var targetView: View? = null
    private var action: (Any.(Float)->Unit)? = null

    override fun getResources(): Resources {
        val res = super.getResources()
        if (Looper.myLooper() == Looper.getMainLooper()) {
            runCatching { AutoSizeCompat.autoConvertDensityOfGlobal(res) }
        }
        return res
    }

    override fun onResume() {
        super.onResume()
        runCatching {
            AutoSizeCompat.autoConvertDensityOfGlobal(resources)
            window?.decorView?.requestLayout()
        }
    }

    override fun dispatchGenericMotionEvent(ev: MotionEvent): Boolean {
        if (targetView?.isInCenter() != true) {
            targetView = findTarget(window.decorView)
        }
        val delta =
            -ev.getAxisValue(MotionEventCompat.AXIS_SCROLL) * ViewConfigurationCompat.getScaledVerticalScrollFactor(
                ViewConfiguration.get(this), this
            ) * Settings.encoderScrollSpeed.value
        (targetView as? RecyclerView)?.let {
            if (Settings.enableSmoothScroll.value) {
                it.smoothScrollBy(0, delta.roundToInt())
            } else {
                it.scrollBy(0, delta.roundToInt())
            }
        }
        (targetView as? ScrollView)?.let {
            if (Settings.enableSmoothScroll.value) {
                it.smoothScrollBy(0, delta.roundToInt())
            } else {
                it.scrollBy(0, delta.roundToInt())
            }
        }
        (targetView as? RFWMatrixImageView)?.let {
            it.scale = (it.scale * (1 + 0.001f * delta)).coerceIn(it.minimumScale, it.maximumScale)
        }
        return super.dispatchGenericMotionEvent(ev)
    }
}

private fun findTarget(rootView: View): View? {
    var target: View? = null
    rootView.asGroup().forEachAll {
        if (target != null) return@forEachAll
        val rv = (it as? RecyclerView)?.layoutManager?.canScrollVertically() == true
        val lv = it is ScrollView
        val iv = it is RFWMatrixImageView
        if ((rv || lv || iv) && it.isInCenter()) {
            target = it
        }
    }
    return target
}

fun View.isInCenter(): Boolean {
    if (!isAttachedToWindow) return false
    getLocationOnScreen(point)
    return point[0] <= screenCenterX && point[0] + width > screenCenterX
}
