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
import momoi.mod.qqpro.util.Utils
import momoi.mod.qqpro.warp
import java.net.URI
import java.util.WeakHashMap

object LinkPreview {
    private val cards = WeakHashMap<AIOCellGroupWidget, Card>()
    private val cache = HashMap<String, Meta?>()

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
            cards[widget]?.hide()
            return
        }
        val content = widget.getContentWidget<View>() ?: run {
            cards[widget]?.hide()
            return
        }
        cards.getOrPut(widget) { Card(content) }.show(url)
    }

    private class Card(content: View) {
        private lateinit var root: LinearLayout
        private lateinit var ivIcon: ImageView
        private lateinit var tvSite: TextView
        private lateinit var tvTitle: TextView
        private lateinit var tvDesc: TextView
        private lateinit var ivImage: ImageView

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
            val host = content.warp()
            host.addView(root)
        }

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
            if (boundUrl == url && root.visibility == View.VISIBLE) return
            boundUrl = url
            val my = ++token
            root.visibility = View.VISIBLE
            reset()

            val host = runCatching { URI(url).host }.getOrNull().orEmpty()
            tvSite.text = host
            tvTitle.text = "解析中…"

            root.setOnClickListener { Utils.openUrl(LinkText.withScheme(url)) }

            cache[url]?.let { apply(my, url, it); return }
            if (cache.containsKey(url)) {
                fallback(my, host)
                return
            }
            resolve(url) { meta ->
                cache[url] = meta
                runOnUi {
                    if (my != token) return@runOnUi
                    if (meta != null && meta.usable) apply(my, url, meta) else fallback(my, host)
                }
            }
        }

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

    private fun resolve(url: String, callback: (Meta?) -> Unit) {
        Http.get(url) { body ->
            if (body.startsWith("Error:") || body.startsWith("HTTP error:")) {
                callback(null)
                return@get
            }
            callback(parse(url, body))
        }
    }

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

    private fun meta(html: String, property: String): String? {
        val p = Regex.escape(property)
        Regex(
            """<meta[^>]*?(?:property|name)\s*=\s*["']$p["'][^>]*?content\s*=\s*["']([^"']*)["']""",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.get(1)?.let { return it }
        return Regex(
            """<meta[^>]*?content\s*=\s*["']([^"']*)["'][^>]*?(?:property|name)\s*=\s*["']$p["']""",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.get(1)
    }

    private fun titleTag(html: String): String? =
        Regex("""<title[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE)
            .find(html)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

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
