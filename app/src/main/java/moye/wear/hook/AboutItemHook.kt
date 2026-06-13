package moye.wear.hook

import android.view.View
import androidx.fragment.app.Fragment
import com.tencent.qqnt.watch.selftab.item.AboutItem
import com.tencent.qqnt.watch.ui.componet.tips.TipsUtils
import com.tencent.qqlive.module.videoreport.collect.EventCollector
import momoi.anno.mixin.Mixin
import moye.wear.DisplayConfig

@Mixin
class AboutItemHook(
    fragment: Fragment
) : AboutItem(fragment) {
    override fun onClick(v: View?) {
        val fragment = b ?: return
        v?.let {
            EventCollector.getInstance().onViewClickedBefore(it)
        }
        TipsUtils.h(
            TipsUtils.a,
            fragment,
            0,
            DisplayConfig.aboutDialogText,
            0,
            2114454996,
            null,
            null,
            0,
            null,
            0,
            null,
            0,
            null,
            0,
            null,
            null,
            null,
            null,
            null,
            null,
            1048552
        )
        v?.let {
            EventCollector.getInstance().onViewClicked(it)
        }
    }
}
