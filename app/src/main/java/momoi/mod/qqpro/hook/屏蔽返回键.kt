package momoi.mod.qqpro.hook

import androidx.fragment.app.FragmentActivity
import com.tencent.qqnt.watch.mainframe.MainActivity
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.util.Utils

@Mixin
class 屏蔽返回键 : MainActivity() {
    override fun onResume() {
        super.onResume()
        if (Settings.disableSwipeBack.value) {
            try {
                enableFlingGesture(false)
            } catch (e: Throwable) {
                Utils.log("disableSwipeBack: err ${e.javaClass.simpleName}: ${e.message}")
            }
        }
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java", replaceWith = ReplaceWith("放过我吧waring大爹"))
    override fun onBackPressed() {
        if (!Settings.blockBack.value) {
            super.onBackPressed()
        }
    }

}