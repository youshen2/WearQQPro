package momoi.mod.qqpro.hook.view

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.clickable
import momoi.mod.qqpro.lib.content
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.gravity
import momoi.mod.qqpro.lib.margin
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize
import momoi.mod.qqpro.lib.vertical
import momoi.mod.qqpro.lib.width
import momoi.mod.qqpro.util.Utils

private val ACCENT = 0xFF_4FC3F7.toInt()

class CallConfirmFragment(
    private val title: String,
    private val action: View,
    private val onConfirm: (() -> Unit)? = null,
) : MyDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = inflater.context
        val root = LinearLayout(ctx)
            .vertical()
            .padding(20.dp)
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(0xF0_121212.toInt())

        root.content {
            add<TextView>()
                .text(title)
                .textSize(16f)
                .textColor(0xFF_FFFFFF)
                .gravity(Gravity.CENTER)
                .padding(bottom = 16.dp)

            button("确定", ACCENT, 0xFF_000000.toInt()) {
                dismiss()
                if (onConfirm != null) {
                    onConfirm.invoke()
                } else {
                    action.performClick()
                }
            }
            button("取消", 0xFF_2A2A2A.toInt(), 0xFF_FFFFFF.toInt()) {
                dismiss()
            }
        }
        return root
    }

    private fun momoi.mod.qqpro.lib.LinearScope.button(
        label: String,
        bg: Int,
        fg: Int,
        onClick: () -> Unit
    ) {
        add<TextView>()
            .text(label)
            .textSize(14f)
            .textColor(fg)
            .gravity(Gravity.CENTER)
            .width(FILL)
            .padding(top = 10.dp, bottom = 10.dp)
            .apply {
                background = GradientDrawable().apply {
                    setColor(bg)
                    cornerRadius = 22.dp.toFloat()
                }
            }
            .margin(top = 6.dp)
            .clickable(onClick)
    }
}
