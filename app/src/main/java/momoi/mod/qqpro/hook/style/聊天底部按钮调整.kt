package momoi.mod.qqpro.hook.style

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.forEach
import com.tencent.mobileqq.app.ThreadManagerV2
import com.tencent.watch.aio_impl.coreImpl.vb.`InputBarController$inputContent$2`
import com.tencent.watch.aio_impl.coreImpl.vb.InputBarControllerKt
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.util.Utils
import momoi.mod.qqpro.asGroup
import momoi.mod.qqpro.drawable.roundCornerDrawable
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.GroupScope
import momoi.mod.qqpro.lib.adjustViewBounds
import momoi.mod.qqpro.lib.background
import momoi.mod.qqpro.lib.bitmapDecodeAssets
import momoi.mod.qqpro.lib.clickable
import momoi.mod.qqpro.lib.content
import momoi.mod.qqpro.lib.create
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.gravity
import momoi.mod.qqpro.lib.height
import momoi.mod.qqpro.lib.imageResource
import momoi.mod.qqpro.lib.margin
import momoi.mod.qqpro.lib.marginHorizontal
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.paddingHorizontal
import momoi.mod.qqpro.lib.scaleType
import momoi.mod.qqpro.lib.size
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize
import moye.wear.hook.ExtraMenuOverlay

@Mixin
class 聊天底部按钮调整() : `InputBarController$inputContent$2`() {
    @SuppressLint("ResourceType", "ClickableViewAccessibility")
    override fun invoke(): Any = (super.invoke() as ConstraintLayout).apply {
        forEach {
            it.visibility = View.INVISIBLE
        }
        val emoji = getChildAt(0)
        val keyboard = getChildAt(2)
        GroupScope(this).apply {
            val roundBg = roundCornerDrawable(0xFF_1B9AF7.toInt(), 9999f)
            add<LinearLayout>().size(FILL, FILL).apply {
                    if (Utils.isRoundScreen) {
                        paddingHorizontal((14.dp / Settings.scale.value).toInt())
                    }
                }.content {
                    add<ImageView>().height(FILL).adjustViewBounds()
                        .scaleType(ImageView.ScaleType.FIT_CENTER).background(roundBg)
                        .bitmapDecodeAssets("pro/ic_emoji.png").padding(8.dp).clickable {
                            emoji.callOnClick()
                        }
                    val voice = if (Settings.hideVoiceButton.value) {
                        null
                    } else {
                        create<ImageView>().height(FILL).adjustViewBounds().background(roundBg)
                            .bitmapDecodeAssets("pro/ic_voice.png").padding(6.dp)
                            .scaleType(ImageView.ScaleType.FIT_CENTER).also {
                                ThreadManagerV2.getUIHandlerV2().post {
                                    b.e.invoke(it)
                                }
                            }
                    }
                    val input = if (Settings.text.isEmpty()) {
                        create<ImageView>().bitmapDecodeAssets("pro/ic_keyboard.png")
                            .scaleType(ImageView.ScaleType.FIT_CENTER).padding(8.dp)
                    } else {
                        create<TextView>().gravity(Gravity.CENTER).textSize(14f)
                            .textColor(0xFF_FFFFFF).text(Settings.text)
                    }.height(FILL).weight(1f)
                        .background(ContextCompat.getDrawable(context, 2114457248)).clickable {
                            keyboard.callOnClick()
                        }
                    val extraMenu = if (Settings.enableExtraMenu.value) {
                        create<ImageView>().height(FILL).adjustViewBounds().background(roundBg)
                            .imageResource(0x7e080564).padding(8.dp)
                            .scaleType(ImageView.ScaleType.FIT_CENTER).clickable {
                                ExtraMenuOverlay.toggleFromCurrent()
                            }
                    } else {
                        null
                    }

                    if (Settings.swapCenterKeyboard.value) {
                        add(input.marginHorizontal(2.dp))
                        voice?.let { add(it) }
                        extraMenu?.let { add(it.margin(left = 2.dp)) }
                    } else {
                        voice?.let {
                            add(it.marginHorizontal(2.dp))
                            add(input)
                        } ?: add(input.marginHorizontal(2.dp))
                        extraMenu?.let { add(it.margin(left = 2.dp)) }
                    }
                }
        }
    }
}
