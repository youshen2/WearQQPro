package moye.wear.hook

import com.tencent.qqnt.kernel.nativeinterface.Contact
import com.tencent.qqnt.kernel.nativeinterface.IOperateCallback
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.msg.api.impl.MsgServiceImpl
import momoi.anno.mixin.Mixin
import moye.wear.span.ExtraSpanHelper

@Mixin
class MsgServiceHook : MsgServiceImpl() {

    override fun sendMsg(
        contact: Contact,
        msgId: Long,
        elements: ArrayList<MsgElement>,
        callback: IOperateCallback?
    ) {
        ExtraSpanHelper.parseTextElements(elements)
        // 回复等未内联到输入框的附加元素仍交给基座 sendMsg 处理，避免丢失 ReplyElement。
        super.sendMsg(contact, msgId, elements, callback)
    }
}
