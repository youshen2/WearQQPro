package momoi.mod.qqpro.hook.style

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.forEach
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import com.tencent.qqnt.kernel.nativeinterface.MemberRole
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.tencent.qqnt.msg.KernelServiceUtil
import com.tencent.watch.aio_impl.ui.cell.base.WatchAIOGroupWidgetItemCell
import com.tencent.watch.aio_impl.ui.menu.AIOLongClickMenuFragment
import com.tencent.watch.aio_impl.ui.menu.MenuItemFactory
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.asGroup
import momoi.mod.qqpro.enums.ElementType
import momoi.mod.qqpro.forEachAll
import momoi.mod.qqpro.hook.action.CurrentContact
import momoi.mod.qqpro.hook.action.CurrentGroupMembers
import momoi.mod.qqpro.hook.action.SelfContact
import momoi.mod.qqpro.hook.action.isGroup
import momoi.mod.qqpro.hook.aio_cell.forwardMsgRecord
import momoi.mod.qqpro.hook.aio_cell.forwardText
import momoi.mod.qqpro.hook.aio_cell.isForwardableMsgRecord
import momoi.mod.qqpro.hook.view.PartialCopyFragment
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.LinearScope
import momoi.mod.qqpro.lib.background
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.height
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.paddingHorizontal
import momoi.mod.qqpro.lib.vh
import momoi.mod.qqpro.lib.width
import momoi.mod.qqpro.util.Utils
import moye.wear.hook.LongPressMenuOrderManager

private const val NATIVE_MENU_ITEM_LAYOUT = 2114715895
private const val NATIVE_MENU_SWITCH_ID = 2114519474
private const val NATIVE_MENU_TEXT_ID = 2114519756
private const val NATIVE_MENU_ICON_ID = 2114520149

private fun createNativeMenuItem(
    parent: LinearLayout,
    text: String,
    onClick: () -> Unit,
): View {
    val item = LayoutInflater.from(parent.context).inflate(NATIVE_MENU_ITEM_LAYOUT, parent, false)
    item.findViewById<AppCompatTextView>(NATIVE_MENU_TEXT_ID)?.text = text
    item.findViewById<ImageView>(NATIVE_MENU_ICON_ID)?.isVisible = false
    item.findViewById<View>(NATIVE_MENU_SWITCH_ID)?.isVisible = false
    item.setOnClickListener { onClick() }
    return item
}

private fun process(
    group: ViewGroup,
    msg: MsgRecord?,
    fm: FragmentManager?,
    dismiss: () -> Unit
) {
    group.removeViewAt(0)
    val linear = group.getChildAt(0).asGroup()
        .getChildAt(0).asGroup()
        .getChildAt(0) as LinearLayout
    linear.background(0x44_000000)
    val items = linkedMapOf<String, View>()
    linear.forEach { item ->
        item.asGroup().forEachAll {
            if (it is TextView) {
                items[it.text.toString()] = item
            }
        }
    }

    fun renderItems() {
        LongPressMenuOrderManager.rememberLabels(items.keys)
        linear.removeAllViews()
        LinearScope(linear).add<View>()
            .width(FILL)
            .height(if (Utils.isRoundScreen) 0.16f.vh else 0)
        if (Utils.isRoundScreen) {
            linear.paddingHorizontal(0.1f.vh)
        }
        LongPressMenuOrderManager.sortLabels(items.keys).forEach { label ->
            items[label]?.let { linear.addView(it) }
        }
        if (Utils.isRoundScreen) {
            LinearScope(linear).add<View>()
                .width(FILL)
                .height(0.16f.vh)
        }
    }

    val copyText = msg?.elements
        ?.filter { it.elementType == ElementType.TEXT }
        ?.mapNotNull { it.textElement?.content }
        ?.joinToString("")
        ?.takeIf { it.isNotEmpty() }
    val forwardable = msg?.let { isForwardableMsgRecord(it) } == true
    val nativeForwardLabel = listOf("分享", "转发").firstOrNull { items.containsKey(it) }
    if (forwardable && nativeForwardLabel != null) {
        items[nativeForwardLabel]?.setOnClickListener {
            linear.forwardMsgRecord(msg!!)
            dismiss()
        }
    }
    if (forwardable && nativeForwardLabel == null) {
        items["转发"] = createNativeMenuItem(linear, "转发") {
            linear.forwardMsgRecord(msg!!)
            dismiss()
        }
    }
    if (!forwardable && copyText != null) {
        items["转发"] = createNativeMenuItem(linear, "转发") {
            linear.forwardText(copyText)
            dismiss()
        }
    }
    if (copyText != null && fm != null) {
        items["自由复制"] = createNativeMenuItem(linear, "自由复制") {
            runCatching {
                PartialCopyFragment(copyText).show(fm, "qqpro_partial_copy")
            }.onFailure {
                Utils.log("partial copy open failed: $it")
            }
            dismiss()
        }
    }
    renderItems()
    if (msg != null && CurrentContact.isGroup) {
        CurrentGroupMembers.get(SelfContact.peerUid) {
            if (it.role == MemberRole.OWNER || it.role == MemberRole.ADMIN) {
                linear.post {
                    items["撤回"] = createNativeMenuItem(linear, "撤回") {
                        KernelServiceUtil.c()?.recallMsg(CurrentContact, msg.msgId, null)
                    }
                    renderItems()
                }
            }
        }
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
        val msg = runCatching {
            val field = this.b.javaClass.getDeclaredField("b")
            field.isAccessible = true
            val cell = field.get(this.b) as WatchAIOGroupWidgetItemCell<*, *>
            cell.f()!!.d
        }.getOrNull()
        val fm = runCatching { parentFragmentManager }.getOrNull()
        return super.onCreateView(inflater, container, savedInstanceState).apply {
            this.asGroup().getChildAt(0).asGroup().let { group ->
                process(group, msg, fm) { dismiss() }
            }
        }
    }
}
