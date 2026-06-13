package momoi.mod.qqpro.hook.view

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import momoi.mod.qqpro.hook.action.CurrentMsgList

/**
 * 直接跳到 [position] 并把该项吸附到顶部，无动画。聊天记录搜索使用此方法，
 * 因为目标距离很远时 [smoothScrollToStart] 的逐项动画会非常慢。
 */
fun RecyclerView.scrollToStartInstant(position: Int) {
    (layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(position, 0)
        ?: scrollToPosition(position)
}

fun RecyclerView.smoothScrollToStart(position: Int) {
    layoutManager?.startSmoothScroll(
        object : LinearSmoothScroller(context) {
            init {
                targetPosition = position
            }

            override fun getVerticalSnapPreference(): Int {
                return SNAP_TO_START
            }
        })
}