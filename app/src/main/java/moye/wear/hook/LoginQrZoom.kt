package moye.wear.hook

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import com.tencent.qqnt.account.login.ui.QrLoginFragment
import com.tencent.qqnt.watch.selftab.ui.SelfQrFragment
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.util.Utils
import momoi.mod.qqpro.util.runOnUi

@Mixin
class LoginQrZoom : QrLoginFragment() {
    override fun Y(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = super.Y(inflater, container, savedInstanceState)
        LoginQrZoomHelper.attach(root)
        return root
    }

    override fun Q() {
        super.Q()
        LoginQrZoomHelper.dismiss()
    }

    override fun k() {
        super.k()
        LoginQrZoomHelper.dismiss()
    }
}

@Mixin
class SelfQrZoom : SelfQrFragment() {
    override fun Y(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.Y(inflater, container, savedInstanceState)
        if (root != null) SelfQrZoomHelper.attach(root)
        return root
    }
}

object SelfQrZoomHelper {
    fun attach(root: View) {
        try {
            val ctx = root.context
            val id = ctx.resources.getIdentifier("qr_code", "id", ctx.packageName)
            val qr = if (id != 0) root.findViewById<View>(id) else null
            if (qr == null) {
                Utils.log("SelfQrZoom: qr_code not found")
                return
            }
            LoginQrZoomHelper.attachTapZoom(qr)
        } catch (e: Throwable) {
            Utils.log("SelfQrZoom: attach failed: ${e.javaClass.simpleName}: ${e.message}")
        }
    }
}

object LoginQrZoomHelper {
    @Volatile
    private var current: FrameLayout? = null

    fun dismiss() {
        val o = current ?: return
        runOnUi {
            (o.parent as? ViewGroup)?.removeView(o)
            if (current === o) current = null
        }
    }

    fun attach(root: View) {
        try {
            val ctx = root.context
            val id = ctx.resources.getIdentifier("qr_code_container", "id", ctx.packageName)
            val qr = if (id != 0) root.findViewById<View>(id) else null
            if (qr == null) {
                Utils.log("LoginQrZoom: qr_code_container not found")
                return
            }
            attachTapZoom(qr)
        } catch (e: Throwable) {
            Utils.log("LoginQrZoom: attach failed: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

    fun attachTapZoom(qr: View) {
        qr.setOnClickListener { showZoom(qr) }
    }

    private fun showZoom(qr: View) {
        if (qr.width == 0 || qr.height == 0) return
        val activity = qr.context.findActivity() ?: return
        val content = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val snapshot = Bitmap.createBitmap(qr.width, qr.height, Bitmap.Config.ARGB_8888)
        Canvas(snapshot).apply { qr.draw(this) }
        val side = minOf(content.width, content.height)
        val overlay = FrameLayout(activity).apply {
            setBackgroundColor(0xE6000000.toInt())
        }
        val image = ImageView(activity).apply {
            setImageBitmap(snapshot)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        overlay.addView(image, FrameLayout.LayoutParams(side, side, Gravity.CENTER))
        overlay.setOnClickListener {
            content.removeView(overlay)
            if (current === overlay) current = null
        }
        content.addView(overlay, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        current = overlay
    }

    private fun Context.findActivity(): Activity? {
        var ctx = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
