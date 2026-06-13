package momoi.mod.qqpro.hook.action

import android.text.SpannableStringBuilder
import com.tencent.qqnt.chats.core.adapter.holder.BaseChatViewHolder
import com.tencent.qqnt.chats.core.adapter.itemdata.BaseChatItem
import com.tencent.qqnt.chats.core.adapter.itemdata.RecentContactChatItem
import com.tencent.qqnt.kernel.nativeinterface.RecentContactInfo
import com.tencent.qqnt.watch.chat.list.WatchRecentContactHolder
import com.tencent.qqnt.watch.chat.list.WatchRecentItemBuilder
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.util.Utils

object RecentContacts {
    val map = mutableMapOf<String, Data>()
    fun get(peerUin: String?) = map[peerUin]
    class Data(
        val raw: RecentContactInfo,
        val unreadCntCached: Int,
    ) {
        val atType get() = raw.atType
    }

    private const val FACE_LEAD = '\u0014'

    private fun isFaceGarbage(c: Char): Boolean {
        val code = c.code
        return (code in 0x01..0x1F && code != 0x09 && code != 0x0A && code != 0x0D) ||
            code in 0xE000..0xF8FF ||
            code == 0xFFFD
    }

    fun sanitizeRecentSummary(cs: CharSequence): CharSequence {
        if (cs.isEmpty()) return cs
        val ranges = ArrayList<IntArray>()
        var i = 0
        while (i < cs.length) {
            val c = cs[i]
            when {
                c == FACE_LEAD -> {
                    ranges.add(intArrayOf(i, minOf(cs.length, i + 2)))
                    i += 2
                }
                isFaceGarbage(c) -> {
                    val start = i
                    i++
                    while (i < cs.length && isFaceGarbage(cs[i])) i++
                    ranges.add(intArrayOf(start, i))
                }
                else -> i++
            }
        }
        if (ranges.isEmpty()) return cs
        val out = SpannableStringBuilder(cs)
        for (k in ranges.indices.reversed()) {
            val r = ranges[k]
            out.replace(r[0], r[1], "[表情]")
        }
        return out
    }

    fun sanitizeItem(item: RecentContactChatItem) {
        item.h?.let { info -> info.a?.let { info.a = sanitizeRecentSummary(it) } }
    }

    @Mixin
    abstract class Hook : WatchRecentItemBuilder() {
        override fun t(item: RecentContactChatItem, holder: WatchRecentContactHolder) {
            Utils.log("load recent contact: ${item.a.peerName}, unreadCnt: ${item.a.unreadCnt}, chatCnt: ${item.a.unreadChatCnt}, peerUid: ${item.a.peerUid}")
            map[item.a.peerUid] = Data(
                item.a,
                item.a.unreadCnt.toInt()
            )
            sanitizeItem(item)
            super.t(item, holder)
        }

        override fun q(item: RecentContactChatItem, holder: WatchRecentContactHolder) {
            sanitizeItem(item)
            super.q(item, holder)
        }

        override fun m(
            holder: BaseChatViewHolder<BaseChatItem>,
            item: RecentContactChatItem,
            payload: List<Any?>,
        ) {
            sanitizeItem(item)
            super.m(holder, item, payload)
        }
    }
}