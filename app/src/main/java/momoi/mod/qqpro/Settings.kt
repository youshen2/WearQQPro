package momoi.mod.qqpro

import android.content.SharedPreferences
import androidx.core.content.edit
import momoi.mod.qqpro.util.Utils

object Settings {
    val sp: SharedPreferences = Utils.application.getSharedPreferences("qqpro", 0)
    val scale = FloatPref("scale", 0.9f)
    val chatScale = FloatPref("chatScale", 0.93f)
    val enableSmoothScroll = BooleanPref("enableSmoothScroll", false)
    val encoderScrollSpeed = FloatPref("encoderScrollSpeed", 1.0f)
    val blockBack = BooleanPref("blockBack", false)
    val disableSwipeBack = BooleanPref("disableSwipeBack", false)
    val swapCenterKeyboard = BooleanPref("swapCenterKeyboard", false)
    val hideVoiceButton = BooleanPref("hideVoiceButton", false)
    val enableExtraMenu = BooleanPref("enableExtraMenu", false)
    val enableAntiDetection = BooleanPref("enableAntiDetection", true)

    // 识别没有 http(s):// 前缀的裸链接（如 example.com/path）
    val wideUrlMatch = BooleanPref("wideUrlMatch", true)
    // 在聊天消息下方展示链接预览卡片（会向链接所在站点发起网络请求，默认关闭）
    val enableLinkPreview = BooleanPref("enableLinkPreview", false)
    // 群聊昵称与称号改为双行显示，关闭时即使开启头像也保持单行
    val nickTitleTwoLine = BooleanPref("nickTitleTwoLine", false)
    // 群聊昵称左侧显示发送者头像（会从 QQ 头像 CDN 拉取图片，默认关闭）
    val showGroupAvatar = BooleanPref("showGroupAvatar", false)
    // 群聊头像大小，相对昵称文字的倍数，默认 2.4 倍
    val avatarSizeScale = FloatPref("avatarSizeScale", 2.4f)
    // 同一人连续发言时，只在第一条显示头像/昵称/等级，其余合并（默认关闭）
    val hideRepeatedSender = BooleanPref("hideRepeatedSender", false)
    val picMaxHeightRatio = FloatPref("picMaxHeightRatio", 0.5f)
    val parseAtMember = BooleanPref("parseAtMember", false)

    private val moye = Utils.application.getSharedPreferences("wearqq", 0)
    val text get() = moye.getString("voice_btn_text", "")?.let {
        if (it == "QQ") {
            ""
        } else {
            it
        }
    } ?: ""
}

abstract class Pref<T>(def: T) {
    var value: T = def
        set(value) {
            field = value
            set(value)
        }

    protected abstract fun set(value: T)
}

class FloatPref(private val key: String, def: Float) :
    Pref<Float>(Settings.sp.getFloat(key, def)) {
    override fun set(value: Float) = Settings.sp.edit {
        putFloat(key, value)
    }
}

class BooleanPref(private val key: String, def: Boolean) :
    Pref<Boolean>(Settings.sp.getBoolean(key, def)) {
    override fun set(value: Boolean) = Settings.sp.edit {
        putBoolean(key, value)
    }
}
