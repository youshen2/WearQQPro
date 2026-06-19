package moye.wear.hook

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.AIOLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tencent.aio.api.list.IListUIOperationApi
import com.tencent.mvi.api.help.CreateViewParams
import com.tencent.watch.aio_impl.coreImpl.vb.WatchAIOListVB
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.drawable.roundCornerDrawable
import momoi.mod.qqpro.hook.action.CurrentMsgList
import momoi.mod.qqpro.lib.FrameScope
import momoi.mod.qqpro.lib.background
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.margin
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize

class BackToBottomAction(
    private val rv: RecyclerView,
    private val tv: TextView
) {
    init {
        tv.visibility = View.GONE
        tv.text = "回到底部"
        tv.setOnClickListener {
            val size = CurrentMsgList.msgList.value.size
            if (size > 0) rv.scrollToPosition(size - 1)
        }
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lm = recyclerView.layoutManager as? AIOLayoutManager ?: return
                val lastVisible = lm.findLastVisibleItemPosition()
                val total = CurrentMsgList.msgList.value.size
                val atBottom = total > 0 && lastVisible >= total - 1
                tv.visibility = if (atBottom) View.GONE else View.VISIBLE
            }
        })
    }
}

@Mixin
class BackToBottom : WatchAIOListVB() {
    @SuppressLint("RtlHardcoded")
    override fun h(
        createViewParams: CreateViewParams,
        childView: View,
        uiHelper: IListUIOperationApi
    ): View = FrameScope(super.h(createViewParams, childView, uiHelper) as FrameLayout).apply {
        if (!Settings.showBackToBottom.value) return@apply
        val tv = add<TextView>()
            .layoutGravity(Gravity.RIGHT or Gravity.BOTTOM)
            .background(roundCornerDrawable(0xFF_303030.toInt(), 9999f, 0f, 9999f, 0f))
            .padding(6.dp)
            .margin(bottom = 12.dp)
            .textSize(12f)
            .textColor(0xFF_22a6f2)
        BackToBottomAction(this@BackToBottom.H, tv)
    }.group
}
