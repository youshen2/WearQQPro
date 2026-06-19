package momoi.mod.qqpro.hook

import androidx.core.text.buildSpannedString
import androidx.core.text.color
import com.tencent.qqnt.chats.core.adapter.holder.BaseChatViewHolder
import com.tencent.qqnt.chats.core.adapter.itemdata.BaseChatItem
import com.tencent.qqnt.chats.core.adapter.itemdata.RecentContactChatItem
import com.tencent.qqnt.kernel.nativeinterface.RecentContactInfo
import com.tencent.qqnt.watch.chat.list.WatchRecentContactHolder
import com.tencent.qqnt.watch.chat.list.WatchRecentItemBuilder
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Colors
import momoi.mod.qqpro.lib.clickable
import momoi.mod.qqpro.util.Utils

const val TOKEN = "\u200B\u200B\u200B\u200B\u200B"
val atList = mutableListOf<String>()

val notifyTagMap = mutableMapOf<String, String>()

private val eventTypeLabels = mapOf(
    1000 to "[有人@我]",
    1001 to "[有人@我]",
    1002 to "[有人回复我]",
    2000 to "[有人@全体成员]",
    2001 to "[有新文件]",
    2003 to "[有新作业]",
    2004 to "[有新公告]",
)

private fun resolveNotifyLabel(info: RecentContactInfo): String? {
    val events = info.listOfSpecificEventTypeInfosInMsgBox
    if (events != null) {
        var bestSeq = Long.MIN_VALUE
        var bestLabel: String? = null
        for (e in events) {
            val last = e.msgInfos?.lastOrNull()
            val label = last?.highlightDigest?.takeIf { it.isNotEmpty() }
                ?: eventTypeLabels[e.eventTypeInMsgBox]
            val seq = last?.msgSeq ?: 0L
            if (label != null && seq >= bestSeq) {
                bestSeq = seq
                bestLabel = label
            }
        }
        if (bestLabel != null) return bestLabel
    }
    if (info.notifiedType != 0 && info.atType == 6) return "[有人@我]"
    return null
}

@Mixin
abstract class 有人at我 : WatchRecentItemBuilder() {
    override fun m(
        p0: BaseChatViewHolder<BaseChatItem?>,
        p1: RecentContactChatItem,
        p2: List<Any?>
    ) {
        super.m(p0, p1, p2)
        val tv = (p0 as WatchRecentContactHolder).b.c
        p0.itemView.clickable {
            m(p0, p1, p2)
            tv.text = tv.text.toString().removeBefore(TOKEN)
        }

        val unread = p1.i.b
        val uid = p1.a.peerUid
        val label = if (unread > 0L) resolveNotifyLabel(p1.a) else null

        if (unread > 0L) {
            val events = p1.a.listOfSpecificEventTypeInfosInMsgBox?.joinToString(",") { e ->
                "${e.eventTypeInMsgBox}:${e.msgInfos?.lastOrNull()?.highlightDigest}"
            }
            Utils.log(
                "notify-tag ${p1.a.peerName} unread=$unread atType=${p1.a.atType} " +
                    "notifiedType=${p1.a.notifiedType} events=[$events] -> $label"
            )
        }

        if (label != null) {
            notifyTagMap[uid] = label
        } else {
            notifyTagMap.remove(uid)
            tv.text = tv.text.toString().removeBefore(TOKEN)
        }

        notifyTagMap[uid]?.let { tag ->
            tv.text = buildSpannedString {
                color(Colors.atMe) {
                    append(tag)
                }
                append(TOKEN)
                append(tv.text.toString().removeBefore(TOKEN))
            }
        }
    }
}

private fun String.removeBefore(token: String): String {
    val index = indexOf(token)
    return if (index != -1) substring(index + token.length) else this
}
