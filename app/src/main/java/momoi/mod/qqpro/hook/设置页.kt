package momoi.mod.qqpro.hook

import android.annotation.SuppressLint
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
import momoi.mod.qqpro.lib.hint
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
class 设置页 : SettingsActivity() {
    @SuppressLint("ResourceType", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val linear = findViewById<View>(2114521834).parent.parent.asGroup()
        linear.parent.asGroup().requestFocus()
        val header = linear.getChildAt(0)
        val originalWearQQViews = (1 until linear.childCount).map { index ->
            linear.getChildAt(index)
        }
        val saveButton = originalWearQQViews.find { view ->
            (view as? TextView)?.text?.toString() == "保存"
        }
        val wearQQContentViews = originalWearQQViews.filterNot { it === saveButton }
        linear.removeAllViews()
        linear.addView(header)
        GroupScope(linear).apply {
            wearQQContentViews.forEach { view ->
                add(view)
            }
            sectionTitle("QQPro设置")
            floatInput(
                "缩放倍数",
                "返回聊天页即时生效",
                Settings.scale
            )
            floatInput(
                "聊天文本缩放",
                "",
                Settings.chatScale
            )
            switch(
                "平滑表冠滚动",
                "表冠划起来没动画开这个",
                Settings.enableSmoothScroll
            )
            switch(
                "屏蔽返回键",
                "用于米兔等会将右滑当作返回的手表",
                Settings.blockBack
            )
            switch(
                "输入键居中",
                "在聊天页面将输入键居中放置",
                Settings.swapCenterKeyboard
            )
            add<View>()
                .height(6.dp)
            sectionTitle("WearQQ Pro设置")
            switch(
                "反检测",
                "隐藏 root、调试、模拟器、VPN/代理 等环境判定",
                Settings.enableAntiDetection
            )
            actionEntry(
                "长按菜单排序",
                "拖拽调整聊天气泡长按菜单项顺序"
            ) {
                runCatching {
                    LongPressMenuOrderDialog.show(this@设置页)
                }.onFailure {
                    Utils.toast(this@设置页, "打开排序面板失败")
                }
            }
            switch(
                "昵称称号双行",
                "开启后群聊昵称与称号分两行显示，关闭时即使显示头像也保持单行",
                Settings.nickTitleTwoLine
            )
            switch(
                "启用附加菜单",
                "开启后使用加号按钮代替第二页",
                Settings.enableExtraMenu
            )
            switch(
                "隐藏语音键",
                "开启后聊天页面不再显示语音按钮",
                Settings.hideVoiceButton
            )
            switch(
                "识别无前缀链接",
                "识别 example.com 这类没有 http 前缀的链接",
                Settings.wideUrlMatch
            )
            switch(
                "链接预览卡片",
                "在消息下方展示链接预览，会向链接站点发起请求",
                Settings.enableLinkPreview
            )
            switch(
                "群聊显示头像",
                "在群聊昵称左侧显示发送者头像，会从头像服务器拉取图片",
                Settings.showGroupAvatar
            )
            floatInput(
                "头像大小",
                "群聊头像相对昵称文字的倍数，默认 2.4",
                Settings.avatarSizeScale
            )
            floatInput(
                "图片最大高度",
                "聊天图片最大显示高度(占屏幕高度比例)，默认 0.5",
                Settings.picMaxHeightRatio
            )
            switch(
                "合并连续消息头",
                "同一人连续发言时只在第一条显示头像和昵称",
                Settings.hideRepeatedSender
            )
            saveButton?.let { add(it) }
            add<View>()
                .height(64.dp)
        }
    }

    private fun GroupScope.sectionTitle(title: String) {
        add<TextView>()
            .text(title)
            .textSize(12f)
            .textColor(0xFF_FFFFFF)
            .padding(left = 8.dp, top = 0, right = 8.dp, bottom = 0)
            .margin(left = 4.dp, top = 10.dp, right = 4.dp, bottom = 2.dp)
            .apply {
                alpha = 0.72f
            }
    }

    private fun GroupScope.switch(
        title: String,
        desc: String = "",
        pref: Pref<Boolean>
    ) {
        baseEntry(title, desc) {
            add(
                Switch(group.context, null).apply {
                    layoutParams = LinearLayout.LayoutParams(WRAP, WRAP)
                    isChecked = pref.value
                    setOnCheckedChangeListener { _, checked ->
                        pref.value = checked
                    }
                }
            )
        }
    }

    private fun GroupScope.floatInput(
        title: String,
        desc: String = "",
        pref: Pref<Float>
    ) {
        baseEntry(title, desc) {
            add<EditText>()
                .text(pref.value.toString())
                .width(70.dp)
                .textSize(12f)
                .textColor(0xFF_FFFFFF)
                .gravity(Gravity.CENTER)
                .hint("支持小数")
                .background(roundCornerDrawable(0x44_000000, 8.dpf))
                .padding(left = 6.dp, top = 4.dp, right = 6.dp, bottom = 4.dp)
                .apply {
                    isSingleLine = true
                    inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                }
                .doAfterTextChanged {
                    it?.toString()?.toFloatOrNull()?.let { value ->
                        pref.value = value
                    }
                }
        }
    }

    private fun GroupScope.actionEntry(
        title: String,
        desc: String = "",
        onClick: () -> Unit
    ) {
        val row = baseEntry(title, desc) {
            add<TextView>()
                .text("进入")
                .textSize(12f)
                .textColor(0xFF_FFFFFF)
                .padding(left = 10.dp, top = 5.dp, right = 10.dp, bottom = 5.dp)
                .background(roundCornerDrawable(0x44_000000, 8.dpf))
                .clickable(onClick)
        }
        row.clickable(onClick)
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
                                .apply {
                                    alpha = 0.88f
                                }
                        }
                    }
                content.invoke(this)
            }
    }
}
