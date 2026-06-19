package moye.wear.hook

import android.content.Context
import android.net.Uri
import com.tencent.mobileqq.qroute.QRoute
import com.tencent.qqnt.kernel.nativeinterface.Contact
import com.tencent.qqnt.kernel.nativeinterface.IOperateCallback
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.watch.api.IMsgApi
import momoi.mod.qqpro.MsgUtil
import momoi.mod.qqpro.hook.action.CurrentContact
import momoi.mod.qqpro.util.Utils
import momoi.mod.qqpro.util.runOnUi
import moye.wearqq.IMEOperation
import java.io.File

object AlbumMultiSelect {

    private fun sendWithImage(): Boolean =
        Utils.application.getSharedPreferences("wearqq", 0).getBoolean("send_with_image", true)

    fun deliver(context: Context, uris: List<Uri>) {
        if (uris.isEmpty()) return
        val appContext = context.applicationContext
        Thread {
            val elements = ArrayList<MsgElement>(uris.size)
            for (uri in uris) {
                val path = resolvePath(appContext, uri) ?: continue
                runCatching {
                    QRoute.api(IMsgApi::class.java).createPicElement(path, 0)
                }.onSuccess {
                    elements.add(it)
                }.onFailure {
                    Utils.log("AlbumMultiSelect createPicElement failed: $it")
                }
            }
            if (elements.isEmpty()) {
                runOnUi { Utils.toast(appContext, "图片处理失败") }
                return@Thread
            }
            if (sendWithImage()) {
                runOnUi { insertIntoIme(elements) }
            } else {
                sendSequentially(elements, 0)
            }
        }.start()
    }

    private fun insertIntoIme(elements: List<MsgElement>) {
        runCatching {
            IMEOperation.extraMsg.addAll(elements)
            IMEOperation.INSTANCE.openIME()
        }.onFailure {
            Utils.log("AlbumMultiSelect insertIntoIme failed: $it")
        }
    }

    private fun sendSequentially(elements: List<MsgElement>, index: Int) {
        if (index >= elements.size) return
        val target = Contact(CurrentContact.chatType, CurrentContact.peerUid, CurrentContact.guildId)
        val payload = arrayListOf(elements[index])
        MsgUtil.msgService.sendMsg(target, 0L, payload, IOperateCallback { code, errMsg ->
            if (code != 0) {
                Utils.log("AlbumMultiSelect send failed code=$code msg=$errMsg")
            }
            sendSequentially(elements, index + 1)
        })
    }

    private fun resolvePath(context: Context, uri: Uri): String? {
        copyToCache(context, uri)?.let { return it }
        return null
    }

    private fun copyToCache(context: Context, uri: Uri): String? {
        return try {
            val dir = context.externalCacheDir ?: context.cacheDir
            val out = File(dir, "album_${System.currentTimeMillis()}_${uri.hashCode()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                out.outputStream().use { input.copyTo(it) }
            } ?: return null
            if (out.exists() && out.length() > 0L) out.path else null
        } catch (e: Exception) {
            Utils.log("AlbumMultiSelect copyToCache failed: ${e.message}")
            null
        }
    }
}
