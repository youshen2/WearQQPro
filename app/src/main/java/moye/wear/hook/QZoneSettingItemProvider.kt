package moye.wear.hook

import androidx.fragment.app.Fragment
import com.tencent.watch.aio_impl.ui.frames.setting.AbsSettingItem
import com.tencent.watch.aio_impl.ui.frames.setting.SettingItemProvider
import momoi.anno.mixin.Mixin

/**
 * 在私聊会话详情页的设置项列表末尾追加「QQ空间」选项。
 *
 * 原始 [SettingItemProvider.a] 为私聊返回 [消息设置 / 备注 / 删除好友] 三项，
 * 这里在其后追加 [QZoneSettingItem]，点击可快速跳转到对方的 QQ 空间动态列表。
 * 群聊不会走到该方法（群聊的列表在 SettingFrame 中单独构建），因此只影响个人会话。
 */
@Mixin
class QZoneSettingItemProvider : SettingItemProvider() {

    override fun a(
        fragment: Fragment,
        uid: String,
        uin: String
    ): MutableList<AbsSettingItem> {
        val items = super.a(fragment, uid, uin)
        items.add(QZoneSettingItem(fragment, uid, uin))
        return items
    }
}
