package moye.wear.hook

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.tencent.qqnt.kernel.nativeinterface.FileTransNotifyInfo
import com.tencent.watch.aio_impl.coreImpl.payLoad.AIOMsgItemPayload
import com.tencent.watch.aio_impl.coreImpl.payLoad.AIOMsgItemPayloadType
import com.tencent.watch.aio_impl.data.WatchAIOMsgItem
import com.tencent.watch.aio_impl.ui.cell.video.WatchVideoGroupWidget
import com.tencent.watch.aio_impl.ui.cell.video.WatchVideoMsgItem
import kotlin.math.roundToInt

object VideoDownloadProgressHook {
    private const val PLAY_ICON_RES = 2114453679
    private const val PROGRESS_TAG = "qqpro_video_download_progress"

    fun bind(view: View, item: WatchAIOMsgItem, payloads: List<Any>) {
        val widget = view as? WatchVideoGroupWidget ?: return
        val videoItem = item as? WatchVideoMsgItem ?: return
        val progress = payloads.downloadProgress()
        if (progress != null) {
            widget.showDownloadProgress(progress)
        } else {
            widget.hideDownloadProgress(!videoItem.A())
        }
    }

    private fun List<Any>.downloadProgress(): Int? {
        for (payload in this) {
            val richMediaPayload = (payload as? Map<*, *>)
                ?.get(AIOMsgItemPayloadType.d) as? AIOMsgItemPayload.RichMediaPayload ?: continue
            return richMediaPayload.a.downloadProgress()
        }
        return null
    }

    private fun FileTransNotifyInfo.downloadProgress(): Int? {
        if (fileDownType == 2 || trasferStatus == 4 || trasferStatus == 5) return null
        if (totalSize > 0L) {
            return ((fileProgress.toFloat() / totalSize.toFloat()) * 100f).roundToInt().coerceIn(0, 99)
        }
        if (fileProgress in 0..99) {
            return fileProgress.toInt()
        }
        return null
    }

    private fun WatchVideoGroupWidget.showDownloadProgress(progress: Int) {
        val stateView = `getStateView$aio_impl_release`()
        stateView.setImageDrawable(null)
        stateView.visibility = View.INVISIBLE
        progressText()?.apply {
            syncWithCover()
            text = "$progress%"
            visibility = View.VISIBLE
            bringToFront()
        }
    }

    private fun WatchVideoGroupWidget.hideDownloadProgress(resetPlayIcon: Boolean) {
        val stateView = `getStateView$aio_impl_release`()
        progressText(false)?.visibility = View.GONE
        if (resetPlayIcon) {
            stateView.setImageResource(PLAY_ICON_RES)
        }
        stateView.visibility = View.VISIBLE
    }

    private fun WatchVideoGroupWidget.progressText(create: Boolean = true): TextView? {
        val stateView = `getStateView$aio_impl_release`()
        val parent = stateView.parent as? ViewGroup ?: return null
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child is TextView && child.tag == PROGRESS_TAG) return child
        }
        if (!create) return null
        return TextView(context).apply {
            tag = PROGRESS_TAG
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
            background = GradientDrawable().apply {
                setColor(0x99000000.toInt())
                cornerRadius = 8.dp.toFloat()
            }
            setShadowLayer(3f, 0f, 1f, Color.BLACK)
            isClickable = false
            isFocusable = false
            visibility = View.GONE
            parent.addView(this)
        }
    }

    private fun WatchVideoGroupWidget.syncWithCover() {
        val cover = `getCoverImage$aio_impl_release`()
        val overlay = progressText(false) ?: return
        val coverLp = cover.layoutParams
        val width = coverLp?.width?.takeIf { it > 0 } ?: cover.width.takeIf { it > 0 }
        val height = coverLp?.height?.takeIf { it > 0 } ?: cover.height.takeIf { it > 0 }
        overlay.layoutParams = FrameLayout.LayoutParams(
            width ?: ViewGroup.LayoutParams.MATCH_PARENT,
            height ?: ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER
        )
    }

    private val Int.dp: Int
        get() = (this * android.content.res.Resources.getSystem().displayMetrics.density).roundToInt()
}
