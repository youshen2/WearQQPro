package moye.wear.hook

import com.tencent.watch.aio_impl.ui.frames.MenuFrame
import momoi.anno.mixin.StaticHook
import momoi.mod.qqpro.hook.view.CallConfirmFragment

@StaticHook(MenuFrame::class)
fun b0(frame: MenuFrame, isVideo: Boolean) {
    val label = if (isVideo) "视频通话" else "语音通话"
    val fm = frame.childFragmentManager
    runCatching {
        CallConfirmFragment(
            "确定要发起$label 吗？",
            frame.view!!,
            onConfirm = { MenuFrame.b0(frame, isVideo) }
        ).show(fm, "qqpro_call_confirm")
    }.onFailure {
        MenuFrame.b0(frame, isVideo)
    }
}
