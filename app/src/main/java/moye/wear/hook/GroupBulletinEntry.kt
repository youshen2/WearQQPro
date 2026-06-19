package moye.wear.hook

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.tencent.qqnt.kernel.nativeinterface.MemberRole
import com.tencent.watch.aio_impl.ui.frames.SettingFrame
import momoi.mod.qqpro.hook.action.CurrentGroupMembers
import momoi.mod.qqpro.hook.action.SelfContact
import momoi.mod.qqpro.hook.view.MyDialogFragment
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.LinearScope
import momoi.mod.qqpro.lib.clickable
import momoi.mod.qqpro.lib.content
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.height
import momoi.mod.qqpro.lib.linearLayout
import momoi.mod.qqpro.lib.margin
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.paddingHorizontal
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize
import momoi.mod.qqpro.lib.vh
import momoi.mod.qqpro.lib.vertical
import momoi.mod.qqpro.lib.width
import momoi.mod.qqpro.util.Utils
import momoi.mod.qqpro.util.runOnUi
import moye.wear.lib.SwipeBackLayout
import mqq.app.AppRuntime
import mqq.app.MobileQQ
import mqq.manager.TicketManager
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

private fun String.decodeHtmlEntities(): String {
    return replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace(Regex("&#x([0-9a-fA-F]+);")) {
            it.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: it.value
        }
        .replace(Regex("&#(\\d+);")) {
            it.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: it.value
        }
}

private const val GROUP_BULLETIN_LABEL = "群公告"
private const val GROUP_MEMBER_LABEL = "群成员"
private const val ICON_GROUP_BULLETIN = 0x7e080598
private const val GROUP_BULLETIN_LIST_URL = "https://web.qun.qq.com/cgi-bin/announce/list_announce"
private const val GROUP_BULLETIN_DELETE_URL = "https://web.qun.qq.com/cgi-bin/announce/del_feed"
private const val GROUP_BULLETIN_REMIND_URL = "https://qun.qq.com/cgi-bin/qunapp/announce_remindread"
private const val PAGE_SIZE = 20
private const val NATIVE_MENU_ITEM_LAYOUT = 2114715895
private const val NATIVE_MENU_SWITCH_ID = 2114519474
private const val NATIVE_MENU_TEXT_ID = 2114519756
private const val NATIVE_MENU_ICON_ID = 2114520149
private const val ICON_COPY = 2114454957
private const val ICON_REVOKE = 2114454918
private val BULLETIN_ACCENT = 0xFF_4FC3F7.toInt()
private val BULLETIN_BG = 0xF0_121212.toInt()
private val BULLETIN_TIME = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

data class Announcement(
    val id: String,
    val text: String,
    val author: String,
    val publishTime: Long,
    val picCount: Int
) {
    fun toArgs() = Bundle().apply {
        putString("id", id)
        putString("text", text)
        putString("author", author)
        putLong("publishTime", publishTime)
        putInt("picCount", picCount)
    }

    companion object {
        fun fromArgs(args: Bundle) = Announcement(
            id = args.getString("id").orEmpty(),
            text = args.getString("text").orEmpty(),
            author = args.getString("author").orEmpty(),
            publishTime = args.getLong("publishTime"),
            picCount = args.getInt("picCount")
        )
    }
}

fun addGroupBulletinEntry(fragment: SettingFrame) {
    runCatching {
        val args = fragment.arguments ?: return
        if (args.getInt("key_bundle_chat_type") != 2) return
        val scroll = fragment.i ?: return
        val container = scroll.getChildAt(0) as? LinearLayout ?: return
        val ctx = fragment.requireContext()
        val res = ctx.resources
        val pkg = ctx.packageName
        val descId = res.getIdentifier("desc", "id", pkg)
        val layoutId = res.getIdentifier("setting_item", "layout", pkg)
        if (descId == 0 || layoutId == 0) return
        for (i in 0 until container.childCount) {
            if (container.getChildAt(i).entryText(descId) == GROUP_BULLETIN_LABEL) return
        }
        val row = LayoutInflater.from(ctx).inflate(layoutId, container, false)
        row.findViewById<ImageView>(res.getIdentifier("icon", "id", pkg))?.setImageResource(ICON_GROUP_BULLETIN)
        row.findViewById<TextView>(descId)?.text = GROUP_BULLETIN_LABEL
        row.setOnClickListener {
            GroupBulletinFragment.newInstance(args.getString("key_bundle_peer_id").orEmpty())
                .show(fragment.childFragmentManager, "group_bulletin")
        }
        container.addView(row, groupBulletinIndex(container, descId))
    }.onFailure {
        Utils.log("GroupBulletinEntry: add failed: $it")
    }
}

private fun groupBulletinIndex(container: LinearLayout, descId: Int): Int {
    for (i in 0 until container.childCount) {
        if (container.getChildAt(i).entryText(descId) == GROUP_MEMBER_LABEL) return i + 1
    }
    return minOf(5, container.childCount)
}

private fun View.entryText(descId: Int): String? =
    findViewById<TextView>(descId)?.text?.toString()

private fun requestAnnouncements(
    groupCode: Long,
    page: Int,
    callback: (Result<List<Announcement>>) -> Unit
) {
    thread {
        runCatching {
            val auth = webAuth()
            val fields = linkedMapOf(
                "qid" to groupCode.toString(),
                "bkn" to bkn(auth.skey).toString(),
                "ft" to "23",
                "s" to pageStart(page).toString(),
                "n" to PAGE_SIZE.toString(),
                "ni" to if (page == 1) "1" else "0",
                "format" to "json"
            )
            val body = postMultipart(GROUP_BULLETIN_LIST_URL, fields, auth)
            parseAnnouncements(body)
        }.fold(
            onSuccess = { callback(Result.success(it)) },
            onFailure = { callback(Result.failure(it)) }
        )
    }
}

private data class WebAuth(
    val uin: String,
    val skey: String,
    val pskey: String
)

private fun webAuth(): WebAuth {
    val app = MobileQQ.sMobileQQ?.peekAppRuntime() ?: error("运行时未就绪")
    val uin = app.currentAccount().takeIf { it.isNotBlank() } ?: error("账号未就绪")
    val ticket = app.getManager(AppRuntime.TICKET_MANAGER) as? TicketManager ?: error("票据服务不可用")
    val skey = ticket.getSkey(uin)?.takeIf { it.isNotBlank() }
        ?: ticket.getRealSkey(uin)?.takeIf { it.isNotBlank() }
        ?: error("skey 缺失")
    val pskey = ticket.getPskey(uin, "qun.qq.com")?.takeIf { it.isNotBlank() }
        ?: ticket.getPskey(uin, "web.qun.qq.com")?.takeIf { it.isNotBlank() }
        ?: error("p_skey 缺失")
    return WebAuth(uin, skey, pskey)
}

private fun AppRuntime.currentAccount(): String =
    runCatching { currentAccountUin }.getOrNull()
        ?: runCatching { currentUin }.getOrNull()
        ?: runCatching { account }.getOrNull()
        ?: ""

private fun pageStart(page: Int): Int =
    if (page <= 1) -1 else -(page * PAGE_SIZE + 1)

private fun bkn(skey: String): Int {
    var hash = 5381
    for (ch in skey) {
        hash += (hash shl 5) + ch.code
    }
    return hash and 0x7fffffff
}

private fun requestDeleteAnnouncement(
    groupCode: Long,
    fid: String,
    callback: (Result<Unit>) -> Unit
) {
    thread {
        runCatching {
            val auth = webAuth()
            val body = postMultipart(
                GROUP_BULLETIN_DELETE_URL,
                linkedMapOf(
                    "qid" to groupCode.toString(),
                    "bkn" to bkn(auth.skey).toString(),
                    "fid" to fid,
                    "format" to "json"
                ),
                auth
            )
            checkCgi(body)
        }.fold(
            onSuccess = { callback(Result.success(Unit)) },
            onFailure = { callback(Result.failure(it)) }
        )
    }
}

private fun requestRemindAnnouncement(
    groupCode: Long,
    fid: String,
    callback: (Result<Unit>) -> Unit
) {
    thread {
        runCatching {
            val auth = webAuth()
            val query = linkedMapOf(
                "gc" to groupCode.toString(),
                "feed_id" to fid,
                "bkn" to bkn(auth.skey).toString()
            ).toQuery()
            val body = postEmpty("$GROUP_BULLETIN_REMIND_URL?$query", auth)
            checkCgi(body)
        }.fold(
            onSuccess = { callback(Result.success(Unit)) },
            onFailure = { callback(Result.failure(it)) }
        )
    }
}

private fun postMultipart(url: String, fields: Map<String, String>, auth: WebAuth): String {
    val boundary = "----QQPro${System.currentTimeMillis()}"
    val body = buildString {
        for ((key, value) in fields) {
            append("--").append(boundary).append("\r\n")
            append("Content-Disposition: form-data; name=\"").append(key).append("\"\r\n\r\n")
            append(value).append("\r\n")
        }
        append("--").append(boundary).append("--\r\n")
    }.toByteArray(Charsets.UTF_8)
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 8000
        readTimeout = 8000
        doOutput = true
        setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        setRequestProperty("Accept", "application/json, text/plain, */*")
        setRequestProperty("Origin", "https://web.qun.qq.com")
        setRequestProperty("Referer", "https://web.qun.qq.com/")
        setRequestProperty("User-Agent", "Mozilla/5.0 QQPro")
        setRequestProperty("Cookie", auth.cookieHeader())
    }
    return conn.readResponse(body)
}

private fun postEmpty(url: String, auth: WebAuth): String {
    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 8000
        readTimeout = 8000
        doOutput = true
        setRequestProperty("Accept", "application/json, text/plain, */*")
        setRequestProperty("User-Agent", "Mozilla/5.0 QQPro")
        setRequestProperty("Cookie", auth.cookieHeader())
    }
    return conn.readResponse(ByteArray(0))
}

private fun HttpURLConnection.readResponse(body: ByteArray): String {
    return try {
        outputStream.use { it.write(body) }
        val stream = if (responseCode in 200..299) inputStream else errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (responseCode !in 200..299) error("HTTP $responseCode: ${text.take(80)}")
        text
    } finally {
        disconnect()
    }
}

private fun WebAuth.cookieHeader(): String =
    listOf(
        "uin=o$uin",
        "p_uin=o$uin",
        "skey=$skey",
        "p_skey=$pskey"
    ).joinToString("; ")

private fun Map<String, String>.toQuery(): String =
    entries.joinToString("&") {
        "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
    }

private fun parseAnnouncements(body: String): List<Announcement> {
    val root = JSONObject(body)
    val code = root.optInt("ec", root.optInt("code", root.optInt("ret", 0)))
    if (code != 0) {
        val msg = root.optString("em", root.optString("msg", root.optString("message", "unknown")))
        error("接口返回 $code: $msg")
    }
    val array = findAnnouncementArray(root) ?: JSONArray()
    val out = arrayListOf<Announcement>()
    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue
        out.add(parseAnnouncement(item))
    }
    return out
}

private fun checkCgi(body: String) {
    val root = JSONObject(body)
    val code = root.optInt("ec", root.optInt("code", root.optInt("ret", root.optInt("cgicode", 0))))
    if (code != 0) {
        val msg = root.optString(
            "em",
            root.optString("msg", root.optString("message", root.optString("errorMessage", "unknown")))
        )
        error("接口返回 $code: $msg")
    }
}

private fun findAnnouncementArray(root: JSONObject): JSONArray? {
    root.optJSONArray("feeds")?.let { return it }
    root.optJSONArray("list")?.let { return it }
    root.optJSONArray("announcements")?.let { return it }
    root.optJSONObject("data")?.let { data ->
        data.optJSONArray("feeds")?.let { return it }
        data.optJSONArray("list")?.let { return it }
        data.optJSONArray("announcements")?.let { return it }
    }
    return null
}

private fun parseAnnouncement(item: JSONObject): Announcement {
    val msg = item.optJSONObject("msg")
        ?: item.optJSONObject("message")
        ?: item.optJSONObject("content")
        ?: JSONObject()
    val text = msg.optStringAny("text", "content", "textFace", "msg")
        .ifBlank { item.optStringAny("text", "content") }
    val id = item.optStringAny("fid", "feedId", "id").ifBlank {
        "${item.optLongAny("pubt", "publishTime", "time")}:$text"
    }
    val author = item.optStringAny("nickname", "nick", "authorName").ifBlank {
        item.optJSONObject("uinfo")?.optStringAny("nick", "name").orEmpty()
    }.ifBlank {
        item.optLongAny("u", "uin", "author").takeIf { it > 0 }?.toString().orEmpty()
    }
    val publishTime = item.optLongAny("pubt", "publishTime", "time", "createTime")
    val picCount = msg.optJSONArray("pics")?.length()
        ?: item.optJSONArray("pics")?.length()
        ?: 0
    return Announcement(id, text, author, publishTime, picCount)
}

private fun JSONObject.optStringAny(vararg keys: String): String {
    for (key in keys) {
        if (has(key)) return optString(key).orEmpty()
    }
    return ""
}

private fun JSONObject.optLongAny(vararg keys: String): Long {
    for (key in keys) {
        if (has(key)) return optLong(key)
    }
    return 0L
}

private fun feedTitle(feed: Announcement): String =
    feed.text.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }
        ?: "群公告"

private fun feedMeta(feed: Announcement): String {
    val author = feed.author.takeIf { it.isNotBlank() } ?: "未知发布者"
    val time = if (feed.publishTime > 0) BULLETIN_TIME.format(Date(feed.publishTime * 1000)) else ""
    return listOf(author, time).filter { it.isNotBlank() }.joinToString("  ·  ")
}

private fun feedPreview(feed: Announcement): String {
    val text = feed.text.decodeHtmlEntities().replace('\n', ' ').trim()
    val pics = feed.picCount
    val body = when {
        text.isNotBlank() -> text
        pics > 0 -> "[图片 $pics 张]"
        else -> "无文本内容"
    }
    return if (body.length > 80) body.take(80) + "…" else body
}

private fun feedBody(feed: Announcement): String {
    val text = feed.text.decodeHtmlEntities().trim()
    val pics = feed.picCount
    return buildString {
        if (text.isNotBlank()) append(text)
        if (pics > 0) {
            if (isNotEmpty()) append("\n\n")
            append("[图片 ").append(pics).append(" 张]")
        }
        if (isEmpty()) append("无文本内容")
    }
}

class GroupBulletinFragment : MyDialogFragment() {

    private lateinit var root: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val feeds = arrayListOf<Announcement>()
    private var groupCode = 0L
    private var page = 1
    private var hasMore = false
    private var loading = false
    private var active = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupCode = arguments?.getString(ARG_GROUP_CODE)?.toLongOrNull() ?: 0L
    }

    override fun onDestroyView() {
        active = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = LinearLayout(inflater.context).vertical()
        root.layoutParams = android.view.ViewGroup.LayoutParams(FILL, FILL)
        root.setBackgroundColor(BULLETIN_BG)
        return SwipeBackLayout(inflater.context).apply {
            addView(root, FILL, FILL)
            onSwipeBack = { dismiss() }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        showLoading()
        load(1)
    }

    private fun load(targetPage: Int) {
        if (loading) return
        if (groupCode == 0L) {
            showMessage("群号无效")
            return
        }
        loading = true
        handler.postDelayed({ onLoadFailed("群公告加载超时") }, 10000L)
        requestAnnouncements(groupCode, targetPage) { result ->
            runOnUi {
                if (!active) return@runOnUi
                handler.removeCallbacksAndMessages(null)
                loading = false
                result.onSuccess { items ->
                    page = targetPage
                    hasMore = items.size >= PAGE_SIZE
                    appendFeeds(items)
                    showList()
                }.onFailure {
                    if (feeds.isEmpty()) showMessage(it.message ?: "群公告加载失败")
                    else Utils.toast(requireContext(), it.message ?: "群公告加载失败")
                }
            }
        }
    }

    private fun appendFeeds(items: List<Announcement>) {
        for (item in items) {
            if (feeds.none { it.id == item.id }) feeds.add(item)
        }
    }

    private fun onLoadFailed(message: String) {
        if (!active || !loading) return
        loading = false
        if (feeds.isEmpty()) showMessage(message) else Utils.toast(requireContext(), message)
    }

    private fun showLoading() {
        root.removeAllViews()
        root.gravity = Gravity.CENTER
        root.content {
            add<TextView>()
                .text("加载群公告…")
                .textSize(14f)
                .textColor(0xFF_FFFFFF)
        }
    }

    private fun showList() {
        root.removeAllViews()
        root.gravity = Gravity.NO_GRAVITY
        if (feeds.isEmpty()) {
            showMessage("暂无群公告")
            return
        }
        root.content {
            title("群公告 (${feeds.size})")
            val data = feeds.toList()
            val list = add<RecyclerView>().linearLayout()
            (list.layoutParams as LinearLayout.LayoutParams).apply {
                width = FILL
                height = 0
                weight = 1f
            }
            list.content(
                data = data,
                factory = { bulletinRow() },
                update = { feed ->
                    (getChildAt(0) as TextView).text = feedTitle(feed)
                    (getChildAt(1) as TextView).text = feedMeta(feed)
                    (getChildAt(2) as TextView).text = feedPreview(feed)
                    clickable { openDetail(feed) }
                    setOnLongClickListener {
                        showBulletinMenu(feed)
                        true
                    }
                }
            )
            if (hasMore) {
                button("加载更多") { load(page + 1) }
            }
        }
    }

    private fun showMessage(message: String) {
        root.removeAllViews()
        root.gravity = Gravity.CENTER
        root.content {
            add<TextView>()
                .text(message)
                .textSize(14f)
                .textColor(0xFF_FFFFFF)
                .padding(bottom = 14.dp)
        }
    }

    private fun openDetail(feed: Announcement) {
        GroupBulletinDetailFragment.newInstance(groupCode, feed)
            .show(childFragmentManager, "group_bulletin_detail")
    }

    private fun showBulletinMenu(feed: Announcement) {
        resolveCanManage {
            GroupBulletinMenuFragment.newInstance(groupCode, feed, it).apply {
                onDeleted = {
                    feeds.removeAll { item -> item.id == feed.id }
                    if (feeds.isEmpty()) showMessage("暂无群公告") else showList()
                }
            }.show(childFragmentManager, "group_bulletin_menu")
        }
    }

    private fun resolveCanManage(callback: (Boolean) -> Unit) {
        CurrentGroupMembers.get(SelfContact.peerUid) { member ->
            runOnUi {
                callback(member.role == MemberRole.OWNER || member.role == MemberRole.ADMIN)
            }
        }
    }

    private fun momoi.mod.qqpro.lib.GroupScope.title(label: String) {
        add<TextView>()
            .text(label)
            .textSize(15f)
            .textColor(0xFF_FFFFFF)
            .apply {
                gravity = Gravity.CENTER
                layoutParams.width = FILL
            }
            .padding(top = 14.dp, bottom = 12.dp)
    }

    private fun momoi.mod.qqpro.lib.GroupScope.button(label: String, onClick: () -> Unit) {
        add<TextView>()
            .text(label)
            .textSize(14f)
            .textColor(0xFF_000000.toInt())
            .padding(top = 10.dp, bottom = 10.dp)
            .apply {
                gravity = Gravity.CENTER
                layoutParams.width = FILL
                background = GradientDrawable().apply {
                    setColor(BULLETIN_ACCENT)
                    cornerRadius = 22.dp.toFloat()
                }
            }
            .margin(left = 18.dp, top = 6.dp, right = 18.dp, bottom = 0)
            .clickable(onClick)
    }

    private fun bulletinRow(): View {
        val row = LinearLayout(requireContext()).vertical()
        row.layoutParams = LinearLayout.LayoutParams(FILL, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        row.setPadding(16.dp, 10.dp, 16.dp, 10.dp)
        row.addView(TextView(requireContext()).apply {
            textSize = 14f
            setTextColor(0xFF_FFFFFF.toInt())
            maxLines = 1
        })
        row.addView(TextView(requireContext()).apply {
            textSize = 11f
            setTextColor(BULLETIN_ACCENT)
        })
        row.addView(TextView(requireContext()).apply {
            textSize = 12f
            setTextColor(0xFF_CFCFCF.toInt())
            maxLines = 2
        })
        return row
    }

    private fun feedTitle(feed: Announcement): String =
        feed.text.lineSequence().firstOrNull()?.takeIf { it.isNotBlank() }
            ?: "群公告"

    private fun feedMeta(feed: Announcement): String {
        val author = feed.author.takeIf { it.isNotBlank() } ?: "未知发布者"
        val time = if (feed.publishTime > 0) BULLETIN_TIME.format(Date(feed.publishTime * 1000)) else ""
        return listOf(author, time).filter { it.isNotBlank() }.joinToString("  ·  ")
    }

    private fun feedPreview(feed: Announcement): String {
        val text = feed.text.decodeHtmlEntities().replace('\n', ' ').trim()
        val pics = feed.picCount
        val body = when {
            text.isNotBlank() -> text
            pics > 0 -> "[图片 $pics 张]"
            else -> "无文本内容"
        }
        return if (body.length > 80) body.take(80) + "…" else body
    }

    private fun feedBody(feed: Announcement): String {
        val text = feed.text.decodeHtmlEntities().trim()
        val pics = feed.picCount
        return buildString {
            if (text.isNotBlank()) append(text)
            if (pics > 0) {
                if (isNotEmpty()) append("\n\n")
                append("[图片 ").append(pics).append(" 张]")
            }
            if (isEmpty()) append("无文本内容")
        }
    }

    companion object {
        private const val ARG_GROUP_CODE = "group_code"

        fun newInstance(groupCode: String) = GroupBulletinFragment().apply {
            arguments = Bundle().apply { putString(ARG_GROUP_CODE, groupCode) }
        }
    }
}

class GroupBulletinDetailFragment : MyDialogFragment() {

    private var groupCode = 0L
    private lateinit var feed: Announcement

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments ?: Bundle()
        groupCode = args.getLong(ARG_GROUP_CODE)
        feed = Announcement.fromArgs(args.getBundle(ARG_FEED) ?: Bundle())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = LinearLayout(inflater.context).vertical()
        root.layoutParams = android.view.ViewGroup.LayoutParams(FILL, FILL)
        root.setBackgroundColor(BULLETIN_BG)
        val sv = ScrollView(inflater.context)
        sv.layoutParams = LinearLayout.LayoutParams(FILL, FILL)
        val col = LinearLayout(inflater.context).vertical()
        col.setPadding(16.dp, 10.dp, 16.dp, 16.dp)
        sv.addView(col, android.view.ViewGroup.LayoutParams(FILL, android.view.ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(sv)
        col.content {
            add<TextView>()
                .text(feedTitle(feed))
                .textSize(15f)
                .textColor(0xFF_FFFFFF)
                .apply {
                    gravity = Gravity.CENTER
                    layoutParams.width = FILL
                }
                .padding(top = 14.dp, bottom = 12.dp)
            add<TextView>()
                .text(feedMeta(feed))
                .textSize(11f)
                .textColor(BULLETIN_ACCENT)
                .apply { layoutParams.width = FILL }
                .padding(bottom = 10.dp)
            add<TextView>()
                .text(feedBody(feed))
                .textSize(14f)
                .textColor(0xFF_EEEEEE.toInt())
                .apply { layoutParams.width = FILL }
                .padding(bottom = 12.dp)
        }
        col.setOnLongClickListener {
            resolveCanManage {
                GroupBulletinMenuFragment.newInstance(groupCode, feed, it).apply {
                    onDeleted = { dismiss() }
                }.show(childFragmentManager, "group_bulletin_menu")
            }
            true
        }
        return SwipeBackLayout(inflater.context).apply {
            addView(root, FILL, FILL)
            onSwipeBack = { dismiss() }
        }
    }

    private fun resolveCanManage(callback: (Boolean) -> Unit) {
        CurrentGroupMembers.get(SelfContact.peerUid) { member ->
            runOnUi {
                callback(member.role == MemberRole.OWNER || member.role == MemberRole.ADMIN)
            }
        }
    }

    companion object {
        private const val ARG_GROUP_CODE = "group_code"
        private const val ARG_FEED = "feed"

        fun newInstance(groupCode: Long, feed: Announcement) = GroupBulletinDetailFragment().apply {
            arguments = Bundle().apply {
                putLong(ARG_GROUP_CODE, groupCode)
                putBundle(ARG_FEED, feed.toArgs())
            }
        }
    }
}

class GroupBulletinMenuFragment : MyDialogFragment() {

    var onDeleted: (() -> Unit)? = null
    private var groupCode = 0L
    private var canManage = false
    private lateinit var feed: Announcement

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments ?: Bundle()
        groupCode = args.getLong(ARG_GROUP_CODE)
        canManage = args.getBoolean(ARG_CAN_MANAGE)
        feed = Announcement.fromArgs(args.getBundle(ARG_FEED) ?: Bundle())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val root = LinearLayout(inflater.context).vertical()
        root.layoutParams = android.view.ViewGroup.LayoutParams(FILL, FILL)
        root.gravity = Gravity.CENTER
        val menu = LinearLayout(inflater.context).vertical()
        menu.layoutParams = LinearLayout.LayoutParams(FILL, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
        menu.background = GradientDrawable().apply { setColor(0x44_000000) }
        if (Utils.isRoundScreen) {
            LinearScope(menu).add<View>().width(FILL).height(0.16f.vh)
            menu.paddingHorizontal(0.1f.vh)
        }
        menu.addView(createMenuItem(menu, "复制内容", ICON_COPY) {
            Utils.copyToClipboard(requireContext(), feedBody(feed))
            dismiss()
        })
        if (canManage) {
            menu.addView(createMenuItem(menu, "提醒未读") {
                remind()
            })
            menu.addView(createMenuItem(menu, "删除公告", ICON_REVOKE) {
                delete()
            })
        }
        if (Utils.isRoundScreen) {
            LinearScope(menu).add<View>().width(FILL).height(0.16f.vh)
        }
        root.addView(menu)
        return SwipeBackLayout(inflater.context).apply {
            addView(root, FILL, FILL)
            onSwipeBack = { dismiss() }
        }
    }

    private fun remind() {
        requestRemindAnnouncement(groupCode, feed.id) { result ->
            runOnUi {
                if (!isAdded) return@runOnUi
                result.onSuccess {
                    Utils.toast(requireContext(), "已提醒")
                }.onFailure {
                    Utils.toast(requireContext(), it.message ?: "提醒失败")
                }
                dismiss()
            }
        }
    }

    private fun delete() {
        requestDeleteAnnouncement(groupCode, feed.id) { result ->
            runOnUi {
                if (!isAdded) return@runOnUi
                result.onSuccess {
                    Utils.toast(requireContext(), "已删除")
                    onDeleted?.invoke()
                }.onFailure {
                    Utils.toast(requireContext(), it.message ?: "删除失败")
                }
                dismiss()
            }
        }
    }

    companion object {
        private const val ARG_GROUP_CODE = "group_code"
        private const val ARG_FEED = "feed"
        private const val ARG_CAN_MANAGE = "can_manage"

        fun newInstance(groupCode: Long, feed: Announcement, canManage: Boolean) =
            GroupBulletinMenuFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_GROUP_CODE, groupCode)
                    putBundle(ARG_FEED, feed.toArgs())
                    putBoolean(ARG_CAN_MANAGE, canManage)
                }
            }
    }
}

private fun createMenuItem(
    parent: LinearLayout,
    text: String,
    iconRes: Int = 0,
    onClick: () -> Unit
): View {
    val item = LayoutInflater.from(parent.context).inflate(NATIVE_MENU_ITEM_LAYOUT, parent, false)
    item.findViewById<AppCompatTextView>(NATIVE_MENU_TEXT_ID)?.text = text
    item.findViewById<ImageView>(NATIVE_MENU_ICON_ID)?.let { icon ->
        if (iconRes != 0) {
            icon.setImageResource(iconRes)
            icon.isVisible = true
        } else {
            icon.isVisible = false
        }
    }
    item.findViewById<View>(NATIVE_MENU_SWITCH_ID)?.isVisible = false
    item.setOnClickListener { onClick() }
    return item
}
