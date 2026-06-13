package momoi.mod.qqpro.hook.aio_cell

import android.annotation.SuppressLint
import android.content.Context
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.tencent.qqnt.kernel.nativeinterface.MemberInfo
import com.tencent.qqnt.kernel.nativeinterface.MemberRole
import com.tencent.watch.aio_impl.data.WatchAIOMsgItem
import com.tencent.watch.aio_impl.ui.cell.base.BaseWatchItemCell
import com.tencent.watch.aio_impl.ui.cell.unsupport.WatchToQQViewMsgItem
import com.tencent.watch.aio_impl.ui.widget.AIOCellGroupWidget
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Colors
import momoi.mod.qqpro.enums.NTMsgType
import momoi.mod.qqpro.hook.action.CurrentContact
import momoi.mod.qqpro.hook.action.CurrentGroupMembers
import momoi.mod.qqpro.hook.action.SelfContact
import momoi.mod.qqpro.hook.action.isGroup
import momoi.mod.qqpro.lib.RadiusBackgroundSpan
import momoi.mod.qqpro.lib.create
import momoi.mod.qqpro.util.linkify
import momoi.mod.qqpro.warp
import moye.wear.hook.LinkPreview
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
            val widget = view as? AIOCellGroupWidget ?: return
            if (CurrentContact.isGroup) {
                val raw = widget.getNickWidget<TextView>()?.text!!
                CurrentGroupMembers.get(item.d.senderUid) {
                    widget.post {
                        if (widget.getNickWidget<TextView>()?.text == raw) {
                            widget.getNickWidget<TextView>()?.text = it.toDisplay()
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
                it.layoutParams?.let {
                    it.width = ViewGroup.LayoutParams.WRAP_CONTENT
                    it.height = ViewGroup.LayoutParams.WRAP_CONTENT
                }
            }
            // 文本消息下方挂载链接预览卡片（按开关与是否含链接决定显隐）
            LinkPreview.bind(widget, (widget.contentWidget as? TextView)?.text)
        }
    }

    private const val TYPE_OWNER = 1
    private const val TYPE_ADMIN = 2
    private const val TYPE_SPECIAL = 3
    private const val TYPE_NORMAL = 0
    private fun MemberInfo.toDisplay() = buildSpannedString {
        val type = when (true) {
            (role == MemberRole.OWNER) -> TYPE_OWNER
            (role == MemberRole.ADMIN) -> TYPE_ADMIN
            !memberSpecialTitle.isNullOrEmpty() -> TYPE_SPECIAL
            else -> TYPE_NORMAL
        }
        val name = when (true) {
            cardName.isNotEmpty() -> cardName
            remark.isNotEmpty() -> remark
            else -> nick
        }
        val isSelf = uid == SelfContact.peerUid
        if (isSelf) {
            append(name)
            append(" ")
        }
        inSpans(
            RadiusBackgroundSpan(
                bgColor = when (type) {
                    TYPE_ADMIN -> Colors.NickTag.adminBg
                    TYPE_OWNER -> Colors.NickTag.ownerBg
                    TYPE_SPECIAL -> Colors.NickTag.specialBg
                    else -> Colors.NickTag.normalBg
                },
                textColor = when (type) {
                    TYPE_ADMIN -> Colors.NickTag.adminText
                    TYPE_OWNER -> Colors.NickTag.ownerText
                    TYPE_SPECIAL -> Colors.NickTag.specialText
                    else -> Colors.NickTag.normalText
                }
            ),
            RelativeSizeSpan(0.8f)
        ) {
            append("LV")
            append(memberLevel.toString())
            if (!memberSpecialTitle.isNullOrEmpty()) {
                append(" ")
                append(memberSpecialTitle)
            } else {
                when (type) {
                    TYPE_OWNER -> append(" 群主")
                    TYPE_ADMIN -> append(" 管理员")
                }
            }
        }
        if (!isSelf) {
            append(" ")
            append(name)
        }
    }
}