package moye.wear.hook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tencent.watch.ime.InputMethodFragment
import momoi.anno.mixin.Mixin
import moye.wear.span.ExtraSpanHelper

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
