package moye.wear.hook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tencent.watch.ime.InputMethodFragment
import momoi.anno.mixin.Mixin
import moye.wear.span.ExtraSpanHelper

/**
 * After the IME view is built, insert any pending @-mention / image extras into the input
 * EditText as blue chips (replacing the base's plain text-preview behavior).
 *
 * The CustomEditText is held by the KeyboardPresenter (field `f.j`), which Y_ constructs.
 */
@Mixin
class InputMethodFragmentHook : InputMethodFragment() {

    override fun Y_(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.Y_(inflater, container, savedInstanceState)
        runCatching {
            val et = f.j
            ExtraSpanHelper.insertPendingExtras(et)
            ExtraSpanHelper.apply(et)
        }
        return view
    }
}
