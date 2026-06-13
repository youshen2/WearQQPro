package momoi.mod.qqpro.hook.aio_cell

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.tencent.qqnt.kernel.nativeinterface.ReplyElement
import com.tencent.watch.aio_impl.ui.widget.AIOCellGroupWidget
import momoi.mod.qqpro.hook.action.CurrentMsgList
import momoi.mod.qqpro.hook.view.smoothScrollToStart
import momoi.mod.qqpro.util.Utils

class ReplyClick(
    val widget: AIOCellGroupWidget,
    val reply: ReplyElement
) : View.OnClickListener {
    private var finding = false

    override fun onClick(v: View?) {
        val rv = widget.parent as RecyclerView
        if (finding) {
            return
        }
        finding = true
        CurrentMsgList.findMsg(
            seq = reply.replayMsgSeq,
            result = { item ->
                finding = false
                if (item != null) {
                    rv.smoothScrollToStart(CurrentMsgList.getMsgIndex(item))
                }
                else {
                    Utils.toast(rv.context, "无法定位消息")
                }
            }
        )
    }
}