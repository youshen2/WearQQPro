package momoi.mod.qqpro.hook.action

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tencent.aio.api.factory.IAIOFactory
import com.tencent.aio.api.list.IListUIOperationApi
import com.tencent.aio.base.chat.ChatPie
import com.tencent.aio.base.mvi.part.MsgListUiState
import com.tencent.aio.main.fragment.ChatFragment
import com.tencent.aio.part.root.panel.content.firstLevel.msglist.mvx.intent.MsgListDataIntent
import com.tencent.watch.aio_impl.coreImpl.vb.WatchAIOListVB
import com.tencent.watch.aio_impl.data.WatchAIOMsgItem
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.lib.Observable
import momoi.mod.qqpro.util.ThreadManager
import momoi.mod.qqpro.util.Utils
import java.util.LinkedList

object CurrentMsgList {
    lateinit var vb: WatchAIOListVB
        private set
    var msgList = Observable(mutableListOf<WatchAIOMsgItem>())
        private set

    private var indexCacheList: List<WatchAIOMsgItem>? = null
    private var indexCache: java.util.IdentityHashMap<WatchAIOMsgItem, Int>? = null

    fun getMsgIndex(msg: WatchAIOMsgItem): Int {
        val current = msgList.value
        if (indexCacheList !== current || indexCache == null) {
            val map = java.util.IdentityHashMap<WatchAIOMsgItem, Int>(current.size)
            current.forEachIndexed { i, item -> map[item] = i }
            indexCache = map
            indexCacheList = current
        }
        return indexCache!![msg] ?: -1
    }

    private var isLoadingMsg = false
    private fun loadMoreMsg() {
        if (!isLoadingMsg) {
            msgList.observeOnce {
                isLoadingMsg = false
            }
            isLoadingMsg = true
            Utils.log("Load more msg. currentSize: ${msgList.value.size}")
            vb.L(MsgListDataIntent.LoadTopPage("WatchAIOListVB"))
        }
    }

    /**
     * Scroll target is [count] messages above [current]. Pages in older messages until enough
     * history is loaded, then invokes [callback] with the resulting list position.
     *
     * [onProgress] is called with a 0..100 percentage after each page so the caller can show a
     * loading indicator. [onFail] fires (on the UI thread) if a page load times out or the top of
     * history is reached before the target — so the UI can show a toast and reset instead of
     * hanging silently.
     */
    fun upwardMsg(
        current: Int,
        count: Int,
        onProgress: (Int) -> Unit = {},
        onFail: () -> Unit = {},
        callback: (Int) -> Unit
    ) {
        val target = msgList.value.size - 1 - current + count
        upwardMsgInternal(target, msgList.value.size, onProgress, onFail, callback)
    }

    private fun upwardMsgInternal(
        target: Int,
        startSize: Int,
        onProgress: (Int) -> Unit,
        onFail: () -> Unit,
        callback: (Int) -> Unit
    ) {
        if (msgList.value.size >= target) {
            callback(msgList.value.size - target - 1)
            return
        }
        val before = msgList.value.size
        if (target > startSize) {
            val pct = ((before - startSize) * 100 / (target - startSize)).coerceIn(0, 99)
            onProgress(pct)
        }
        var settled = false
        msgList.observeOnce {
            if (settled) return@observeOnce
            settled = true
            ThreadManager.runOnUiThread({
                if (msgList.value.size <= before) {
                    // List stopped growing -> reached the top of history before the target.
                    Utils.log("upwardMsg: reached top of history before target=$target, size=${msgList.value.size}")
                    onFail()
                } else {
                    upwardMsgInternal(target, startSize, onProgress, onFail, callback)
                }
            })
        }
        ThreadManager.runOnUiThread({
            if (settled) return@runOnUiThread
            settled = true
            Utils.log("upwardMsg: timed out waiting for more msgs, target=$target size=${msgList.value.size}")
            onFail()
        }, 5000L)
        isLoadingMsg = false // clear any stuck guard from a previously interrupted load
        loadMoreMsg()
    }

    /**
     * 向上分页加载历史消息，直到到达历史顶部（列表不再增长），随后在 UI 线程调用 [onDone]。
     * 每加载一页后会通过 [onProgress] 回传当前消息总数。聊天记录搜索需要把全部历史加载进内存，故使用此方法。
     *
     * [shouldContinue] 会在每次分页前以及每个回调前检查——传入生命周期判断（如 `{ isAdded }`），
     * 这样在搜索被关闭/取消后能及时停止加载链，而不会在后台把整段历史拉完并触发已失效的回调。
     *
     * 加载前会先清掉 [isLoadingMsg]：上一次被中断的加载（聊天中途关闭）可能把该守卫卡在 `true`，
     * 否则 [loadMoreMsg] 会静默无效化，导致这里永远无法推进。
     */
    fun loadAll(
        onProgress: (Int) -> Unit = {},
        shouldContinue: () -> Boolean = { true },
        onDone: () -> Unit
    ) {
        if (!shouldContinue()) {
            Utils.log("loadAll: cancelled before start")
            return
        }
        val before = msgList.value.size
        var settled = false
        msgList.observeOnce {
            if (settled) return@observeOnce
            settled = true
            ThreadManager.runOnUiThread({
                if (!shouldContinue()) {
                    Utils.log("loadAll: cancelled, stopping")
                    return@runOnUiThread
                }
                if (msgList.value.size <= before) {
                    Utils.log("loadAll: reached top of history, total=${msgList.value.size}")
                    onDone()
                } else {
                    onProgress(msgList.value.size)
                    loadAll(onProgress, shouldContinue, onDone)
                }
            })
        }
        ThreadManager.runOnUiThread({
            if (settled) return@runOnUiThread
            settled = true
            Utils.log("loadAll: timed out waiting for more msgs, total=${msgList.value.size}")
            if (shouldContinue()) onDone()
        }, 5000L)
        isLoadingMsg = false
        loadMoreMsg()
    }

    fun findMsg(
        seq: Long,
        result: (WatchAIOMsgItem?) -> Unit,
        repeatCount: Int = 1000
    ) {
        val msg = msgList.value.find { it.d.msgSeq == seq }
        if (msg != null) {
            result(msg)
            return
        }
        if (repeatCount <= 0) {
            Utils.log("findMsg: give up (repeat exhausted) seq=$seq")
            result(null)
            return
        }
        // 加载更老的消息后重试。两种停止条件，避免无限挂起：
        // 1) 一次加载后列表不再增长 -> 已到达历史顶部；
        // 2) 超时仍无更新到达 -> 加载卡住 / 没有可加载内容。
        val sizeBefore = msgList.value.size
        var settled = false
        msgList.observeOnce {
            if (settled) return@observeOnce
            settled = true
            if (msgList.value.size <= sizeBefore) {
                // 到达历史顶部仍未找到目标。
                Utils.log("findMsg: reached top of history, seq=$seq not found")
                result(null)
            } else {
                findMsg(seq, result, repeatCount - 1)
            }
        }
        ThreadManager.runOnUiThread({
            if (settled) return@runOnUiThread
            settled = true
            Utils.log("findMsg: timed out waiting for more msgs, seq=$seq")
            result(null)
        }, 3000L)
        loadMoreMsg()
    }

    @Mixin
    class Hook : WatchAIOListVB() {
        @Suppress("UNCHECKED_CAST")
        override fun n(state: MsgListUiState, uiHelper: IListUIOperationApi) {
            vb = this
            val msg = msgList.value
            val list = state as LinkedList<WatchAIOMsgItem>
            // 预建 msgId -> 下标 映射，替换循环内的 O(n) indexOfLast，把整体合并从 O(n×m) 降到 O(n+m)。
            // 原始项一律不会因后续插入而移位（新项都插在 insertIndex == 原始末尾之后），
            // 故映射在循环内对原始项始终有效；插入新项时同步写入映射以保持 indexOfLast 语义。
            val indexByMsgId = HashMap<Long, Int>(msg.size)
            msg.forEachIndexed { i, item -> indexByMsgId[item.d.msgId] = i }
            var insertIndex = -1
            while (true) {
                val last = list.pollLast()
                if (last == null) {
                    list.addAll(msg)
                    break
                }
                val index = indexByMsgId[last.d.msgId] ?: -1
                if (index == -1) {
                    if (insertIndex == -1) {
                        msg.add(last)
                        insertIndex = msg.lastIndex
                    } else {
                        msg.add(insertIndex, last)
                    }
                    indexByMsgId[last.d.msgId] = insertIndex
                } else {
                    msg[index] = last
                    //if (insertIndex == -1) {
                    //    insertIndex = 0
                    //}
                    //for (i in insertIndex until msg.size) {
                    //    msg[i].checkAndSetSameSender(msg.getOrNull(i-1))
                    //}
                    list.addAll(msg.subList(index, msg.size))
                    break
                }
            }
            msgList.update(list.toMutableList())
            super.n(list as MsgListUiState, uiHelper)
        }
    }

    @Mixin
    class Clear(p0: IAIOFactory) : ChatPie(p0) {
        override fun a(
            fragment: ChatFragment,
            inflater: LayoutInflater,
            container: ViewGroup,
            isPreload: Boolean
        ): View {
            msgList = Observable(ArrayList())
            return super.a(fragment, inflater, container, isPreload)
        }
    }

}