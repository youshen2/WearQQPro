package moye.wear.hook

import androidx.viewpager2.widget.ViewPager2
import com.tencent.watch.aio_impl.ui.WatchAIOFragment
import momoi.anno.mixin.Mixin

@Mixin
class WatchAIOFragmentHook : WatchAIOFragment() {
    override fun X(): Boolean {
        val vp = f ?: return super.X()
        return vp.currentItem == 0 && vp.scrollState == ViewPager2.SCROLL_STATE_IDLE
    }
}
