package moye.wear.hook

import momoi.mod.qqpro.Settings
import java.util.regex.Pattern

object LinkText {

    private const val STOP = "\\s\\u4e00-\\u9fa5\\u3002\\uff1f\\uff01\\uff0c\\u3001\\uff1b\\uff1a\\u201c\\u201d\\u2018\\u2019\\uff08\\uff09\\u300a\\u300b\\u3008\\u3009\\u3010\\u3011\\u300e\\u300f\\u300c\\u300d\\uff43\\uff44\\u3014\\u3015\\u2026\\u2014\\uff5e\\uff4f\\uffe5"

    private val strictPattern: Pattern =
        Pattern.compile("(?i)https?://[^$STOP]+")

    private val widePattern: Pattern =
        Pattern.compile(
            "(?:https?://)?(?:[\\w-]+\\.)+[a-zA-Z]{2,}(?:[/:?#][^\\s$STOP]*)?"
        )

    private val currentPattern: Pattern
        get() = if (Settings.wideUrlMatch.value) widePattern else strictPattern

    fun ranges(text: CharSequence): List<IntRange> {
        val result = mutableListOf<IntRange>()
        val matcher = currentPattern.matcher(text)
        while (matcher.find()) {
            val raw = text.subSequence(matcher.start(), matcher.end()).toString()
            if (looksLikeUrl(raw)) {
                result.add(matcher.start() until matcher.end())
            }
        }
        return result
    }

    fun firstUrl(text: CharSequence?): String? {
        text ?: return null
        return ranges(text).firstOrNull()?.let { text.subSequence(it.first, it.last + 1).toString() }
    }

    fun withScheme(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun looksLikeUrl(raw: String): Boolean {
        if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) return true
        val host = raw.substringBefore('/').substringBefore('?').substringBefore('#')
        val tld = host.substringAfterLast('.', "")
        return tld.length >= 2 && tld.all { it.isLetter() }
    }
}
