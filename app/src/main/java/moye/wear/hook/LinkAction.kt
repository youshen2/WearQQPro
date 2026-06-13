package moye.wear.hook

import android.view.View
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.showDialog
import momoi.mod.qqpro.util.Utils

/**
 * 统一的"打开链接"入口。
 *
 * 开启确认开关时弹出 [LinkConfirmFragment]；关闭时直接跳转。
 * 弹窗需要借宿主 Fragment 的 childFragmentManager，所以拿不到宿主就退回直接打开，
 * 而不是把这次点击吞掉。
 */
fun View.openLinkWithConfirm(url: String) {
    if (!Settings.confirmOpenLink.value) {
        Utils.openUrl(LinkText.withScheme(url))
        return
    }
    runCatching { showDialog(LinkConfirmFragment(url)) }
        .onFailure { Utils.openUrl(LinkText.withScheme(url)) }
}
