package moye.wear.hook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.tencent.watch.aio_impl.ui.WatchAIOFragment
import momoi.anno.mixin.Mixin

@Mixin
class WatchAIOFragmentHook : WatchAIOFragment() {
    override fun X(): Boolean {
        val vp = f ?: return super.X()
        return vp.currentItem == 0 && vp.scrollState == ViewPager2.SCROLL_STATE_IDLE
    }

    override fun Y(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = requireNotNull(super.Y(inflater, container, savedInstanceState))
        (view as? ViewGroup)?.let {
            ExtraMenuOverlay.attach(this, it)
        }
        return view
    }

    override fun onDestroy() {
        ExtraMenuOverlay.detach(this)
        super.onDestroy()
    }
}
