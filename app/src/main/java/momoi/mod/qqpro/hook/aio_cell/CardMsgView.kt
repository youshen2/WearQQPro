package momoi.mod.qqpro.hook.aio_cell

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.tencent.qqnt.kernel.nativeinterface.ArkElement
import loadPicUrl
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.hook.style.heightLimit
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.adjustViewBounds
import momoi.mod.qqpro.lib.background
import momoi.mod.qqpro.lib.clickable
import momoi.mod.qqpro.lib.content
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.gravity
import momoi.mod.qqpro.lib.margin
import momoi.mod.qqpro.lib.marginVertical
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.scaleType
import momoi.mod.qqpro.lib.size
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize
import momoi.mod.qqpro.lib.vertical
import momoi.mod.qqpro.lib.width
import momoi.mod.qqpro.util.Json
import momoi.mod.qqpro.util.Utils
import moye.wear.hook.LinkText
import moye.wear.hook.openAddSearch

class CardMsgView(context: Context) : LinearLayout(context) {
    private lateinit var mGeneric: LinearLayout
    private lateinit var mTvTitle: TextView
    private lateinit var mTvTag: TextView
    private lateinit var mTvDesc: TextView
    private lateinit var mIvIcon: ImageView
    private lateinit var mIvPreview: ImageView
    private lateinit var mContact: LinearLayout
    private lateinit var mIvAvatar: ImageView
    private lateinit var mTvCardNick: TextView
    private lateinit var mTvCardAccount: TextView
    private lateinit var mTvCardTag: TextView

    init {
        vertical()
        padding(2.dp)
        content {
            mGeneric = add<LinearLayout>().vertical().width(FILL).content {
                add<LinearLayout>()
                    .width(FILL)
                    .gravity(Gravity.CENTER_VERTICAL)
                    .content {
                        val iconSize = (28f * Settings.chatScale.value).toInt().dp
                        mIvIcon = add<ImageView>()
                            .size(iconSize)
                            .scaleType(ImageView.ScaleType.FIT_CENTER)
                        add<LinearLayout>().vertical().weight(1f).margin(left = 4.dp).content {
                            mTvTitle = add<TextView>()
                                .textSize(12f * Settings.chatScale.value)
                                .textColor(0xFF_FFFFFF.toInt())
                            mTvDesc = add<TextView>()
                                .textSize(10f * Settings.chatScale.value)
                                .textColor(0xFF_CCCCCC.toInt())
                        }
                    }
                add<View>()
                    .size(width = FILL, height = 1)
                    .background(0xFF_AAAAAA.toInt())
                    .marginVertical(1.dp)
                mIvPreview = add<ImageView>()
                    .width(FILL)
                    .scaleType(ImageView.ScaleType.FIT_CENTER)
                    .adjustViewBounds()
                    .apply { maxHeight = heightLimit.toInt() }
                mTvTag = add<TextView>()
                    .width(FILL)
                    .textSize(9f * Settings.chatScale.value)
                    .textColor(0xFF_FFFFFF.toInt())
                    .text(" ")
            }
            mContact = add<LinearLayout>().width(FILL).gravity(Gravity.CENTER_VERTICAL).content {
                val avatarSize = (36f * Settings.chatScale.value).toInt().dp
                mIvAvatar = add<ImageView>()
                    .size(avatarSize)
                    .scaleType(ImageView.ScaleType.CENTER_CROP)
                add<LinearLayout>().vertical().weight(1f).margin(left = 6.dp).content {
                    mTvCardNick = add<TextView>()
                        .textSize(13f * Settings.chatScale.value)
                        .textColor(0xFF_FFFFFF.toInt())
                    mTvCardAccount = add<TextView>()
                        .textSize(10f * Settings.chatScale.value)
                        .textColor(0xFF_CCCCCC.toInt())
                    mTvCardTag = add<TextView>()
                        .textSize(9f * Settings.chatScale.value)
                        .textColor(0xFF_AAAAAA.toInt())
                }
            }
            mContact.visibility = GONE
        }
    }

    fun loadData(ark: ArkElement) {
        try {
            val json = Json(ark.bytesData)
            val meta = json.json("meta")!!
            if (json.str("app") == "com.tencent.contact.lua") {
                mGeneric.visibility = GONE
                mContact.visibility = VISIBLE
                loadContactCard(json, meta)
                return
            }
            mContact.visibility = GONE
            mGeneric.visibility = VISIBLE
            val data = meta.json(meta.keys.first())!!
            val desc = data.str("desc")
            val title = data.str("title")!!
            mTvTitle.text = title
            mTvDesc.visibility = VISIBLE
            mTvDesc.text = desc
            val icon = data.str("icon") ?: data.str("tagIcon")
            if (!icon.isNullOrEmpty()) {
                mIvIcon.visibility = VISIBLE
                mIvIcon.loadPicUrl(icon)
            } else {
                mIvIcon.visibility = GONE
            }
            val tag = data.str("tag")
            if (!tag.isNullOrEmpty()) {
                mTvTag.visibility = VISIBLE
                mTvTag.text = tag
            } else {
                mTvTag.visibility = GONE
            }
            val preview = data.str("preview")
            if (!preview.isNullOrEmpty() && json.str("view") != "news") {
                mIvPreview.visibility = VISIBLE
                mIvPreview.loadPicUrl(preview)
            } else {
                mIvPreview.visibility = GONE
            }
            clickable {
                (data.str("jumpUrl") ?: data.str("qqdocurl"))?.let {
                    Utils.openUrl(LinkText.withScheme(it))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadContactCard(json: Json, meta: Json) {
        try {
            val contact = meta.json("contact") ?: return
            mTvCardNick.text = contact.str("nickname") ?: json.str("prompt") ?: "[名片]"
            val account = contact.str("contact")
            if (!account.isNullOrEmpty()) {
                mTvCardAccount.visibility = VISIBLE
                mTvCardAccount.text = account
            } else {
                mTvCardAccount.visibility = GONE
            }
            val tag = contact.str("tag")
            if (!tag.isNullOrEmpty()) {
                mTvCardTag.visibility = VISIBLE
                mTvCardTag.text = tag
            } else {
                mTvCardTag.visibility = GONE
            }
            val avatar = contact.str("avatar")
            if (!avatar.isNullOrEmpty()) {
                mIvAvatar.loadPicUrl(avatar)
            }
            val uin = (account?.let { Regex("\\d{5,}").find(it)?.value }
                ?: contact.str("jumpUrl")?.let { Regex("uin=(\\d+)").find(it)?.groupValues?.get(1) })
            if (!uin.isNullOrEmpty()) {
                isClickable = true
                setOnClickListener { openAddSearch(uin, type = 1) }
            } else {
                setOnClickListener(null)
                isClickable = false
            }
        } catch (e: Exception) {
            Utils.log("CardMsgView contact card error: ${e.message}")
        }
    }
}
