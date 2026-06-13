package moye.wear.hook

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import momoi.mod.qqpro.drawable.roundCornerDrawable
import momoi.mod.qqpro.hook.view.MyDialogFragment
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.LinearScope
import momoi.mod.qqpro.lib.WRAP
import momoi.mod.qqpro.lib.background
import momoi.mod.qqpro.lib.content
import momoi.mod.qqpro.lib.create
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.dpf
import momoi.mod.qqpro.lib.gravity
import momoi.mod.qqpro.lib.layoutParams
import momoi.mod.qqpro.lib.margin
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.size
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize
import momoi.mod.qqpro.lib.vertical
import momoi.mod.qqpro.lib.width
import momoi.mod.qqpro.util.Utils
import java.net.URI

/**
 * 链接跳转确认弹窗。
 *
 * 没有直接用系统 AlertDialog —— 手表那块被强行改过 DPI 的圆屏上，
 * 带窗口边框的 Dialog 会渲染得很奇怪，所以这里沿用项目里 [MyDialogFragment] 的全屏方案，
 * 自己用 DSL 拼一个居中卡片。
 *
 * 布局采用"上下分区"：上半区展示链接信息（域名做主标题、完整链接小字可滚动），
 * 下半区是两个上下堆叠的整宽按钮（打开在上、取消在下），方便手指点按。
 */
class LinkConfirmFragment(private val url: String) : MyDialogFragment() {

    /** 限高滚动容器：链接很长时滚动，不会无限撑高把按钮挤出可视区。 */
    private class BoundedScrollView(context: Context, private val maxH: Int) : ScrollView(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(maxH, MeasureSpec.AT_MOST)
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = inflater.context
        // 圆屏左右留更多边距，保证卡片不被圆角裁掉
        val sideMargin = if (Utils.isRoundScreen) 26.dp else 16.dp
        val host = runCatching { URI(LinkText.withScheme(url)).host }
            .getOrNull()?.takeIf { it.isNotBlank() } ?: "未知来源"

        return create<LinearLayout>(ctx)
            .size(FILL, FILL)
            .vertical()
            .gravity(Gravity.CENTER)
            .padding(left = sideMargin, top = 0, right = sideMargin, bottom = 0)
            // 点击卡片外的空白处直接关闭
            .apply { setOnClickListener { dismiss() } }
            .content {
                add<LinearLayout>()
                    .width(FILL)
                    .vertical()
                    .background(roundCornerDrawable(0xFF_222428.toInt(), 20.dpf))
                    .padding(left = 14.dp, top = 16.dp, right = 14.dp, bottom = 12.dp)
                    // 拦截卡片自身的点击，免得穿透到外层把弹窗关掉
                    .apply { setOnClickListener { } }
                    .content {
                        // ---- 上半区：链接信息 ----
                        add<TextView>()
                            .width(FILL)
                            .text("是否打开链接")
                            .textSize(11f)
                            .textColor(0xFF_8A9099.toInt())
                            .gravity(Gravity.CENTER)

                        // 域名作主标题，醒目
                        add<TextView>()
                            .width(FILL)
                            .text(host)
                            .textSize(16f)
                            .textColor(0xFF_FFFFFF.toInt())
                            .gravity(Gravity.CENTER)
                            .margin(top = 4.dp)
                            .apply {
                                maxLines = 1
                                ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
                            }

                        // 完整链接，小字、限高可滚动
                        val scroll = BoundedScrollView(ctx, Utils.heightPixels / 4)
                            .layoutParams(
                                LinearLayout.LayoutParams(FILL, WRAP).apply {
                                    topMargin = 6.dp
                                    bottomMargin = 14.dp
                                }
                            )
                        scroll.content {
                            add<TextView>()
                                .width(FILL)
                                .text(url)
                                .textSize(11f)
                                .textColor(0xFF_7FA8E6.toInt())
                                .gravity(Gravity.CENTER)
                        }
                        add(scroll)

                        // ---- 下半区：上下堆叠的整宽按钮 ----
                        actionButton("打开", 0xFF_2F6BD8.toInt(), 0xFF_FFFFFF.toInt()) {
                            dismiss()
                            Utils.openUrl(LinkText.withScheme(url))
                        }
                        actionButton("取消", 0xFF_33363B.toInt(), 0xFF_C8CDD4.toInt()) {
                            dismiss()
                        }
                    }
            }
    }

    private fun LinearScope.actionButton(
        label: String,
        bgColor: Int,
        textColor: Int,
        onClick: () -> Unit
    ) {
        add<TextView>()
            .width(FILL)
            .text(label)
            .textSize(14f)
            .textColor(textColor)
            .gravity(Gravity.CENTER)
            .padding(top = 11.dp, bottom = 11.dp)
            .margin(top = 8.dp)
            .background(roundCornerDrawable(bgColor, 14.dpf))
            .apply { setOnClickListener { onClick() } }
    }
}
