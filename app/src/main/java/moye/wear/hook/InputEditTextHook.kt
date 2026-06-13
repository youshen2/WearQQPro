package moye.wear.hook

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import com.tencent.watch.ime.CustomEditText
import momoi.anno.mixin.Mixin
import moye.wear.span.ExtraSpanHelper

@Mixin
class InputEditTextHook(context: Context, attrs: AttributeSet) : CustomEditText(context, attrs) {

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL && ExtraSpanHelper.deleteBeforeCursor(this)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (selStart == selEnd) {
            val pos = ExtraSpanHelper.normalizeSelection(this, selStart)
            if (pos != selStart) {
                setSelection(pos)
                return
            }
        }
        super.onSelectionChanged(selStart, selEnd)
    }
}
