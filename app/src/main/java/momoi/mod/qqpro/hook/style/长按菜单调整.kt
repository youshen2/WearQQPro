package momoi.mod.qqpro.hook.style

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.forEach
import com.tencent.qqnt.kernel.nativeinterface.MemberRole
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.tencent.qqnt.msg.KernelServiceUtil
import com.tencent.watch.aio_impl.ui.cell.base.WatchAIOGroupWidgetItemCell
import com.tencent.watch.aio_impl.ui.menu.AIOLongClickMenuFragment
import com.tencent.watch.aio_impl.ui.menu.MenuItemFactory
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Colors
import momoi.mod.qqpro.asGroup
import momoi.mod.qqpro.drawable.roundCornerDrawable
import momoi.mod.qqpro.enums.ElementType
import momoi.mod.qqpro.forEachAll
import momoi.mod.qqpro.hook.action.CurrentContact
import momoi.mod.qqpro.hook.action.CurrentGroupMembers
import momoi.mod.qqpro.hook.action.SelfContact
import momoi.mod.qqpro.hook.action.isGroup
import momoi.mod.qqpro.hook.aio_cell.forwardText
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.LinearScope
import momoi.mod.qqpro.lib.background
import momoi.mod.qqpro.lib.clickable
import momoi.mod.qqpro.lib.create
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.dpf
import momoi.mod.qqpro.lib.gravity
import momoi.mod.qqpro.lib.height
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.paddingHorizontal
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textSize
import momoi.mod.qqpro.lib.vh
import momoi.mod.qqpro.lib.width
import momoi.mod.qqpro.util.Utils

val menuSort = arrayOf(
    "回复",
    "@Ta",
    "复制文本",
    "复读文本",
    "去聊天",
    "加好友",
    "删除",
)

private fun process(group: ViewGroup, msg: MsgRecord?, dismiss: () -> Unit) {
    group.removeViewAt(0)
    val linear = group.getChildAt(0).asGroup()
        .getChildAt(0).asGroup()
        .getChildAt(0) as LinearLayout
    linear.background(0x44_000000)
    val items = mutableMapOf<String, View>()
    linear.forEach { item ->
        item.asGroup().forEachAll {
            if (it is AppCompatTextView) {
                items[it.text.toString()] = item
            }
        }
    }
    linear.removeAllViews()
    LinearScope(linear).add<View>()
        .width(FILL)
        .height(if (Utils.isRoundScreen) 0.16f.vh else 0)
    if (Utils.isRoundScreen) {
        linear.paddingHorizontal(0.1f.vh)
    }
    // 纯文本消息没有原生「转发」入口，这里手动注入一个转发按钮。
    if (msg != null) {
        val text = msg.elements
            ?.filter { it.elementType == ElementType.TEXT }
            ?.mapNotNull { it.textElement?.content }
            ?.joinToString("")
            ?.takeIf { it.isNotEmpty() }
        if (text != null) {
            linear.addView(
                create<TextView>(linear.context)
                    .width(FILL)
                    .gravity(Gravity.CENTER)
                    .padding(6.dp)
                    .text("转发")
                    .textSize(16f)
                    .background(roundCornerDrawable(
                        color = Colors.replyBackground,
                        radius = 16.dpf
                    ))
                    .clickable {
                        linear.forwardText(text)
                        dismiss()
                    }
            )
        }
    }
    if (msg != null && CurrentContact.isGroup) {
        CurrentGroupMembers.get(SelfContact.peerUid) {
            if (it.role == MemberRole.OWNER || it.role == MemberRole.ADMIN) {
                linear.post {
                    linear.addView(
                        create<TextView>(linear.context)
                            .width(FILL)
                            .gravity(Gravity.CENTER)
                            .padding(6.dp)
                            .text("撤回")
                            .textSize(16f)
                            .background(roundCornerDrawable(
                                color = Colors.replyBackground,
                                radius = 16.dpf
                            ))
                            .clickable {
                                KernelServiceUtil.c()?.recallMsg(CurrentContact, msg.msgId, null)
                            }
                    )
                }
            }
        }
    }
    menuSort.forEach {
        items[it]?.let { item ->
            linear.addView(item)
        }
    }
    items.values.forEach {
        if (it.parent == null) {
            linear.addView(it, 1)
        }
    }
    if (Utils.isRoundScreen) {
        LinearScope(linear).add<View>()
            .width(FILL)
            .height(0.16f.vh)
    }
}

@Mixin
class 长按菜单调整(p0: (MenuItemFactory.ItemEnum) -> Unit, p1: String?) :
    AIOLongClickMenuFragment(p0, p1) {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 自建长按菜单没有原生 cell，上面的撤回按钮在这种场景下直接降级跳过。
        val msg = runCatching {
            val field = this.b.javaClass.getDeclaredField("b")
            field.isAccessible = true
            val cell = field.get(this.b) as WatchAIOGroupWidgetItemCell<*, *>
            cell.f()!!.d
        }.getOrNull()
        return super.onCreateView(inflater, container, savedInstanceState).apply {
            this.asGroup().getChildAt(0).asGroup().let { group ->
                process(group, msg) { dismiss() }
            }
        }
    }
}
