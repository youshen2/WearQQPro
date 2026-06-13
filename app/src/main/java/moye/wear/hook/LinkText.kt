package moye.wear.hook

import momoi.mod.qqpro.Settings
import java.util.regex.Pattern

/**
 * 链接识别与规整的统一入口。
 *
 * 这里把"带前缀"和"无前缀"两种匹配规则分开维护：
 * - 严格模式只认 http(s):// 开头的链接，与项目原有行为保持一致；
 * - 宽松模式额外识别 example.com/path 这类裸域名，由设置项 [Settings.wideUrlMatch] 控制。
 */
object LinkText {

    // 链接尾部需要排除的中文标点 / 全角符号，避免把句末标点也吞进链接里
    private const val TAIL_EXCLUDE =
        "\\u4e00-\\u9fa5\\u3002\\uff1f\\uff01\\uff0c\\u3001\\uff1b\\uff1a" +
        "\\u201c\\u201d\\u2018\\u2019\\uff08\\uff09\\u300a\\u300b\\u3008\\u3009" +
        "\\u3010\\u3011\\u300e\\u300f\\u300c\\u300d\\u3014\\u3015\\u2026\\u2014\\uff5e\\uffe5"

    // 带前缀：必须以 http:// 或 https:// 开头
    private val strictPattern: Pattern =
        Pattern.compile("https?://[^\\s$TAIL_EXCLUDE]+")

    // 无前缀：前缀可有可无，域名形如 a.b(.c) 且顶级域为 2 位以上字母，后面可跟路径
    private val widePattern: Pattern =
        Pattern.compile(
            "(?:https?://)?(?:[\\w-]+\\.)+[a-zA-Z]{2,}(?:[/:?#][^\\s$TAIL_EXCLUDE]*)?"
        )

    private val currentPattern: Pattern
        get() = if (Settings.wideUrlMatch.value) widePattern else strictPattern

    /** 文本中所有链接的位置区间（按出现顺序）。 */
    fun ranges(text: CharSequence): List<IntRange> {
        val result = mutableListOf<IntRange>()
        val matcher = currentPattern.matcher(text)
        while (matcher.find()) {
            // 裸域名很容易误伤纯数字（如版本号 1.2.3），这里再过滤一遍
            val raw = text.subSequence(matcher.start(), matcher.end()).toString()
            if (looksLikeUrl(raw)) {
                result.add(matcher.start() until matcher.end())
            }
        }
        return result
    }

    /** 取文本里的第一个链接，没有则返回 null。 */
    fun firstUrl(text: CharSequence?): String? {
        text ?: return null
        return ranges(text).firstOrNull()?.let { text.subSequence(it.first, it.last + 1).toString() }
    }

    /** 补全协议头：裸链接统一按 https 处理。 */
    fun withScheme(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    // 简单启发式：带协议头的一律放行；裸域名要求顶级域是字母，挡掉纯数字误匹配
    private fun looksLikeUrl(raw: String): Boolean {
        if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) return true
        val host = raw.substringBefore('/').substringBefore('?').substringBefore('#')
        val tld = host.substringAfterLast('.', "")
        return tld.length >= 2 && tld.all { it.isLetter() }
    }
}
