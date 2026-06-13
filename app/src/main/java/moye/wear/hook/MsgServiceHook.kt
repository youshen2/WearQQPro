package moye.wear.hook

import com.tencent.qqnt.kernel.nativeinterface.Contact
import com.tencent.qqnt.kernel.nativeinterface.IOperateCallback
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.msg.api.impl.MsgServiceImpl
import momoi.anno.mixin.Mixin
import moye.wear.span.ExtraSpanHelper
import moye.wearqq.IMEOperation

@Mixin
class MsgServiceHook : MsgServiceImpl() {

    override fun sendMsg(
        contact: Contact,
        msgId: Long,
        elements: ArrayList<MsgElement>,
        callback: IOperateCallback?
    ) {
        ExtraSpanHelper.parseTextElements(elements)
        elements.addAll(IMEOperation.extraMsg)
        IMEOperation.INSTANCE.clearExtra()
        IMEOperation.extraMsg.clear()
        super.sendMsg_old(contact, msgId, elements, callback)
    }
}
