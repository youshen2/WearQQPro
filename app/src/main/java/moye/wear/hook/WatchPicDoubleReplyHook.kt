package moye.wear.hook

import android.content.Context
import android.view.`View$OnLongClickListener`
import com.tencent.qphone.base.util.BaseApplication
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.tencent.watch.aio_impl.data.WatchAIOMsgItem
import com.tencent.watch.aio_impl.ui.cell.mix.WatchMixMsgItem
import com.tencent.watch.aio_impl.ui.cell.pic.WatchPicMsgItem
import com.tencent.watch.aio_impl.ui.widget.AIOCellGroupWidget
import com.tencent.watch.aio_impl.ui.widget.DoubleClickListener
import momoi.anno.mixin.StaticHook
import moye.wearqq.IMEOperation
import moye.wearqq.MsgOperation
import moye.wearqq.ReplyElementArg

@StaticHook(WatchPicElementExtKt::class)
fun g(
    widget: AIOCellGroupWidget,
    data: WatchAIOMsgItem,
    longClickListener: `View$OnLongClickListener`,
    doubleClickListener: DoubleClickListener?,
    mask: Int
) {
    val context = BaseApplication.context as? Context
    val enableDoubleReply = context
        ?.getSharedPreferences("wearqq", 0)
        ?.getBoolean("double_reply", false) == true

    val listener = if (enableDoubleReply && (data is WatchPicMsgItem || data is WatchMixMsgItem)) {
        DoubleClickListener {
            openReply(data.d)
        }
    } else {
        doubleClickListener
    }

    WatchPicElementExtKt.f(widget, data, longClickListener, listener)
}

private fun openReply(msg: MsgRecord) {
    val senderName = msg.sendMemberName?.takeUnless { it.isEmpty() } ?: msg.sendNickName
    IMEOperation.INSTANCE.openIMEWithExtra(
        ReplyElementArg(
            msg.msgId,
            msg.senderUid,
            MsgOperation.getSummary(msg),
            senderName,
            ""
        )
    )
}
