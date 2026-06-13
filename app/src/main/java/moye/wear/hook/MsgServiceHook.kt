package moye.wear.hook

import com.tencent.qqnt.kernel.nativeinterface.Contact
import com.tencent.qqnt.kernel.nativeinterface.IOperateCallback
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.msg.api.impl.MsgServiceImpl
import momoi.anno.mixin.Mixin
import moye.wear.span.ExtraSpanHelper
import moye.wearqq.IMEOperation

/**
 * Send path for the new span input UX.
 *
 * Replaces the base mod's sendMsg (which blind-prepends @-elements positionally-unaware).
 * We instead let [ExtraSpanHelper.parseTextElements] expand the placeholder chars already
 * present in the text in-place, append any leftover carried images, then call the pristine
 * Tencent send (`sendMsg_old`) — NOT super.sendMsg, which would re-run the old processing.
 */
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
