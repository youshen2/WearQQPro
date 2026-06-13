package moye.wear.hook

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.tencent.watch.aio_impl.data.WatchAIOMsgItem
import com.tencent.watch.aio_impl.ui.frames.SettingFrame
import loadPicUrl
import momoi.mod.qqpro.enums.ElementType
import momoi.mod.qqpro.msg.getImageUrl
import momoi.mod.qqpro.hook.action.CurrentMsgList
import momoi.mod.qqpro.hook.view.MyDialogFragment
import momoi.mod.qqpro.hook.view.scrollToStartInstant
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.clickable
import momoi.mod.qqpro.lib.content
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.gravity
import momoi.mod.qqpro.lib.linearLayout
import momoi.mod.qqpro.lib.margin
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize
import momoi.mod.qqpro.lib.vertical
import momoi.mod.qqpro.lib.width
import momoi.mod.qqpro.util.Utils
import moye.wear.lib.SwipeBackLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ACCENT = 0xFF_4FC3F7.toInt()
private val BG = 0xF0_121212.toInt()
private const val ICON_SEARCH = 0x7e0805ca // R.drawable.icon_search

private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

/** 归类为“其他”的元素类型（文件 / 语音 / 卡片 / 转发 / 位置 …）。 */
private val OTHER_TYPES = setOf(
    ElementType.FILE,
    ElementType.PTT,
    ElementType.ARK,
    ElementType.STRUCT_LONG_MSG,
    ElementType.MARKDOWN,
    ElementType.MULTI_FORWARD,
    ElementType.WALLET,
    ElementType.LIVE_GIFT,
    ElementType.SHARE_LOCATION,
    ElementType.CALENDAR,
)

private const val ENTRY_LABEL = "搜索聊天记录"

private enum class SearchType(val label: String) {
    TEXT("文本"),
    MEDIA("图片 / 视频"),
    OTHER("其他文件"),
    DATE("按日期"),
}

/**
 * 在聊天最右侧的设置页（[SettingFrame]）插入一条“搜索聊天记录”入口，群聊与单聊都会显示
 * （与 群成员/群设置/退出群 或 消息设置/删除好友 并列）。点击后打开 [ChatSearchFragment]。
 * 该行复用原生的 `setting_item` 布局，使其外观与其它条目一致。由 SettingFrame Hook 的
 * onViewCreated 调用。
 */
fun addChatSearchEntry(fragment: SettingFrame) {
    runCatching {
        val scroll = fragment.i ?: return
        val container = scroll.getChildAt(0) as? LinearLayout ?: return
        val ctx = fragment.requireContext()
        val res = ctx.resources
        val pkg = ctx.packageName
        val descId = res.getIdentifier("desc", "id", pkg)
        // onViewCreated 可能在同一 container 上再次触发（例如返回该页面）——
        // 通过标签文本判断已存在则提前返回，避免每次都重复追加入口行。
        for (i in 0 until container.childCount) {
            val desc = container.getChildAt(i).findViewById<TextView>(descId)
            if (desc?.text?.toString() == ENTRY_LABEL) return
        }
        val layoutId = res.getIdentifier("setting_item", "layout", pkg)
        if (layoutId == 0) {
            Utils.log("ChatSearch: setting_item layout not found")
            return
        }
        val row = LayoutInflater.from(ctx).inflate(layoutId, container, false)
        row.findViewById<ImageView>(res.getIdentifier("icon", "id", pkg))?.setImageResource(ICON_SEARCH)
        row.findViewById<TextView>(descId)?.text = ENTRY_LABEL
        row.setOnClickListener {
            runCatching {
                ChatSearchFragment().show(fragment.childFragmentManager, "chat_search")
            }.onFailure { Utils.log("ChatSearch: open failed: $it") }
        }
        // 头部视图依次是 avatar(0)、nick(1)、peerId(2)、info(3)；插入到菜单项之前。
        container.addView(row, minOf(4, container.childCount))
        Utils.log("ChatSearch: entry added")
    }.onFailure { Utils.log("ChatSearch: addEntry failed: $it") }
}

/**
 * 手表端的全屏聊天记录搜索。流程：
 *  1. 选择类型（文本 / 图片视频 / 其他 / 按日期）；
 *  2. 文本类型可通过软键盘输入可选关键词；按日期会先列出有消息的日期供选择；
 *  3. 把整段聊天历史分页加载进内存（[CurrentMsgList.loadAll]）并过滤；
 *  4. 显示结果列表——点击某条结果即关闭搜索并把聊天滚动到该消息（与回复跳转/首条未读使用同一套跳转设施）。
 */
class ChatSearchFragment : MyDialogFragment() {

    private lateinit var root: LinearLayout

    /** 对话框销毁时置为 false，使进行中的 loadAll 停止而不泄漏。 */
    private var active = true

    override fun onDestroyView() {
        active = false
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = LinearLayout(inflater.context).vertical()
        root.layoutParams = ViewGroup.LayoutParams(FILL, FILL)
        root.setBackgroundColor(BG)
        showMenu()
        return SwipeBackLayout(inflater.context).apply {
            addView(root, FILL, FILL)
            onSwipeBack = { dismiss() }
        }
    }

    // ---- 各级界面 -------------------------------------------------------------

    private fun showMenu() {
        root.removeAllViews()
        root.gravity = Gravity.NO_GRAVITY
        scrollColumn {
            title("搜索聊天记录")
            for (type in SearchType.values()) {
                button(type.label, 0xFF_2A2A2A.toInt(), 0xFF_FFFFFF.toInt()) {
                    when (type) {
                        SearchType.TEXT -> showTextInput()
                        SearchType.DATE -> startDateFlow()
                        else -> startSearch(type, null, null)
                    }
                }
            }
            button("取消", 0xFF_1A1A1A.toInt(), 0xFF_999999.toInt()) { dismiss() }
        }
    }

    private fun showTextInput() {
        root.removeAllViews()
        root.gravity = Gravity.NO_GRAVITY
        val ctx = requireContext()
        lateinit var input: EditText
        scrollColumn {
            title("搜索文本")
            input = add<EditText>()
                .textSize(14f)
                .textColor(0xFF_FFFFFF)
                .width(FILL)
                .padding(10.dp)
                .apply {
                    hint = "关键词(可留空)"
                    setHintTextColor(0xFF_777777.toInt())
                    setSingleLine()
                    background = GradientDrawable().apply {
                        setColor(0xFF_222222.toInt())
                        cornerRadius = 12.dp.toFloat()
                    }
                }
                .margin(bottom = 10.dp)
            button("搜索", ACCENT, 0xFF_000000.toInt()) {
                hideKeyboard(input)
                startSearch(SearchType.TEXT, input.text?.toString()?.trim().orEmpty(), null)
            }
            button("返回", 0xFF_1A1A1A.toInt(), 0xFF_999999.toInt()) {
                hideKeyboard(input)
                showMenu()
            }
        }
        input.requestFocus()
        input.post {
            (ctx.getSystemService(InputMethodManager::class.java))
                ?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun startDateFlow() {
        showLoading()
        CurrentMsgList.loadAll(onProgress = ::updateLoading, shouldContinue = { isAdded && active }) {
            if (!isAdded || !active) return@loadAll // 加载途中对话框已关闭
            val days = CurrentMsgList.msgList.value
                .asSequence()
                .filter { it.d.elements.isNotEmpty() }
                .map { dayFmt.format(Date(it.d.msgTime * 1000)) }
                .distinct()
                .sortedDescending()
                .toList()
            showDateList(days)
        }
    }

    private fun showDateList(days: List<String>) {
        root.removeAllViews()
        root.gravity = Gravity.NO_GRAVITY
        if (days.isEmpty()) {
            showEmpty("没有可选日期")
            return
        }
        root.content {
            title("选择日期")
            val list = add<androidx.recyclerview.widget.RecyclerView>().linearLayout()
            (list.layoutParams as LinearLayout.LayoutParams).apply {
                width = FILL; height = 0; weight = 1f
            }
            list.content(
                data = days,
                factory = { simpleRow() },
                update = { day ->
                    (getChildAt(0) as TextView).text = day
                    clickable { startSearch(SearchType.DATE, null, day) }
                }
            )
        }
    }

    private fun startSearch(type: SearchType, keyword: String?, day: String?) {
        showLoading()
        CurrentMsgList.loadAll(onProgress = ::updateLoading, shouldContinue = { isAdded && active }) {
            if (!isAdded || !active) return@loadAll // 加载途中对话框已关闭
            val hits = CurrentMsgList.msgList.value.filter { matches(it, type, keyword, day) }
            Utils.log("ChatSearch: type=$type keyword=$keyword day=$day hits=${hits.size}")
            showResults(hits, withPreview = type == SearchType.MEDIA)
        }
    }

    private fun showResults(hits: List<WatchAIOMsgItem>, withPreview: Boolean) {
        root.removeAllViews()
        root.gravity = Gravity.NO_GRAVITY
        if (hits.isEmpty()) {
            showEmpty("没有匹配的消息")
            return
        }
        // 最新的排在最前。
        val data = hits.reversed()
        root.content {
            title("搜索结果 (${data.size})")
            val list = add<androidx.recyclerview.widget.RecyclerView>().linearLayout()
            (list.layoutParams as LinearLayout.LayoutParams).apply {
                width = FILL; height = 0; weight = 1f
            }
            list.content(
                data = data,
                factory = { resultRow(withPreview) },
                update = { item ->
                    if (withPreview) {
                        bindPreview(getChildAt(0) as ImageView, item.d)
                        val texts = getChildAt(1) as LinearLayout
                        // 混淆字段名：l = 显示昵称，k = 时间
                        (texts.getChildAt(0) as TextView).text = "${item.l}  ·  ${item.k}"
                        (texts.getChildAt(1) as TextView).text = buildSnippet(item.d)
                    } else {
                        // 混淆字段名：l = 显示昵称，k = 时间
                        (getChildAt(0) as TextView).text = "${item.l}  ·  ${item.k}"
                        (getChildAt(1) as TextView).text = buildSnippet(item.d)
                    }
                    clickable { jumpTo(item) }
                }
            )
        }
    }

    /** 把 [rec] 的首张图片缩略图加载到 [iv]（视频 / 无法解析时显示深色占位）。 */
    private fun bindPreview(iv: ImageView, rec: MsgRecord) {
        iv.setImageDrawable(null)
        iv.setBackgroundColor(0xFF_222222.toInt())
        val pic = rec.elements.firstOrNull { it.elementType == ElementType.PIC }?.picElement
        // getImageUrl() 在 originImageUrl 为 null 时会抛异常（Kotlin 非空断言），故加以保护。
        val url = pic?.let { runCatching { it.getImageUrl() }.getOrNull() }
        if (!url.isNullOrEmpty()) {
            runCatching { iv.loadPicUrl(url, pic.md5HexStr ?: url) }
                .onFailure { Utils.log("ChatSearch: preview load failed: $it") }
        } else {
            iv.contentDescription = "视频" // 视频或无可下载缩略图
        }
    }

    private fun showLoading() {
        root.removeAllViews()
        root.gravity = Gravity.CENTER
        root.content {
            add<TextView>()
                .text("加载聊天记录…")
                .textSize(14f)
                .textColor(0xFF_FFFFFF)
                .gravity(Gravity.CENTER)
                .apply { tag = "loading" }
        }
    }

    private fun updateLoading(count: Int) {
        if (!isAdded || !active) return
        (root.findViewWithTag<TextView>("loading"))?.text = "加载聊天记录… ($count)"
    }

    private fun showEmpty(msg: String) {
        root.removeAllViews()
        root.gravity = Gravity.NO_GRAVITY
        scrollColumn {
            title(msg)
            button("返回", 0xFF_2A2A2A.toInt(), 0xFF_FFFFFF.toInt()) { showMenu() }
        }
    }

    /** 一个垂直居中、可滚动的列（让菜单能适配方形屏幕）。 */
    private fun scrollColumn(block: momoi.mod.qqpro.lib.LinearScope.() -> Unit) {
        val ctx = requireContext()
        val sv = ScrollView(ctx)
        sv.isFillViewport = true
        sv.layoutParams = LinearLayout.LayoutParams(FILL, 0, 1f)
        val col = LinearLayout(ctx).vertical()
        col.gravity = Gravity.CENTER
        col.setPadding(16.dp, 8.dp, 16.dp, 16.dp)
        col.layoutParams = ViewGroup.LayoutParams(FILL, ViewGroup.LayoutParams.WRAP_CONTENT)
        sv.addView(col)
        root.addView(sv)
        col.content(block)
    }

    // ---- 动作 -------------------------------------------------------------

    private fun jumpTo(item: WatchAIOMsgItem) {
        // 在 dismiss() 把我们从界面分离前先抓住 decor view（之后 activity 会变为 null）。
        val decor = activity?.window?.decorView
        val rv = runCatching { CurrentMsgList.vb.H }.getOrNull()
        val index = CurrentMsgList.getMsgIndex(item)
        dismiss()
        runCatching {
            // 搜索入口位于最右侧的设置页；把聊天页（第 0 页）切回前台，
            // 与发送图片/视频时的行为相同（参见 WatchAIOPageReset / InputMethodFragmentHook）。
            if (decor != null) switchToChatPage(decor)
            // 瞬时跳转——smoothScrollToStart 逐项动画，距离很远时会非常慢。
            if (rv != null && index >= 0) rv.post { rv.scrollToStartInstant(index) }
        }.onFailure { Utils.log("ChatSearch: jump failed: $it") }
    }

    // ---- UI 辅助 ----------------------------------------------------------

    private fun momoi.mod.qqpro.lib.GroupScope.title(label: String) {
        add<TextView>()
            .text(label)
            .textSize(15f)
            .textColor(0xFF_FFFFFF)
            .gravity(Gravity.CENTER)
            .width(FILL)
            .padding(top = 14.dp, bottom = 12.dp)
    }

    private fun momoi.mod.qqpro.lib.GroupScope.button(
        label: String,
        bg: Int,
        fg: Int,
        onClick: () -> Unit
    ) {
        add<TextView>()
            .text(label)
            .textSize(14f)
            .textColor(fg)
            .gravity(Gravity.CENTER)
            .width(FILL)
            .padding(top = 12.dp, bottom = 12.dp)
            .apply {
                background = GradientDrawable().apply {
                    setColor(bg)
                    cornerRadius = 22.dp.toFloat()
                }
            }
            .margin(top = 6.dp)
            .clickable(onClick)
    }

    /** 单行可点击行（用于日期列表）。 */
    private fun simpleRow(): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).vertical()
        row.layoutParams = LinearLayout.LayoutParams(FILL, ViewGroup.LayoutParams.WRAP_CONTENT)
        row.setPadding(18.dp, 12.dp, 18.dp, 12.dp)
        val tv = TextView(ctx)
        tv.textSize = 14f
        tv.setTextColor(0xFF_FFFFFF.toInt())
        row.addView(tv)
        return row
    }

    /**
     * 结果行。无预览时：上下两行文本（发送者·时间、摘要）。
     * 带预览时：左侧缩略图 + 右侧一列两行文本
     * （子视图：[0]=ImageView，[1]=包含两个 TextView 的 LinearLayout）。
     */
    private fun resultRow(withPreview: Boolean): View {
        val ctx = requireContext()

        fun head() = TextView(ctx).apply {
            textSize = 11f; setTextColor(ACCENT); setSingleLine()
        }
        fun body() = TextView(ctx).apply {
            textSize = 14f; setTextColor(0xFF_EEEEEE.toInt()); maxLines = 2
        }

        if (!withPreview) {
            val row = LinearLayout(ctx).vertical()
            row.layoutParams = LinearLayout.LayoutParams(FILL, ViewGroup.LayoutParams.WRAP_CONTENT)
            row.setPadding(16.dp, 10.dp, 16.dp, 10.dp)
            row.addView(head())
            row.addView(body().apply {
                layoutParams = LinearLayout.LayoutParams(FILL, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = 3.dp }
            })
            return row
        }

        val row = LinearLayout(ctx)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.layoutParams = LinearLayout.LayoutParams(FILL, ViewGroup.LayoutParams.WRAP_CONTENT)
        row.setPadding(14.dp, 8.dp, 14.dp, 8.dp)

        val thumb = ImageView(ctx)
        thumb.layoutParams = LinearLayout.LayoutParams(48.dp, 48.dp)
        thumb.scaleType = ImageView.ScaleType.CENTER_CROP
        thumb.maxHeight = 48.dp // loadPicElement 要求 maxHeight != 0
        thumb.adjustViewBounds = false
        row.addView(thumb)

        val texts = LinearLayout(ctx).vertical()
        texts.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            .apply { marginStart = 12.dp }
        texts.addView(head())
        texts.addView(body().apply {
            layoutParams = LinearLayout.LayoutParams(FILL, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = 3.dp }
        })
        row.addView(texts)
        return row
    }

    private fun hideKeyboard(view: View) {
        runCatching {
            (requireContext().getSystemService(InputMethodManager::class.java))
                ?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}

// ---- 页面切换 ----------------------------------------------------------

/** 把 AIO 聊天页（聊天 ViewPager2 的第 0 页）切回前台。 */
private fun switchToChatPage(root: View) {
    val vp = findViewPager2(root) ?: run { Utils.log("ChatSearch: ViewPager2 not found"); return }
    // 内置（R8 混淆后）的 ViewPager2 只暴露了 setCurrentItem(int)。
    runCatching { vp.javaClass.getMethod("setCurrentItem", Int::class.java).invoke(vp, 0) }
        .onFailure { Utils.log("ChatSearch: switchToChatPage failed: $it") }
}

private fun findViewPager2(root: View): View? {
    if (root.javaClass.name.endsWith("ViewPager2")) return root
    if (root is ViewGroup) {
        for (i in 0 until root.childCount) findViewPager2(root.getChildAt(i))?.let { return it }
    }
    return null
}

// ---- 过滤 ---------------------------------------------------------------

private fun matches(
    item: WatchAIOMsgItem,
    type: SearchType,
    keyword: String?,
    day: String?
): Boolean {
    val rec = item.d
    val elements = rec.elements
    if (elements.isEmpty()) return false
    if (elements[0].elementType == ElementType.GREY_TIP) return false // 跳过撤回/拍一拍等小灰条
    return when (type) {
        SearchType.TEXT -> {
            val text = textOf(rec)
            if (text.isBlank()) false
            else if (keyword.isNullOrBlank()) true
            else text.contains(keyword, ignoreCase = true)
        }
        SearchType.MEDIA -> elements.any {
            it.elementType == ElementType.PIC || it.elementType == ElementType.VIDEO
        }
        SearchType.OTHER -> elements.any { it.elementType in OTHER_TYPES }
        SearchType.DATE -> day != null && dayFmt.format(Date(rec.msgTime * 1000)) == day
    }
}

/** 拼接所有 TEXT 元素的文本（用于关键词匹配）。 */
private fun textOf(rec: MsgRecord): String {
    val sb = StringBuilder()
    for (e in rec.elements) {
        if (e.elementType == ElementType.TEXT) sb.append(e.textElement?.content ?: "")
    }
    return sb.toString()
}

/**
 * 为结果行生成简短的人类可读摘要。不会修改 elements（不同于 MsgUtil.summary，
 * 后者会把 elements 置空——那会破坏 CurrentMsgList 中的实时消息）。
 */
private fun buildSnippet(rec: MsgRecord): String {
    val sb = StringBuilder()
    for (e in rec.elements) {
        when (e.elementType) {
            ElementType.TEXT -> sb.append(e.textElement?.content ?: "")
            ElementType.PIC -> sb.append("[图片]")
            ElementType.VIDEO -> sb.append("[视频]")
            ElementType.FILE -> sb.append("[文件]")
            ElementType.PTT -> sb.append("[语音]")
            ElementType.ARK -> sb.append("[卡片]")
            ElementType.MULTI_FORWARD -> sb.append("[聊天记录]")
            ElementType.MFACE, ElementType.FACE -> sb.append(e.marketFaceElement?.faceName ?: "[表情]")
            ElementType.SHARE_LOCATION -> sb.append("[位置]")
            ElementType.WALLET -> sb.append("[红包]")
            else -> {}
        }
    }
    val s = sb.toString().replace('\n', ' ').trim()
    return if (s.length > 60) s.take(60) + "…" else s
}
