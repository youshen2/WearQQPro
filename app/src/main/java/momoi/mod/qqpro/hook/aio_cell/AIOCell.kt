package momoi.mod.qqpro.hook.aio_cell

import android.annotation.SuppressLint
import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.tencent.watch.aio_impl.data.WatchAIOMsgItem
import com.tencent.watch.aio_impl.ui.cell.base.BaseWatchItemCell
import com.tencent.watch.aio_impl.ui.cell.unsupport.WatchToQQViewMsgItem
import com.tencent.watch.aio_impl.ui.widget.AIOCellGroupWidget
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.enums.NTMsgType
import momoi.mod.qqpro.hook.action.CurrentContact
import momoi.mod.qqpro.hook.action.CurrentGroupMembers
import momoi.mod.qqpro.hook.action.CurrentMsgList
import momoi.mod.qqpro.hook.action.isGroup
import momoi.mod.qqpro.lib.create
import momoi.mod.qqpro.util.linkify
import momoi.mod.qqpro.warp
import moye.wear.hook.GroupAvatarHook
import moye.wear.hook.LinkPreview
import moye.wear.hook.parseAtMembers
import java.lang.ref.WeakReference
import java.util.WeakHashMap

object AIOCell {
    val AIOCellGroupWidget.contentWidget get() = this.getContentWidget<View>()!!
    val hooks = mutableListOf<Hook<*>>()

    init {
        addHook<ReplyView>(
            type = NTMsgType.REPLY,
            onBind = { msg, widget ->
                val reply = msg.elements.firstNotNullOf { it.replyElement }
                loadData(CurrentContact, reply)
                setOnClickListener(ReplyClick(widget, reply))
            },
            appendMode = true
        )
        addHook<ForwardMsgView>(
            type = NTMsgType.MULTIMSGFORWARD,
            onBind = { msg, widget ->
                if (msg.forwardData == null) {
                    msg.forwardData = ForwardMsgData(CurrentContact, msg, msg)
                }
                loadData(CurrentContact, msg.forwardData!!)
            },
        )
        addHook<CardMsgView>(
            type = NTMsgType.ARKSTRUCT,
            onBind = { msg, widget ->
                loadData(msg.elements.firstNotNullOf { it.arkElement })
            }
        )
    }

    inline fun <reified T : View> addHook(
        type: Int,
        noinline onBind: T.(MsgRecordEx, AIOCellGroupWidget) -> Unit,
        appendMode: Boolean = false
    ) {
        hooks.add(
            Hook(
                type = type,
                onBind = onBind,
                createView = { create<T>(it) },
                appendMode = appendMode
            )
        )
    }

    class Hook<T : View>(
        val type: Int,
        private val onBind: T.(MsgRecordEx, AIOCellGroupWidget) -> Unit,
        val createView: (Context) -> T,
        val appendMode: Boolean
    ) {
        private val views = WeakHashMap<AIOCellGroupWidget, WeakReference<T>>()
        @Suppress("UNCHECKED_CAST")
        fun bind(widget: AIOCellGroupWidget, view: View, msg: MsgRecordEx) {
            view.visibility = View.VISIBLE
            if (!appendMode) {
                widget.contentWidget.visibility = View.GONE
            }
            onBind(view as T, msg, widget)
        }

        fun getOrCreate(widget: AIOCellGroupWidget): T {
            return views.getOrPut(widget) {
                val view = createView(widget.context)
                val warp = widget.contentWidget.warp()
                warp.addView(view, 0)
                WeakReference(view)
            }.get()!!
        }

        fun recover(widget: AIOCellGroupWidget) {
            views[widget]?.get()?.let {
                it.visibility = View.GONE
                if (!appendMode) {
                    widget.contentWidget.visibility = View.VISIBLE
                }
            }
        }
    }

    @Mixin
    abstract class HookCell : BaseWatchItemCell<WatchAIOMsgItem, View>() {
        @SuppressLint("SetTextI18n")
        override fun i(
            view: View,
            item: WatchAIOMsgItem,
            p3: Int,
            p4: List<Any>,
            p5: Lifecycle,
            p6: LifecycleOwner?
        ) {
            super.i(view, item, p3, p4, p5, p6)
            if (Settings.parseAtMember.value && view is TextView) {
                if (view.movementMethod !is LinkMovementMethod) {
                    view.movementMethod = LinkMovementMethod.getInstance()
                    view.highlightColor = 0x33888888
                }
            }
            val widget = view as? AIOCellGroupWidget ?: return
            if (CurrentContact.isGroup) {
                val senderUid = item.d.senderUid
                val hideHeader = Settings.hideRepeatedSender.value
                        && item.d.msgType != NTMsgType.GRAYTIPS
                        && run {
                    val idx = CurrentMsgList.getMsgIndex(item)
                    val prev = CurrentMsgList.msgList.value.getOrNull(idx - 1)
                    prev != null && prev.d.msgType != NTMsgType.GRAYTIPS
                            && prev.d.senderUid == senderUid
                }
                val nickView = widget.getNickWidget<TextView>()
                if (hideHeader) {
                    nickView?.let {
                        it.visibility = View.VISIBLE
                        it.text = ""
                        it.setCompoundDrawables(null, null, null, null)
                        it.layoutParams = it.layoutParams?.apply { height = 0 }
                    }
                } else {
                    nickView?.layoutParams = nickView?.layoutParams?.apply {
                        height = ViewGroup.LayoutParams.WRAP_CONTENT
                    }
                    val raw = nickView?.text!!
                    CurrentGroupMembers.get(senderUid) { member ->
                        widget.post {
                            if (widget.getNickWidget<TextView>()?.text == raw) {
                                GroupAvatarHook.update(widget, member)
                            }
                        }
                    }
                }
            }
            hooks.forEach {
                if (item.d.msgType == it.type) {
                    val view = it.getOrCreate(widget)
                    it.bind(widget, view, item.d as MsgRecordEx)
                    (item as? WatchToQQViewMsgItem)?.o = ""
                } else {
                    it.recover(widget)
                }
            }
            (widget.contentWidget as? TextView)?.let {
                it.linkify()
                it.parseAtMembers()
                it.layoutParams?.let {
                    it.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    it.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
            LinkPreview.bind(widget, (widget.contentWidget as? TextView)?.text)
        }
    }
}