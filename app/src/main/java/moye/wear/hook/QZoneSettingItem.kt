package moye.wear.hook

import android.view.View
import androidx.fragment.app.Fragment
import com.tencent.qqlive.module.videoreport.collect.EventCollector
import com.tencent.watch.aio_impl.ui.frames.setting.AbsSettingItem

/**
 * 个人会话详情页的「QQ空间」选项，点击后跳转到对方的 QQ 空间动态列表。
 *
 * 该类不是 Mixin，而是作为新类被注入进 APK，由 [QZoneSettingItemProvider]
 * 在私聊设置项列表末尾追加。构造参数与其它 AbsSettingItem 子类保持一致
 * （fragment、uid、uin），其中 uin 为对方 QQ 号，用于打开其空间主页。
 */
class QZoneSettingItem(
    fragment: Fragment,
    uid: String,
    private val uin: String
) : AbsSettingItem(fragment, uid) {

    // drawable/icon_qzone
    override fun getIconResId(): Int = 0x7e0805bd

    // string/share_componet_v2_Qzone -> "QQ空间"
    override fun getText(): Int = 0x7e120b11

    override fun onClick(v: View?) {
        v?.let { EventCollector.getInstance().onViewClickedBefore(it) }
        // B1 内部会自行 findNavController 跳转到空间主页，传入对方 uin 即可
        runCatching {
            WatchPicElementExtKt.B1(fragment, uin.toLong(), null, 0)
        }
        v?.let { EventCollector.getInstance().onViewClicked(it) }
    }
}
