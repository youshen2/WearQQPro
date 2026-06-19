package moye.wear.hook

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.tencent.widget.Switch
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Pref
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.asGroup
import momoi.mod.qqpro.drawable.roundCornerDrawable
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.GroupScope
import momoi.mod.qqpro.lib.LinearScope
import momoi.mod.qqpro.lib.WRAP
import momoi.mod.qqpro.lib.background
import momoi.mod.qqpro.lib.clickable
import momoi.mod.qqpro.lib.content
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.dpf
import momoi.mod.qqpro.lib.gravity
import momoi.mod.qqpro.lib.height
import momoi.mod.qqpro.lib.margin
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize
import momoi.mod.qqpro.lib.vertical
import momoi.mod.qqpro.lib.width
import momoi.mod.qqpro.util.Utils
import moye.wear.hook.LongPressMenuOrderDialog
import moye.wearqq.SettingsActivity

@Mixin
class SettingsPage : SettingsActivity() {

    private lateinit var contentView: LinearLayout
    private lateinit var sp: SharedPreferences

    @SuppressLint("ResourceType", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sp = getSharedPreferences("wearqq", 0)
        val linear = findViewById<View>(0x7e090afd).asGroup()
        linear.requestFocus()
        contentView = LinearLayout(this).vertical()
        linear.addView(contentView)
        linear.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(FILL, 64.dp) })
        showMainMenu()
    }

    private fun showMainMenu() {
        contentView.removeAllViews()
        GroupScope(contentView).apply {
            sectionTitle("设置")
            categoryEntry("显示与缩放", "缩放倍数、头像大小等") { showDisplaySettings() }
            categoryEntry("表冠与滚动", "滚动速度、平滑滚动等") { showScrollSettings() }
            categoryEntry("聊天与输入", "输入控件、消息显示") { showChatSettings() }
            categoryEntry("群聊设置", "头像、昵称显示") { showGroupSettings() }
            categoryEntry("通知与提醒", "通知、震动") { showNotificationSettings() }
            categoryEntry("高级设置", "反检测、严格匹配") { showSecuritySettings() }
            saveButton()
        }
    }

    private fun showDisplaySettings() {
        contentView.removeAllViews()
        GroupScope(contentView).apply {
            sectionTitle("显示与缩放")
            floatInput("缩放倍数", "返回聊天页即时生效", Settings.scale)
            floatInput("聊天文本缩放", "聊天气泡内的文本大小缩放倍数", Settings.chatScale)
            floatInput("头像大小", "群聊头像相对昵称文字的倍数，默认 2.4", Settings.avatarSizeScale)
            floatInput("图片最大高度", "聊天图片最大显示高度(占屏幕高度比例)，默认 0.5", Settings.picMaxHeightRatio)
            backButton()
        }
    }

    private fun showScrollSettings() {
        contentView.removeAllViews()
        GroupScope(contentView).apply {
            sectionTitle("表冠与滚动")
            switch("平滑表冠滚动", "表冠划起来没动画开这个", Settings.enableSmoothScroll)
            floatInput("表冠滚动速度", "表冠滚动距离倍率，默认 1.0", Settings.encoderScrollSpeed)
            switch("屏蔽返回键", "用于米兔等会将右滑当作返回的手表", Settings.blockBack)
            switch("屏蔽右滑返回", "关闭WearQQ内右滑返回上一页的手势", Settings.disableSwipeBack)
            backButton()
        }
    }

    private fun showChatSettings() {
        contentView.removeAllViews()
        GroupScope(contentView).apply {
            sectionTitle("聊天界面")
            switch("输入键居中", "在聊天页面将输入键居中放置", Settings.swapCenterKeyboard)
            switch("隐藏语音键", "开启后聊天页面不再显示语音按钮", Settings.hideVoiceButton)
            switch("启用附加菜单", "开启后使用加号按钮代替第二页", Settings.enableExtraMenu)
            switch("链接预览卡片", "在消息下方展示链接预览，会向链接站点发起请求", Settings.enableLinkPreview)
            stringInput("语音按钮文字", "语音按钮显示的文字，留空恢复默认", "voice_btn_text", "QQ")
            actionEntry("长按菜单排序", "拖拽调整聊天气泡长按菜单项顺序") {
                runCatching {
                    LongPressMenuOrderDialog.show(this@SettingsPage)
                }.onFailure {
                    Utils.toast(this@SettingsPage, "打开排序面板失败")
                }
            }
            sectionTitle("输入与发送")
            switchFromSP("单行输入", "消息输入页面文本框使用单行输入，输入法出现问题时可以尝试开启", "single_line_input", false)
            switchFromSP("携带图片发送", "从相册选择图片时允许携带其他内容进行发送", "send_with_image", true)
            switchFromSP("回复带@", "回复时自动添加@mentions", "reply_with_at", false)
            switchFromSP("双击朗读", "双击聊天气泡调用系统TTS进行朗读", "double_speak", false)
            switchFromSP("双击回复", "双击聊天气泡进行回复", "double_reply", false)
            backButton()
        }
    }

    private fun showGroupSettings() {
        contentView.removeAllViews()
        GroupScope(contentView).apply {
            sectionTitle("群聊设置")
            switch("群聊显示头像", "在群聊昵称左侧显示发送者头像，会从头像服务器拉取图片", Settings.showGroupAvatar)
            switch("昵称称号双行", "开启后群聊昵称与称号分两行显示", Settings.nickTitleTwoLine)
            switch("合并连续消息头", "同一人连续发言时只在第一条显示头像和昵称", Settings.hideRepeatedSender)
            backButton()
        }
    }

    private fun showSecuritySettings() {
        contentView.removeAllViews()
        GroupScope(contentView).apply {
            sectionTitle("高级设置")
            switch("反检测", "隐藏 root、调试、模拟器、VPN/代理 等环境判定", Settings.enableAntiDetection)
            switch("识别无前缀链接", "识别 example.com 这类没有 http 前缀的链接", Settings.wideUrlMatch)
            backButton()
        }
    }

    private fun showNotificationSettings() {
        contentView.removeAllViews()
        GroupScope(contentView).apply {
            sectionTitle("通知与提醒")
            switchFromSP("允许通知", "允许发送通知来提醒新消息。如果是米兔之类会由系统代为通知的设备，请关闭此开关。", "allow_notification", false)
            switchFromSP("常驻通知", "发送一个常驻通知来尝试进行保活（重启WearQQ后生效）", "resident_notification", false)
            switchFromSP("允许震动", "关闭后将禁用WearQQ内所有震动", "allow_vibrate", true)
            backButton()
        }
    }

    private fun GroupScope.sectionTitle(title: String) {
        add<TextView>()
            .text(title)
            .textSize(12f)
            .textColor(0xFF_FFFFFF)
            .padding(left = 8.dp, top = 0, right = 8.dp, bottom = 0)
            .margin(left = 4.dp, top = 10.dp, right = 4.dp, bottom = 2.dp)
            .apply { alpha = 0.72f }
    }

    private fun GroupScope.categoryEntry(
        title: String,
        desc: String = "",
        onClick: () -> Unit
    ) {
        baseEntry(title, desc) {
            add<TextView>()
                .text(">")
                .textSize(14f)
                .textColor(0xFF_FFFFFF)
                .padding(left = 10.dp, right = 10.dp)
        }.clickable(onClick)
    }

    private fun GroupScope.saveButton() {
        add<View>().height(6.dp)
        add<TextView>()
            .text("保存")
            .textSize(14f)
            .textColor(0xFF_000000)
            .gravity(Gravity.CENTER)
            .width(FILL)
            .padding(top = 12.dp, bottom = 12.dp)
            .background(roundCornerDrawable(0xFF_4FC3F7.toInt(), 22.dpf))
            .margin(top = 6.dp)
            .clickable {
                Utils.toast(this@SettingsPage, "设置已保存")
                finish()
            }
    }

    private fun GroupScope.backButton() {
        add<View>().height(6.dp)
        add<TextView>()
            .text("返回")
            .textSize(14f)
            .textColor(0xFF_FFFFFF)
            .gravity(Gravity.CENTER)
            .width(FILL)
            .padding(top = 12.dp, bottom = 12.dp)
            .background(roundCornerDrawable(0xFF_2A2A2A.toInt(), 22.dpf))
            .margin(top = 6.dp)
            .clickable { showMainMenu() }
    }

    private fun GroupScope.switch(title: String, desc: String = "", pref: Pref<Boolean>) {
        baseEntry(title, desc) {
            add(Switch(this@SettingsPage, null).apply {
                layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
                isChecked = pref.value
                setOnCheckedChangeListener { _, checked -> pref.value = checked }
            })
        }
    }

    private fun GroupScope.switchFromSP(
        title: String,
        desc: String = "",
        key: String,
        default: Boolean
    ) {
        baseEntry(title, desc) {
            add(Switch(this@SettingsPage, null).apply {
                layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
                isChecked = sp.getBoolean(key, default)
                setOnCheckedChangeListener { _, checked ->
                    sp.edit().putBoolean(key, checked).apply()
                }
            })
        }
    }

    private fun GroupScope.floatInput(title: String, desc: String = "", pref: Pref<Float>) {
        baseEntry(title, desc) {
            val et: EditText = add()
            et.text(pref.value.toString())
            et.width(70.dp)
            et.textSize(12f)
            et.textColor(0xFF_FFFFFF)
            et.gravity(Gravity.CENTER)
            (et as EditText).hint = "支持小数"
            et.background(roundCornerDrawable(0x44_000000, 8.dpf))
            et.padding(left = 6.dp, top = 4.dp, right = 6.dp, bottom = 4.dp)
            et.isSingleLine = true
            et.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            et.doAfterTextChanged {
                it?.toString()?.toFloatOrNull()?.let { value -> pref.value = value }
            }
        }
    }

    private fun GroupScope.stringInput(
        title: String,
        desc: String = "",
        key: String,
        default: String
    ) {
        baseEntry(title, desc) {
            val et: EditText = add()
            et.text(sp.getString(key, default) ?: default)
            et.width(100.dp)
            et.textSize(12f)
            et.textColor(0xFF_FFFFFF)
            et.gravity(Gravity.CENTER)
            (et as EditText).hint = default
            et.background(roundCornerDrawable(0x44_000000, 8.dpf))
            et.padding(left = 6.dp, top = 4.dp, right = 6.dp, bottom = 4.dp)
            et.isSingleLine = true
            et.doAfterTextChanged {
                sp.edit().putString(key, it?.toString() ?: "").apply()
            }
        }
    }

    private fun GroupScope.actionEntry(title: String, desc: String = "", onClick: () -> Unit) {
        baseEntry(title, desc) {
            add<TextView>()
                .text("进入")
                .textSize(12f)
                .textColor(0xFF_FFFFFF)
                .padding(left = 10.dp, top = 5.dp, right = 10.dp, bottom = 5.dp)
                .background(roundCornerDrawable(0x44_000000, 8.dpf))
                .clickable(onClick)
        }.clickable(onClick)
    }

    private fun GroupScope.baseEntry(
        title: String,
        desc: String = "",
        content: LinearScope.() -> Unit
    ): LinearLayout {
        return add<LinearLayout>()
            .width(FILL)
            .background(roundCornerDrawable(0x99_3D3D3D.toInt(), 10.dpf))
            .margin(left = 4.dp, top = 0, right = 4.dp, bottom = 8.dp)
            .padding(left = 8.dp, top = 7.dp, right = 6.dp, bottom = 7.dp)
            .gravity(Gravity.CENTER_VERTICAL)
            .content {
                add<LinearLayout>()
                    .vertical()
                    .weight(1f)
                    .padding(right = 6.dp)
                    .content {
                        add<TextView>()
                            .text(title)
                            .textSize(12f)
                            .textColor(0xFF_FFFFFF)
                        if (desc.isNotBlank()) {
                            add<TextView>()
                                .text(desc)
                                .textSize(10f)
                                .textColor(0xFF_FFFFFF)
                                .margin(top = 1.dp)
                                .apply { alpha = 0.88f }
                        }
                    }
                content.invoke(this)
            }
    }
}
