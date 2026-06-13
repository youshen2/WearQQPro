package momoi.mod.qqpro.hook.aio_cell

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tencent.qqnt.kernel.api.impl.MsgService
import com.tencent.qqnt.kernel.nativeinterface.Contact
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.tencent.qqnt.kernel.nativeinterface.PicElement
import com.tencent.qqnt.msg.KernelServiceUtil
import com.tencent.richframework.widget.matrix.RFWMatrixImageView
import loadPicElement
import momoi.mod.qqpro.MsgUtil
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.hook.style.MyImageView
import momoi.mod.qqpro.hook.view.MyDialogFragment
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.background
import momoi.mod.qqpro.lib.clickable
import momoi.mod.qqpro.lib.content
import momoi.mod.qqpro.lib.create
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.find
import momoi.mod.qqpro.lib.gravity
import momoi.mod.qqpro.lib.id
import momoi.mod.qqpro.lib.layoutParams
import momoi.mod.qqpro.lib.linearLayout
import momoi.mod.qqpro.lib.margin
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.paddingHorizontal
import momoi.mod.qqpro.lib.size
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize
import momoi.mod.qqpro.lib.vertical
import momoi.mod.qqpro.lib.width
import momoi.mod.qqpro.removeAfter
import momoi.mod.qqpro.showDialog
import momoi.mod.qqpro.util.linkify
import momoi.mod.qqpro.util.runOnUi

class BigImageFragment(private val pic: PicElement) : MyDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FrameLayout(inflater.context)
            .content {
                add(
                    RFWMatrixImageView(inflater.context, null)
                        .layoutParams(ViewGroup.LayoutParams(FILL, FILL))
                        .loadPicElement(pic)
                )
                add<View>()
                    .size(FILL, 12.dp)
                    .clickable {
                        this@BigImageFragment.dismiss()
                    }
            }
    }
}

class DetailFragment(private val contact: Contact, private val data: ForwardMsgData) :
    MyDialogFragment() {
    private val mMsgList = mutableListOf<MsgRecord>()
    private lateinit var mRv: RecyclerView

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        data.getDetail {
            mMsgList.clear()
            mMsgList.addAll(it)
            runOnUi {
                mRv.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return create<LinearLayout>(inflater.context)
            .vertical()
            .size(FILL, FILL)
            .background(0x77_000000)
            .paddingHorizontal(4.dp)
            .content {
                add<TextView>()
                    .text(data.title)
                    .textSize(13f)
                    .width(FILL)
                    .gravity(Gravity.CENTER)
                    .textColor(0xFF_FFFFFF.toInt())
                    .clickable { dismiss() }
                mRv = add<RecyclerView>()
                    .linearLayout()
                    .layoutParams(LinearLayout.LayoutParams(FILL, 0, 1f))
                    .content(
                        data = mMsgList,
                        factory = {
                            create<LinearLayout>(this)
                                .vertical()
                                .width(FILL)
                                .content {
                                    add<TextView>()
                                        .textSize(12f)
                                        .textColor(0xFF_999999.toInt())
                                        .id(0)
                                    add<LinearLayout>()
                                        .vertical()
                                        .padding(3.dp)
                                        .margin(bottom = 2.dp)
                                        .id(1)
                                }
                        },
                        update = { msg ->
                            find<TextView>(0).text(msg.sendNickName)
                            find<LinearLayout>(1)
                                .apply {
                                    removeAllViews()
                                }
                                .content {
                                    val textElements = mutableListOf<MsgElement>()
                                    val applyTexts = {
                                        if (textElements.isNotEmpty()) {
                                            group.background(0xFF_515151.toInt())
                                            add<TextView>()
                                                .textSize(14f * Settings.chatScale.value)
                                                .textColor(0xFF_FFFFFF.toInt())
                                                .text(MsgUtil.summary(textElements))
                                                .linkify()
                                            textElements.clear()
                                        }
                                    }
                                    msg.elements.forEach { ele ->
                                        ele.replyElement?.let {
                                            group.background(0xFF_515151.toInt())
                                            add<ReplyView>()
                                                .loadData(contact, it)
                                            return@forEach
                                        }
                                        ele.multiForwardMsgElement?.let {
                                            group.background(0xFF_515151.toInt())
                                            add<ForwardMsgView>()
                                                .loadData(
                                                    contact,
                                                    ForwardMsgData(contact, data.rootMsg, msg)
                                                )
                                            return@forEach
                                        }
                                        ele.picElement?.let {
                                            applyTexts()
                                            add<MyImageView>()
                                                .size(it.picWidth, it.picHeight)
                                                .clickable {
                                                    showDialog(BigImageFragment(it))
                                                }
                                                .loadPicElement(it)
                                            return@forEach
                                        }
                                        textElements.add(ele)
                                    }
                                    applyTexts()
                                }
                        })
            }
    }
}

class ForwardMsgData(val contact: Contact, val rootMsg: MsgRecord, val rawMsg: MsgRecord) {
    val title: String
    val previewLines: List<String>
    val summary: String

    init {
        val content =
            rawMsg.elements?.firstNotNullOf { it.multiForwardMsgElement }
                ?.xmlContent
                ?.replace("&lt;", "<")
                ?.replace("&gt;", ">")
                ?.replace("&amp;", "&")
                ?.replace("&quot;", "\"")
                ?.replace("&apos;", "'")
        val split = content?.split("</title>")?.map {
            it.split(">").last()
        }
        title = split?.getOrNull(0) ?: ""
        previewLines = split?.drop(1) ?: listOf()
        summary = content?.removeAfter("</summary>")?.split(">")?.last() ?: ""
    }

    fun getDetail(callback: (List<MsgRecord>) -> Unit) {
        (KernelServiceUtil.c() as? MsgService)?.service?.getMultiMsg(
            contact, rootMsg.msgId, rawMsg.msgId
        ) { i: Int, s: String, msgRecords: ArrayList<MsgRecord> ->
            callback(msgRecords)
        }
    }
}