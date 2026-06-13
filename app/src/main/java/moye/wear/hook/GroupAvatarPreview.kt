package moye.wear.hook

import android.os.Bundle
import android.view.View
import com.tencent.qqnt.watch.gallery.preview.RFWLayerLaunchUtilKt
import com.tencent.watch.aio_impl.ui.frames.SettingFrame
import download
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.child
import momoi.mod.qqpro.util.Utils
import momoi.mod.qqpro.util.runOnUi

@Mixin
class GroupAvatarPreview : SettingFrame() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments ?: return
        val peerId = args.getString("key_bundle_peer_id")
        val chatType = args.getInt("key_bundle_chat_type")
        if (chatType == 2 && !peerId.isNullOrEmpty()) {
            Utils.log("GroupAvatarPreview: bind group avatar preview, group=$peerId")
            bindGroupAvatarPreview(this, this.f, peerId)
        }
    }
}

private fun bindGroupAvatarPreview(fragment: SettingFrame, avatarView: View, groupCode: String) {
    avatarView.setOnClickListener {
        val ctx = avatarView.context
        val cacheFile = ctx.externalCacheDir!!.child("group_avatar_$groupCode.jpg")
        val show = {
            val host = WatchPicElementExtKt.X(fragment)
            if (host == null) {
                Utils.log("GroupAvatarPreview: gallery host null")
            } else {
                val media = RFWLayerLaunchUtilKt.f(cacheFile.absolutePath)
                val bundle = Bundle().apply {
                    putBoolean("key_support_long_click", true)
                    putBoolean("key_need_clear_cache", true)
                    putStringArrayList("key_menu_item", arrayListOf("SavePic"))
                }
                RFWLayerLaunchUtilKt.d(ctx, host, null, listOf(media), 0, bundle)
            }
        }
        if (cacheFile.exists()) {
            show()
        } else {
            val url = "https://p.qlogo.cn/gh/$groupCode/$groupCode/0"
            Utils.log("GroupAvatarPreview: downloading $url")
            download(url, cacheFile) { ok ->
                runOnUi {
                    if (ok) show() else Utils.toast(ctx, "头像加载失败")
                }
            }
        }
    }
}
