package moye.wear.hook

import android.content.Context
import android.os.Bundle
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.view.View
import com.tencent.qqnt.graytips.HighlightItem
import com.tencent.qqnt.graytips.action.BaseUserActionInfo
import com.tencent.qqnt.graytips.span.HighlightClickableSpan
import com.tencent.qqnt.kernel.nativeinterface.MemberInfo
import com.tencent.qqnt.watch.profile.ProfileData
import momoi.anno.mixin.ConstructorHook
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.hook.action.CurrentMemberInfo
import momoi.mod.qqpro.util.Utils
import java.lang.ref.WeakReference
import mqq.app.AppRuntime

private const val AT_LINK_COLOR = 0xFF_5E97F6.toInt()

@Mixin
class BaseUserActionInfoHook(uid: String, nick: String, uin: String) :
    BaseUserActionInfo(uid, nick, uin) {

    @JvmField
    var profileUid: String? = null

    @ConstructorHook
    fun storeProfileUid(uid: String?, nick: String?, uin: String?) {
        profileUid = uid
    }
}

@Mixin
class HighlightClickableSpanHook(
    runtime: AppRuntime?, color: Int, ctx: Context?, item: HighlightItem?
) : HighlightClickableSpan(runtime, color, ctx, item) {

    override fun onClick(widget: View) {
        if (Settings.parseAtMember.value) {
            val uid = memberUid()
            if (!uid.isNullOrEmpty()) {
                widget.openMemberProfileByUid(uid)
                return
            }
        }
        super.onClick(widget)
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        if (Settings.parseAtMember.value && !memberUid().isNullOrEmpty()) {
            ds.color = AT_LINK_COLOR
            ds.isUnderlineText = false
        }
    }

    private fun memberUid(): String? =
        ((this.d)?.d as? BaseUserActionInfoHook)?.profileUid
}

fun View.openMemberProfileByUid(uid: String) {
    if (uid.isEmpty()) return
    val cached = CurrentMemberInfo.map[uid]
    if (cached != null) {
        navigateToProfile(cached.uid, cached.uin.toString(), cached.displayName())
    } else {
        navigateToProfile(uid, "", "")
    }
}

private fun View.navigateToProfile(uid: String, uin: String, nick: String) {
    try {
        val nav = findNavControllerFromTree() ?: return
        val destId = resources.getIdentifier("profileCardFragment", "id", context.packageName)
        if (destId == 0) return
        val profileData = ProfileData("0-0", -1, uin, uid, nick, false)
        val bundle = Bundle().apply { putParcelable("profile_data", profileData) }
        val navigate = nav.javaClass.methods.firstOrNull { m ->
            val p = m.parameterTypes
            p.size == 3 && p[0] == Int::class.javaPrimitiveType && p[1] == Bundle::class.java
        } ?: return
        navigate.invoke(nav, destId, bundle, null)
    } catch (e: Exception) {
        Utils.log("navigateToProfile error: ${e.message}")
    }
}

private fun View.findNavControllerFromTree(): Any? {
    val tagId = resources.getIdentifier("nav_controller_view_tag", "id", context.packageName)
    if (tagId == 0) return null
    var v: View? = this
    while (v != null) {
        when (val tag = v.getTag(tagId)) {
            is WeakReference<*> -> tag.get()?.let { return it }
            null -> {}
            else -> return tag
        }
        v = v.parent as? View
    }
    return null
}

private fun MemberInfo.displayName(): String = when {
    cardName.isNotEmpty() -> cardName
    remark.isNotEmpty() -> remark
    else -> nick
}
