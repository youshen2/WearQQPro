package moye.wear.span

import android.text.Spanned
import android.widget.EditText
import com.huanli233.qplus.utils.TextUtilKt
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.msg.api.impl.MsgUtilApiImpl
import moye.wearqq.AtElementArg
import moye.wearqq.IMEOperation

/**
 * Placeholder + span engine for the inline blue-chip @-mention / image input UX.
 *
 * Ported from old_src moye.wearqq.ExtraSpanHelper. New code lives in moye.wear; the
 * data substrate (IMEOperation, AtElementArg) is the base-APK moye.wearqq.* holder that
 * every untouched trigger site already feeds.
 *
 * Pending extras are inserted into the EditText as single Private-Use-Area chars
 * (0xE000.. for @, 0xE100.. for images), each rendered as an [AtSpan] chip. At send time
 * [parseTextElements] converts the placeholder chars back into real MsgElements positionally.
 */
object ExtraSpanHelper {

    private const val SURROUNDING = "ꡦATꡦ"

    private val placeholderMap = HashMap<String, AtElementArg>()
    private val imagePlaceholderMap = HashMap<String, MsgElement>()
    private var nextPlaceholder = 0xE000
    private var nextImagePlaceholder = 0xE100

    private fun buildDisplayText(arg: AtElementArg): String {
        // nickname is stored b64-encoded once at construction; old_src decodes twice.
        var nick = TextUtilKt.b64Decode(arg.atNickname)
        nick = TextUtilKt.b64Decode(nick)
        return "@$nick"
    }

    private fun buildImageDisplayText(): String = "[图片]"

    private fun registerPlaceholder(arg: AtElementArg): String {
        var a = arg
        AtElementArg.Companion.tryParse(arg.toText())?.let { a = it }
        val key = nextPlaceholder.toChar().toString()
        nextPlaceholder += 1
        placeholderMap[key] = a
        return key
    }

    private fun registerImagePlaceholder(el: MsgElement): String {
        val key = nextImagePlaceholder.toChar().toString()
        nextImagePlaceholder += 1
        imagePlaceholderMap[key] = el
        return key
    }

    /** Re-span an EditText: clear existing chips, then re-apply over placeholder chars + inline markers. */
    fun apply(et: EditText?) {
        val editable = et?.text ?: return
        for (s in editable.getSpans(0, editable.length, AtSpan::class.java)) {
            editable.removeSpan(s)
        }
        val str = editable.toString()

        // Pass 1: single-char PUA placeholders (@ args + image elements)
        var i = 0
        while (i < str.length) {
            val ch = str.substring(i, i + 1)
            val arg = placeholderMap[ch]
            if (arg != null) {
                editable.setSpan(AtSpan(buildDisplayText(arg)), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else if (imagePlaceholderMap[ch] != null) {
                editable.setSpan(AtSpan(buildImageDisplayText()), i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            i += 1
        }

        // Pass 2: legacy inline "ꡦATꡦ...ꡦATꡦ" encoded AT blocks
        var from = 0
        while (true) {
            val startIdx = str.indexOf(SURROUNDING, from)
            if (startIdx < 0) return
            val endIdx = str.indexOf(SURROUNDING, startIdx + SURROUNDING.length)
            if (endIdx < 0) return
            val blockEnd = endIdx + SURROUNDING.length
            AtElementArg.Companion.tryParse(str.substring(startIdx, blockEnd))?.let { parsed ->
                editable.setSpan(AtSpan(buildDisplayText(parsed)), startIdx, blockEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            from = blockEnd
        }
    }

    /** Drain pending @ args + image elements from IMEOperation into the EditText at the cursor, then re-span. */
    fun insertPendingExtras(et: EditText?) {
        val editable = et?.text ?: return
        var pos = et.selectionStart
        if (pos < 0) pos = editable.length

        val it1 = IMEOperation.INSTANCE.getExtra().iterator()
        while (it1.hasNext()) {
            val item = it1.next()
            if (item is AtElementArg) {
                val ph = registerPlaceholder(item)
                editable.insert(pos, ph)
                pos += ph.length
                it1.remove()
            }
        }
        val it2 = IMEOperation.extraMsg.iterator()
        while (it2.hasNext()) {
            val el = it2.next()
            val ph = registerImagePlaceholder(el)
            editable.insert(pos, ph)
            pos += ph.length
            it2.remove()
        }
        et.setSelection(pos)
        apply(et)
    }

    /** Backspace handling: delete a whole chip atomically. Returns true if it consumed the event. */
    fun deleteBeforeCursor(et: EditText?): Boolean {
        et ?: return false
        if (et.selectionStart != et.selectionEnd) return false
        val pos = normalizeSelection(et, et.selectionStart)
        if (pos != et.selectionStart) et.setSelection(pos)
        if (pos <= 0) return false
        val editable = et.text ?: return false
        val spans = editable.getSpans(pos - 1, pos, AtSpan::class.java)
        if (spans.isEmpty()) return false
        val span = spans[0]
        val s = editable.getSpanStart(span)
        val e = editable.getSpanEnd(span)
        editable.delete(s, e)
        et.setSelection(s)
        return true
    }

    /** Snap a collapsed caret out of the middle of a chip to its nearer edge. */
    fun normalizeSelection(et: EditText?, sel: Int): Int {
        val editable = et?.text ?: return sel
        if (editable.isEmpty()) return sel
        val lo = (sel - 1).coerceAtLeast(0)
        val hi = (sel + 1).coerceAtMost(editable.length)
        val spans = editable.getSpans(lo, hi, AtSpan::class.java)
        if (spans.isEmpty()) return sel
        val span = spans[0]
        val s = editable.getSpanStart(span)
        val e = editable.getSpanEnd(span)
        if (sel <= s || sel >= e) return sel
        return if (sel <= (s + e) / 2) s else e
    }

    private fun flushTextBuffer(sb: StringBuilder, out: ArrayList<MsgElement>) {
        if (sb.isEmpty()) return
        out.add(MsgUtilApiImpl.instance.createTextElement(sb.toString()))
        sb.delete(0, sb.length)
    }

    /** Expand placeholder chars / inline markers in each text element into real At/image MsgElements. */
    fun parseTextElements(list: ArrayList<MsgElement>?) {
        list ?: return
        val out = ArrayList<MsgElement>()
        for (el in list) {
            val content = el?.textElement?.content
            if (el == null || content == null) {
                if (el != null) out.add(el)
                continue
            }
            val buf = StringBuilder()
            var i = 0
            while (i < content.length) {
                val ch = content.substring(i, i + 1)
                val arg = placeholderMap[ch]
                if (arg != null) {
                    flushTextBuffer(buf, out)
                    out.add(MsgUtilApiImpl.instance.createAtTextElement(buildDisplayText(arg), arg.atUid, 2))
                    i += 1
                    continue
                }
                val img = imagePlaceholderMap[ch]
                if (img != null) {
                    flushTextBuffer(buf, out)
                    out.add(img)
                    i += 1
                    continue
                }
                if (content.indexOf(SURROUNDING, i) == i) {
                    val mEnd = content.indexOf(SURROUNDING, i + SURROUNDING.length)
                    if (mEnd < 0) {
                        buf.append(ch); i += 1; continue
                    }
                    val blockEnd = mEnd + SURROUNDING.length
                    val parsed = AtElementArg.Companion.tryParse(content.substring(i, blockEnd))
                    if (parsed != null) {
                        flushTextBuffer(buf, out)
                        out.add(MsgUtilApiImpl.instance.createAtTextElement(buildDisplayText(parsed), parsed.atUid, 2))
                        i = blockEnd
                    } else {
                        buf.append(ch); i = blockEnd
                    }
                } else {
                    buf.append(ch); i += 1
                }
            }
            flushTextBuffer(buf, out)
        }
        list.clear()
        list.addAll(out)
    }
}
