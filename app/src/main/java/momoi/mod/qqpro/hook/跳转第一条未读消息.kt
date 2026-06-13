package momoi.mod.qqpro.hook

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.AIOLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tencent.aio.api.list.IListUIOperationApi
import com.tencent.mvi.api.help.CreateViewParams
import com.tencent.watch.aio_impl.coreImpl.vb.WatchAIOListVB
import com.tencent.watch.aio_impl.data.WatchAIOMsgItem
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.MsgUtil
import momoi.mod.qqpro.util.Utils
import momoi.mod.qqpro.asGroup
import momoi.mod.qqpro.drawable.roundCornerDrawable
import momoi.mod.qqpro.hook.action.CurrentContact
import momoi.mod.qqpro.hook.action.CurrentMsgList
import momoi.mod.qqpro.hook.action.RecentContacts
import momoi.mod.qqpro.hook.view.smoothScrollToStart
import momoi.mod.qqpro.lib.FrameScope
import momoi.mod.qqpro.lib.background
import momoi.mod.qqpro.lib.clickable
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize

class SkipAction(
    private val rv: RecyclerView,
    private val tv: TextView,
    private val recent: RecentContacts.Data
): View.OnClickListener {

    private fun format(count: Int) = "↑ ${count}条新消息"
    private var count = recent.unreadCntCached
    private var lastUnreadMsg: WatchAIOMsgItem? = null
    private var isClicked = false

    init {
        tv.text = format(count)
        tv.setOnClickListener(this)
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            var isFinished = false
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (isFinished) return
                val first = (rv.layoutManager as AIOLayoutManager).findFirstVisibleItemPosition()
                if (first == -1) return
                val newCount = recent.unreadCntCached - CurrentMsgList.msgList.value.size + first
                if (newCount < count) {
                    count = newCount
                    lastUnreadMsg = CurrentMsgList.msgList.value.getOrNull(first)
                    if (count > 0) {
                        tv.text = format(count)
                    } else {
                        isFinished = true
                        tv.visibility = View.GONE
                    }
                }
            }
        })
    }

    override fun onClick(v: View?) {
        if (isClicked) return
        val list = CurrentMsgList.msgList.value
        Utils.log("SkipAction onClick. count: $count, lastUnreadMsg: $lastUnreadMsg, listSize: ${list.size}")
        when {
            // 已经滚动过，记录了最后一条未读消息，按其索引跳转
            lastUnreadMsg != null -> {
                CurrentMsgList.upwardMsg(CurrentMsgList.getMsgIndex(lastUnreadMsg!!), count) {
                    rv.scrollToPosition(it)
                }
                isClicked = true
            }
            // 还未滚动过，直接从最新一条消息向上推算首条未读的位置
            list.isNotEmpty() && count > 0 -> {
                CurrentMsgList.upwardMsg(list.size - 1, count - 1) {
                    rv.scrollToPosition(it)
                }
                isClicked = true
            }
        }
    }
}
@Mixin
class 跳转第一条未读消息 : WatchAIOListVB() {
    @SuppressLint("RtlHardcoded")
    override fun h(
        createViewParams: CreateViewParams,
        childView: View,
        uiHelper: IListUIOperationApi
    ): View = FrameScope(super.h(createViewParams, childView, uiHelper) as FrameLayout).apply {
        val peerUid = CurrentContact.peerUid
        Utils.log("current peerUid: $peerUid")
        RecentContacts.get(peerUid)?.let { recent ->
            Utils.log("unreadCntCached: ${recent.unreadCntCached}")
            if (recent.unreadCntCached > 0) {
                val tv = add<TextView>()
                    .layoutGravity(Gravity.RIGHT or Gravity.TOP)
                    .background(roundCornerDrawable(0xFF_303030.toInt(), 9999f, 0f, 9999f, 0f))
                    .padding(6.dp)
                    .textSize(12f)
                    .textColor(0xFF_22a6f2)
                SkipAction(this@跳转第一条未读消息.H, tv, recent)
            }
        }
    }.group
}