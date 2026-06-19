package moye.wear.hook

import com.huanli233.qplus.utils.TextUtilKt
import com.tencent.qqnt.kernel.nativeinterface.Contact
import com.tencent.qqnt.kernel.nativeinterface.IOperateCallback
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.msg.api.impl.MsgServiceImpl
import com.tencent.qqnt.msg.api.impl.MsgUtilApiImpl
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.enums.ChatType
import momoi.mod.qqpro.util.Utils
import moye.wear.span.ExtraSpanHelper
import moye.wearqq.AtElementArg
import moye.wearqq.IMEOperation
import moye.wearqq.ReplyElementArg

private const val REPLY_WITH_AT = "reply_with_at"

@Mixin
class MsgServiceHook : MsgServiceImpl() {

    override fun sendMsg(
        contact: Contact,
        msgId: Long,
        elements: ArrayList<MsgElement>,
        callback: IOperateCallback?
    ) {
        ExtraSpanHelper.parseTextElements(elements)
        IMEOperation.INSTANCE.getExtra().forEach {
            when (it) {
                is AtElementArg -> addAt(elements, it)
                is ReplyElementArg -> addReply(contact, elements, it)
            }
        }
        IMEOperation.extraMsg.forEach { elements.add(elements.size, it) }
        IMEOperation.INSTANCE.clearExtra()
        IMEOperation.extraMsg.clear()
        super.sendMsg_old(contact, msgId, elements, callback)
    }

    private fun addAt(elements: ArrayList<MsgElement>, at: AtElementArg) {
        elements.add(
            elements.size,
            MsgUtilApiImpl.instance.createAtTextElement("@${TextUtilKt.b64Decode(at.atNickname)} ", at.atUid, 2)
        )
    }

    private fun addReply(contact: Contact, elements: ArrayList<MsgElement>, reply: ReplyElementArg) {
        val replyElement = MsgUtilApiImpl.instance.createReplyElement(reply.replayMsgId)
        replyElement.replyElement?.senderUid = reply.senderUidStr.toLongOrNull() ?: 0L
        elements.add(0, replyElement)
        if (contact.chatType == ChatType.GROUP && replyWithAtEnabled()) {
            elements.add(
                1,
                MsgUtilApiImpl.instance.createAtTextElement("@${reply.senderUidStr} ", reply.senderUidStr, 2)
            )
        }
    }

    private fun replyWithAtEnabled(): Boolean {
        return Utils.application.getSharedPreferences("wearqq", 0).getBoolean(REPLY_WITH_AT, false)
    }
}
