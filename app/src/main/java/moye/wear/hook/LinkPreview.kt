package moye.wear.hook

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.tencent.watch.aio_impl.ui.widget.AIOCellGroupWidget
import loadPicUrl
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.api.Http
import momoi.mod.qqpro.drawable.roundCornerDrawable
import momoi.mod.qqpro.hook.style.MyImageView
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.WRAP
import momoi.mod.qqpro.lib.background
import momoi.mod.qqpro.lib.content
import momoi.mod.qqpro.lib.create
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.dpf
import momoi.mod.qqpro.lib.margin
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.scaleType
import momoi.mod.qqpro.lib.size
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize
import momoi.mod.qqpro.lib.vertical
import momoi.mod.qqpro.lib.width
import momoi.mod.qqpro.util.runOnUi
import momoi.mod.qqpro.warp
import java.net.URI
import java.util.WeakHashMap

/**
 * 链接预览卡片。
 *
 * 关注点：
 * - RecyclerView 会复用 cell，所以卡片视图按 [AIOCellGroupWidget] 弱引用缓存，
 *   并用一个递增的 token 标记"这次绑定的是哪条链接"，异步结果回来时先比对 token，
 *   过期的（cell 已被复用给别的消息）直接丢弃，避免错位显示。
 * - 解析结果按 URL 缓存：value 为 null 表示"抓过但没拿到有用信息"，不再重复请求。
 */
object LinkPreview {

    // 每个 cell 对应的卡片视图
    private val cards = WeakHashMap<AIOCellGroupWidget, Card>()
    // 解析结果缓存：key = 规整后的 url，value = null 表示无可用信息
    private val cache = HashMap<String, Meta?>()

    /** 抓回来的元数据。 */
    private data class Meta(
        val title: String?,
        val desc: String?,
        val siteName: String?,
        val imageUrl: String?,
        val iconUrl: String?
    ) {
        val usable get() = !title.isNullOrBlank() || !desc.isNullOrBlank() || !imageUrl.isNullOrBlank()
    }

    /**
     * 在消息文本下方挂载/更新预览卡片。
     * @param widget 当前消息 cell
     * @param text   消息文本（用来取第一个链接）
     */
    fun bind(widget: AIOCellGroupWidget, text: CharSequence?) {
        if (!Settings.enableLinkPreview.value) {
            cards[widget]?.hide()
            return
        }
        val url = LinkText.firstUrl(text)?.let(LinkText::withScheme)
        if (url == null) {
            // 没有链接：已存在的卡片隐藏即可，不为无链接的消息创建/warp 视图
            cards[widget]?.hide()
            return
        }
        // 没有内容视图（非文本消息等）就不挂卡片
        val content = widget.getContentWidget<View>() ?: run {
            cards[widget]?.hide()
            return
        }
        cards.getOrPut(widget) { Card(content) }.show(url)
    }

    /** 单个 cell 的卡片，负责视图复用与异步填充。 */
    private class Card(content: View) {
        private lateinit var root: LinearLayout
        private lateinit var ivIcon: ImageView
        private lateinit var tvSite: TextView
        private lateinit var tvTitle: TextView
        private lateinit var tvDesc: TextView
        private lateinit var ivImage: ImageView

        // 递增 token，区分"最近一次绑定的链接"，挡掉复用导致的过期回调
        private var token = 0
        private var boundUrl: String? = null

        init {
            val ctx = content.context
            root = create<LinearLayout>(ctx)
                .width(FILL)
                .vertical()
                .background(roundCornerDrawable(0x66_000000.toInt(), 10.dpf))
                .padding(8.dp)
                .margin(top = 4.dp)
                .content {
                    // 头部：站点图标 + 站点名
                    add<LinearLayout>()
                        .width(FILL)
                        .content {
                            ivIcon = add<ImageView>()
                                .size(14.dp, 14.dp)
                                .scaleType(ImageView.ScaleType.CENTER_CROP)
                            tvSite = add<TextView>()
                                .textSize(10f * Settings.chatScale.value)
                                .textColor(0xFF_AAB4C2.toInt())
                                .margin(left = 4.dp)
                        }
                    tvTitle = add<TextView>()
                        .width(FILL)
                        .textSize(12f * Settings.chatScale.value)
                        .textColor(0xFF_FFFFFF.toInt())
                        .margin(top = 3.dp)
                        .apply { maxLines = 2 }
                    tvDesc = add<TextView>()
                        .width(FILL)
                        .textSize(10f * Settings.chatScale.value)
                        .textColor(0xFF_C8C8C8.toInt())
                        .margin(top = 2.dp)
                        .apply { maxLines = 3 }
                    ivImage = add<MyImageView>()
                        .width(FILL)
                        .margin(top = 4.dp)
                        .scaleType(ImageView.ScaleType.FIT_CENTER)
                }
            // 卡片视图挂到消息文本所在的竖向容器里（warp 会把 content 包进 LinearLayout）
            val host = content.warp()
            host.addView(root)
        }

        // 取消图标/标题/描述/大图，准备重新填充
        private fun reset() {
            ivIcon.setImageDrawable(null)
            ivImage.setImageDrawable(null)
            ivImage.visibility = View.GONE
            tvDesc.visibility = View.GONE
        }

        fun hide() {
            boundUrl = null
            token++
            root.visibility = View.GONE
        }

        fun show(url: String) {
            // 同一条链接已经显示过就不重复折腾（避免滚动时反复闪烁）
            if (boundUrl == url && root.visibility == View.VISIBLE) return
            boundUrl = url
            val my = ++token
            root.visibility = View.VISIBLE
            reset()

            // 先放一个基于域名的占位，提升观感
            val host = runCatching { URI(url).host }.getOrNull().orEmpty()
            tvSite.text = host
            tvTitle.text = "解析中…"

            root.setOnClickListener { it.openLinkWithConfirm(url) }

            cache[url]?.let { apply(my, url, it); return }
            if (cache.containsKey(url)) { // 命中"抓过但没结果"
                fallback(my, host)
                return
            }
            resolve(url) { meta ->
                cache[url] = meta
                runOnUi {
                    if (my != token) return@runOnUi // 已被复用给别的消息
                    if (meta != null && meta.usable) apply(my, url, meta) else fallback(my, host)
                }
            }
        }

        // 无可用元数据时，退化成"只显示域名"的极简卡片
        private fun fallback(my: Int, host: String) {
            if (my != token) return
            tvSite.text = host
            tvTitle.text = host.ifBlank { boundUrl.orEmpty() }
            tvDesc.visibility = View.GONE
            ivImage.visibility = View.GONE
        }

        private fun apply(my: Int, url: String, meta: Meta) {
            if (my != token) return
            tvSite.text = meta.siteName?.takeIf { it.isNotBlank() }
                ?: runCatching { URI(url).host }.getOrNull().orEmpty()
            tvTitle.text = meta.title?.takeIf { it.isNotBlank() } ?: tvSite.text
            meta.desc?.takeIf { it.isNotBlank() }?.let {
                tvDesc.text = it
                tvDesc.visibility = View.VISIBLE
            }
            meta.iconUrl?.let { ivIcon.loadPicUrl(it) }
            meta.imageUrl?.let {
                ivImage.visibility = View.VISIBLE
                ivImage.loadPicUrl(it)
            }
        }
    }

    // 后台抓取并解析页面元数据
    private fun resolve(url: String, callback: (Meta?) -> Unit) {
        Http.get(url) { body ->
            if (body.startsWith("Error:") || body.startsWith("HTTP error:")) {
                callback(null)
                return@get
            }
            callback(parse(url, body))
        }
    }

    // ---- 解析部分：自行用正则抽取 OpenGraph / <title> ----

    private fun parse(url: String, html: String): Meta? {
        val head = html.substringBefore("</head>", html).take(200_000)
        val title = meta(head, "og:title") ?: titleTag(head)
        val desc = meta(head, "og:description")
        val site = meta(head, "og:site_name")
        val image = meta(head, "og:image")?.let { resolveUrl(url, it) }
        val icon = iconLink(head)?.let { resolveUrl(url, it) } ?: resolveUrl(url, "/favicon.ico")
        val meta = Meta(
            title = title?.let(::unescape),
            desc = desc?.let(::unescape),
            siteName = site?.let(::unescape),
            imageUrl = image,
            iconUrl = icon
        )
        return if (meta.usable || !meta.title.isNullOrBlank()) meta else null
    }

    // 匹配 <meta property="og:xxx" content="..."> 的两种属性顺序
    private fun meta(html: String, property: String): String? {
        val p = Regex.escape(property)
        // content 在 property 之后
        Regex(
            """<meta[^>]*?(?:property|name)\s*=\s*["']$p["'][^>]*?content\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.get(1)?.let { return it }
        // content 在 property 之前
        return Regex(
            """<meta[^>]*?content\s*=\s*["']([^"']*)["'][^>]*?(?:property|name)\s*=\s*["']$p["']""",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.get(1)
    }

    private fun titleTag(html: String): String? =
        Regex("""<title[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

    // 取 favicon，跳过 svg（项目里的 BitmapFactory 解不了）
    private fun iconLink(html: String): String? {
        val matches = Regex(
            """<link[^>]*rel\s*=\s*["'][^"']*icon[^"']*["'][^>]*>""",
            RegexOption.IGNORE_CASE
        ).findAll(html)
        for (m in matches) {
            val href = Regex("""href\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
                .find(m.value)?.groupValues?.get(1) ?: continue
            if (href.endsWith(".svg", true)) continue
            return href
        }
        return null
    }

    // 把相对/协议相对地址补成绝对地址
    private fun resolveUrl(base: String, href: String): String? {
        return try {
            when {
                href.startsWith("http://", true) || href.startsWith("https://", true) -> href
                href.startsWith("//") -> "https:$href"
                else -> URI(base).resolve(href).toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    // 解码常见 HTML 实体
    private fun unescape(s: String): String = s
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .trim()
}
