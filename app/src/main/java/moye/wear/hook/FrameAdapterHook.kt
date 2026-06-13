package moye.wear.hook

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.tencent.watch.aio_impl.ui.frames.FrameAdapter
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Settings

@Mixin
class FrameAdapterHook(
    fragment: Fragment,
    requireArguments: Bundle,
    selector: (Int) -> Unit
) : FrameAdapter(fragment, requireArguments, selector) {
    override fun getItemCount(): Int {
        val count = super.getItemCount()
        return if (Settings.enableExtraMenu.value && count > 1) count - 1 else count
    }

    override fun f(position: Int): Fragment {
        if (!Settings.enableExtraMenu.value) {
            return super.f(position)
        }
        val count = super.getItemCount()
        if (count <= 1 || position == 0) {
            return super.f(position)
        }
        return super.f(position + 1)
    }

    override fun createFragment(position: Int): Fragment = f(position)
}
