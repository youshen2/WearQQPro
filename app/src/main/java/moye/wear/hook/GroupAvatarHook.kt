package moye.wear.hook

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.TextView
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import com.tencent.qqnt.kernel.nativeinterface.MemberInfo
import com.tencent.qqnt.kernel.nativeinterface.MemberRole
import com.tencent.watch.aio_impl.ui.widget.AIOCellGroupWidget
import download
import momoi.mod.qqpro.Colors
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.child
import momoi.mod.qqpro.hook.action.SelfContact
import momoi.mod.qqpro.lib.RadiusBackgroundSpan
import momoi.mod.qqpro.util.runOnUi
import java.util.WeakHashMap
import kotlin.concurrent.thread

/**
 * 群聊昵称区域样式调整。
 *
 * 由 AIOCell 的群成员回调驱动：拿到 [MemberInfo] 后，按开关决定昵称 TextView
 * 使用单行还是「等级标签 + 昵称」两行样式；头像开关只控制左侧是否挂圆形头像
 * compound drawable，不再隐式决定单双行。自己发的消息统一保持原本的单行样式。
 *
 * 头像从 QQ 头像 CDN 按 uin 拉取，复用项目里已有的 [download]。所有缓存读写都放在
 * UI 线程上，避免列表快速滚动时并发改 Map 引发的竞态；解码放在后台线程。
 */
object GroupAvatarHook {
    private const val TYPE_NORMAL = 0
    private const val TYPE_OWNER = 1
    private const val TYPE_ADMIN = 2
    private const val TYPE_SPECIAL = 3

    // 已解码好的圆形头像，按 uin 缓存（仅在 UI 线程访问）
    private val avatars = HashMap<Long, Bitmap>()
    // 同一 uin 正在加载时排队的回调，保证只下载/解码一次（仅在 UI 线程访问）
    private val pending = HashMap<Long, MutableList<(Bitmap) -> Unit>>()
    // 记录每个昵称控件当前绑定的 uin，列表复用后用它丢弃过期回调
    private val boundUin = WeakHashMap<View, Long>()

    /**
     * 根据成员信息刷新昵称区域。在 UI 线程调用。
     */
    fun update(widget: AIOCellGroupWidget, member: MemberInfo) {
        val nick = widget.getNickWidget<TextView>() ?: return
        val isSelf = member.uid == SelfContact.peerUid
        val showTwoLine = Settings.nickTitleTwoLine.value && !isSelf
        val showAvatar = Settings.showGroupAvatar.value && !isSelf

        if (!showTwoLine) {
            // 单行内联样式
            nick.maxLines = 1
            nick.text = member.toDisplay()
        } else {
            // 两行样式：等级标签在上，昵称在下
            nick.maxLines = 2
            nick.text = member.toDisplayTwoLine()
        }

        if (!showAvatar) {
            clearAvatar(nick)
            boundUin.remove(nick)
            return
        }

        val uin = member.uin
        boundUin[nick] = uin
        clearAvatar(nick)

        avatars[uin]?.let {
            applyAvatar(nick, it)
            return
        }
        loadAvatar(nick, uin) { bmp ->
            // 控件可能已被复用绑到别的 uin，仅在仍然匹配时应用
            if (boundUin[nick] == uin) applyAvatar(nick, bmp)
        }
    }

    /** 下载并解码头像，带去重；回调在 UI 线程触发。 */
    private fun loadAvatar(view: View, uin: Long, callback: (Bitmap) -> Unit) {
        pending[uin]?.let {
            it.add(callback)
            return
        }
        pending[uin] = mutableListOf(callback)

        val ctx = view.context
        val file = ctx.externalCacheDir?.child("avatar_$uin.jpg") ?: run {
            pending.remove(uin)
            return
        }
        file.parentFile?.mkdirs()

        val decodeAndDispatch = {
            val bmp = runCatching { decodeCircle(file.path) }.getOrNull()
            runOnUi {
                val waiters = pending.remove(uin) ?: return@runOnUi
                if (bmp != null) {
                    avatars[uin] = bmp
                    waiters.forEach { it(bmp) }
                }
            }
        }

        if (file.exists()) {
            thread { decodeAndDispatch() }
        } else {
            val url = "https://q.qlogo.cn/headimg_dl?dst_uin=$uin&spec=100"
            // download 的回调本身就在后台线程，直接解码即可
            download(url, file) { ok ->
                if (ok) decodeAndDispatch()
                else runOnUi { pending.remove(uin) }
            }
        }
    }

    /** 把圆形头像作为左侧 compound drawable 挂到昵称上，尺寸为文字高度的若干倍（可在设置中调整）。 */
    private fun applyAvatar(nick: TextView, bitmap: Bitmap) {
        val size = (nick.textSize * Settings.avatarSizeScale.value).toInt().coerceAtLeast(1)
        val drawable = BitmapDrawable(nick.resources, bitmap).apply {
            setBounds(0, 0, size, size)
        }
        nick.setCompoundDrawables(drawable, null, null, null)
        nick.compoundDrawablePadding = (nick.textSize * 0.4f).toInt()
    }

    private fun clearAvatar(nick: TextView) {
        nick.setCompoundDrawables(null, null, null, null)
    }

    /** 解码图片文件并裁成正方形后做圆形遮罩。 */
    private fun decodeCircle(path: String): Bitmap? {
        val src = BitmapFactory.decodeFile(path) ?: return null
        val side = minOf(src.width, src.height)
        if (side <= 0) return null
        val left = (src.width - side) / 2
        val top = (src.height - side) / 2

        val output = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val rectF = RectF(0f, 0f, side.toFloat(), side.toFloat())
        canvas.drawOval(rectF, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(
            src,
            Rect(left, top, left + side, top + side),
            rectF,
            paint
        )
        if (src != output) src.recycle()
        return output
    }

    /** 单行：与原昵称样式一致——等级标签 + 昵称（自己发的消息则昵称在前）。 */
    fun MemberInfo.toDisplay() = buildSpannedString {
        val isSelf = uid == SelfContact.peerUid
        val name = displayName()
        if (isSelf) {
            append(name)
            append(" ")
            append(levelTag())
        } else {
            append(levelTag())
            append(" ")
            append(name)
        }
    }

    /** 两行：等级标签在上，昵称在下。 */
    private fun MemberInfo.toDisplayTwoLine() = buildSpannedString {
        append(levelTag())
        append("\n")
        append(displayName())
    }

    private fun MemberInfo.displayName() = when {
        cardName.isNotEmpty() -> cardName
        remark.isNotEmpty() -> remark
        else -> nick
    }

    private fun MemberInfo.memberType() = when {
        role == MemberRole.OWNER -> TYPE_OWNER
        role == MemberRole.ADMIN -> TYPE_ADMIN
        !memberSpecialTitle.isNullOrEmpty() -> TYPE_SPECIAL
        else -> TYPE_NORMAL
    }

    /** 构造等级 / 身份标签（带圆角背景）。 */
    private fun MemberInfo.levelTag() = buildSpannedString {
        val type = memberType()
        inSpans(
            RadiusBackgroundSpan(
                bgColor = when (type) {
                    TYPE_ADMIN -> Colors.NickTag.adminBg
                    TYPE_OWNER -> Colors.NickTag.ownerBg
                    TYPE_SPECIAL -> Colors.NickTag.specialBg
                    else -> Colors.NickTag.normalBg
                },
                textColor = when (type) {
                    TYPE_ADMIN -> Colors.NickTag.adminText
                    TYPE_OWNER -> Colors.NickTag.ownerText
                    TYPE_SPECIAL -> Colors.NickTag.specialText
                    else -> Colors.NickTag.normalText
                }
            ),
            RelativeSizeSpan(0.8f)
        ) {
            append("LV")
            append(memberLevel.toString())
            if (!memberSpecialTitle.isNullOrEmpty()) {
                append(" ")
                append(memberSpecialTitle)
            } else when (type) {
                TYPE_OWNER -> append(" 群主")
                TYPE_ADMIN -> append(" 管理员")
            }
        }
    }
}
