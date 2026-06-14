package momoi.mod.qqpro.hook.view

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Selection
import android.text.Spannable
import android.text.method.ArrowKeyMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.util.Utils
import moye.wear.lib.SwipeBackLayout

private val PARTIAL_COPY_BG = 0xF0_121212.toInt()

class PartialCopyFragment(private val content: String) : MyDialogFragment() {

    constructor() : this("")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = inflater.context
        val edgePadding = if (Utils.isRoundScreen) 16.dp else 8.dp
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(PARTIAL_COPY_BG)
            setPadding(edgePadding, 6.dp, edgePadding, 6.dp)
        }

        root.addView(
            TextView(context).apply {
                text = "长按选择要复制"
                textSize = 10f
                setTextColor(0xFF_999999.toInt())
                gravity = Gravity.CENTER
                setPadding(0, 2.dp, 0, 4.dp)
            },
            LinearLayout.LayoutParams(FILL, ViewGroup.LayoutParams.WRAP_CONTENT)
        )

        val body = TextView(context).apply {
            text = content
            textSize = 13f
            setTextColor(0xFF_EEEEEE.toInt())
            setPadding(8.dp, 6.dp, 8.dp, 6.dp)
            setTextIsSelectable(true)
            movementMethod = ArrowKeyMovementMethod.getInstance()
            background = GradientDrawable().apply {
                setColor(0xFF_1C1C1C.toInt())
                cornerRadius = 8.dp.toFloat()
            }
        }
        root.addView(
            ScrollView(context).apply {
                addView(body, ViewGroup.LayoutParams(FILL, ViewGroup.LayoutParams.WRAP_CONTENT))
            },
            LinearLayout.LayoutParams(FILL, 0, 1f)
        )

        val bar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        root.addView(
            bar,
            LinearLayout.LayoutParams(FILL, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 4.dp
            }
        )
        addButton(bar, "全选", 0xFF_2A2A2A.toInt(), 0xFF_FFFFFF.toInt()) {
            body.requestFocus()
            (body.text as? Spannable)?.let {
                Selection.setSelection(it, 0, it.length)
            }
        }
        addButton(bar, "关闭", 0xFF_1A1A1A.toInt(), 0xFF_999999.toInt()) {
            dismiss()
        }

        return SwipeBackLayout(context).apply {
            addView(root, FILL, FILL)
            onSwipeBack = { dismiss() }
        }
    }

    private fun addButton(
        bar: LinearLayout,
        label: String,
        backgroundColor: Int,
        textColor: Int,
        onClick: () -> Unit
    ) {
        bar.addView(
            TextView(bar.context).apply {
                text = label
                textSize = 12f
                setTextColor(textColor)
                gravity = Gravity.CENTER
                setPadding(0, 6.dp, 0, 6.dp)
                background = GradientDrawable().apply {
                    setColor(backgroundColor)
                    cornerRadius = 16.dp.toFloat()
                }
                setOnClickListener { onClick() }
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (bar.childCount > 0) {
                    marginStart = 6.dp
                }
            }
        )
    }
}
