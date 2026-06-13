package momoi.mod.qqpro.hook.aio_cell

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.tencent.biz.richframework.util.RFWSaveUtil
import com.tencent.mobileqq.qroute.QRoute
import com.tencent.qqnt.kernel.api.impl.MsgService
import com.tencent.qqnt.kernel.nativeinterface.AddFavEmojiReq
import com.tencent.qqnt.kernel.nativeinterface.Contact
import com.tencent.qqnt.kernel.nativeinterface.IAddFavEmojiCallback
import com.tencent.qqnt.kernel.nativeinterface.IOperateCallback
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.tencent.qqnt.kernel.nativeinterface.PicElement
import com.tencent.qqnt.kernel.nativeinterface.TextElement
import com.tencent.qqnt.msg.KernelServiceUtil
import com.tencent.qqnt.watch.api.IMsgApi
import com.tencent.qqnt.watch.contact.FriendSelectData
import com.tencent.qqnt.watch.contact.api.IContactRuntimeService
import com.tencent.richframework.widget.matrix.RFWMatrixImageView
import com.tencent.watch.aio_impl.ui.menu.AIOLongClickMenuFragment
import com.tencent.watch.ime.util.ImeTextUtil
import com.tencent.watch.aio_impl.ui.menu.MenuItemFactory
import loadPicElement
import download
import mqq.app.MobileQQ
import momoi.mod.qqpro.enums.ElementType
import momoi.mod.qqpro.MsgUtil
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.child
import momoi.mod.qqpro.hook.action.CurrentContact
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
import momoi.mod.qqpro.lib.longClickable
import momoi.mod.qqpro.lib.margin
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.paddingHorizontal
import momoi.mod.qqpro.lib.size
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize
import momoi.mod.qqpro.lib.vertical
import momoi.mod.qqpro.lib.width
import momoi.mod.qqpro.msg.getImageUrl
import momoi.mod.qqpro.removeAfter
import momoi.mod.qqpro.showDialog
import momoi.mod.qqpro.showFragment
import momoi.mod.qqpro.util.Utils
import momoi.mod.qqpro.util.linkify
import momoi.mod.qqpro.util.runOnUi
import java.io.File
import java.security.MessageDigest

class BigImageFragment(private val msgId: Long, private val pic: PicElement) : MyDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FrameLayout(inflater.context)
            .content {
                val image = RFWMatrixImageView(inflater.context, null)
                    .layoutParams(ViewGroup.LayoutParams(FILL, FILL))
                    .loadPicElement(pic)
                add(image)
                image.longClickable {
                    image.showHistoryMenu(
                        msgId,
                        listOf(
                            MENU_SAVE_FAV_EMOJI,
                            MENU_SHARE,
                            MENU_SAVE_PIC,
                        )
                    ) { item ->
                        when (item) {
                            MENU_SAVE_FAV_EMOJI -> saveFavEmoji(image.context, pic)
                            MENU_SHARE -> image.sharePic(image.context, pic)
                            MENU_SAVE_PIC -> savePic(image.context, pic)
                            else -> {}
                        }
                    }
                }
                add<View>()
                    .size(FILL, 12.dp)
                    .clickable {
                        this@BigImageFragment.dismiss()
                    }
            }
    }
}

private val MENU_COPY = MenuItemFactory.ItemEnum.valueOf("CopyMsg")
private val MENU_REPEAT = MenuItemFactory.ItemEnum.valueOf("RepeatMsg")
private val MENU_SAVE_PIC = MenuItemFactory.ItemEnum.valueOf("SavePic")
private val MENU_SAVE_FAV_EMOJI = MenuItemFactory.ItemEnum.valueOf("SaveFavEmoji")
private val MENU_SHARE = MenuItemFactory.ItemEnum.valueOf("Share")

private fun View.showHistoryMenu(
    msgId: Long,
    items: List<MenuItemFactory.ItemEnum>,
    onItem: (MenuItemFactory.ItemEnum) -> Unit,
) {
    val fragment = AIOLongClickMenuFragment({ onItem(it) }, "pg_watch_long_press_menu")
    fragment.arguments = Bundle().apply {
        putLong("key_msg_id", msgId)
        putStringArrayList("key_item_list", ArrayList(items.map { it.name }))
    }
    showFragment(fragment)
}

private fun copyText(context: Context, text: CharSequence) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("text", text))
}

private fun savePic(context: Context, pic: PicElement) {
    val cacheFile = context.externalCacheDir!!.child("${pic.md5HexStr}.jpg")
    if (cacheFile.exists()) {
        RFWSaveUtil.a(context, cacheFile.path, null)
    } else {
        download(pic.getImageUrl(), cacheFile) { ok ->
            if (ok) {
                runOnUi {
                    RFWSaveUtil.a(context, cacheFile.path, null)
                }
            } else {
                Utils.log("savePic: download failed for ${pic.md5HexStr}")
            }
        }
    }
}

private fun picLocalPath(context: Context, pic: PicElement): String? {
    runCatching { WatchPicElementExtKt.C0(pic) }
        .getOrNull()
        ?.takeIf { it.isNotEmpty() && File(it).exists() }
        ?.let { return it }
    val cacheFile = context.externalCacheDir!!.child("${pic.md5HexStr}.jpg")
    return cacheFile.takeIf { it.exists() }?.path
}

private fun fileMd5(file: File): String {
    val digest = MessageDigest.getInstance("MD5")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val len = input.read(buffer)
            if (len < 0) {
                break
            }
            digest.update(buffer, 0, len)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun saveFavEmoji(context: Context, pic: PicElement) {
    val cacheFile = context.externalCacheDir!!.child("${pic.md5HexStr}.jpg")
    if (cacheFile.exists()) {
        doAddFavEmoji(context, cacheFile)
    } else {
        download(pic.getImageUrl(), cacheFile) { ok ->
            if (ok) {
                runOnUi {
                    doAddFavEmoji(context, cacheFile)
                }
            } else {
                Utils.log("saveFavEmoji: download failed for ${pic.md5HexStr}")
            }
        }
    }
}

private fun doAddFavEmoji(context: Context, file: File) {
    val req = AddFavEmojiReq("", 0, file.path, file.length(), file.name, fileMd5(file), false, true)
    val msgService = WatchPicElementExtKt.r0().wrapperSession?.msgService
    if (msgService == null) {
        Utils.log("saveFavEmoji: msgService null")
        return
    }
    msgService.addFavEmoji(req, IAddFavEmojiCallback { code, msg, type ->
        Utils.log("saveFavEmoji result=$code msg=$msg type=$type")
        runOnUi {
            Toast.makeText(
                context,
                if (type == 1) "表情已存在" else if (code == 0) "收藏表情成功" else "收藏表情失败",
                Toast.LENGTH_SHORT
            ).show()
        }
    })
}

/**
 * 打开 QQ 的好友选择器，把构建好的消息元素转发到所选的会话中。
 * 与「复读」(RepeatMsg) 不同，这里是真正转发到其它会话，而不是在当前会话重发。
 * 混淆字段说明：FriendSelectData.b = uid，FriendSelectData.e = 是否群聊。
 * 0x7e0805cd = R.drawable.icon_share。
 */
private fun View.forwardToFriends(title: String = "转发", buildElements: () -> ArrayList<MsgElement>) {
    val navFragment = WatchPicElementExtKt.W(this)?.let { WatchPicElementExtKt.Y(it) } ?: return
    val app = MobileQQ.getMobileQQ().peekAppRuntime() ?: return
    val contactService =
        app.getRuntimeService(IContactRuntimeService::class.java, "") as? IContactRuntimeService
            ?: return
    contactService.startFriendSelect(
        navFragment,
        emptyList(),
        arrayListOf(app.currentUid),
        title,
        0x7e0805cd,
        1,
        10,
        null,
        false,
        true
    ) { _, friends: List<FriendSelectData> ->
        if (friends.isNotEmpty()) {
            val elements = buildElements()
            friends.forEach { friend ->
                val target = Contact(if (friend.e) 2 else 1, friend.b, "")
                MsgUtil.msgService.sendMsg(
                    target,
                    0L,
                    elements,
                    IOperateCallback { code, msg ->
                        Utils.log("forward send result=$code msg=$msg")
                    }
                )
            }
        }
    }
}

/** 把一段文本转发给所选的好友/群聊。 */
fun View.forwardText(text: CharSequence) = forwardToFriends {
    ImeTextUtil.a.b(text.toString())
}

/** 把图片转发给所选的好友/群聊（转发）。 */
private fun View.sharePic(context: Context, pic: PicElement) {
    val path = picLocalPath(context, pic) ?: run {
        Utils.log("sharePic: no local path for ${pic.md5HexStr}")
        return
    }
    forwardToFriends {
        arrayListOf(QRoute.api(IMsgApi::class.java).createPicElement(path, 0))
    }
}

private fun repeatText(text: CharSequence) {
    runCatching {
        val element = MsgElement().apply {
            elementType = ElementType.TEXT
            textElement = TextElement().apply {
                content = text.toString()
            }
        }
        val contact = Contact(CurrentContact.chatType, CurrentContact.peerUid, CurrentContact.guildId)
        MsgUtil.msgService.sendMsg(contact, 0L, arrayListOf(element), IOperateCallback { code, msg ->
            Utils.log("history repeat send result=$code msg=$msg")
        })
    }.onFailure {
        Utils.log("history repeat failed: $it")
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
                                            val summary = MsgUtil.summary(textElements)
                                            val summaryView = add<TextView>()
                                                .textSize(14f * Settings.chatScale.value)
                                                .textColor(0xFF_FFFFFF.toInt())
                                                .text(summary)
                                                .apply {
                                                    linkify()
                                                }
                                            summaryView.longClickable {
                                                summaryView.showHistoryMenu(
                                                    msg.msgId,
                                                    listOf(MENU_COPY, MENU_REPEAT, MENU_SHARE)
                                                ) { item ->
                                                    when (item) {
                                                        MENU_COPY -> copyText(group.context, summary)
                                                        MENU_REPEAT -> repeatText(summary)
                                                        MENU_SHARE -> summaryView.forwardText(summary)
                                                        else -> {}
                                                    }
                                                }
                                            }
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
                                        ele.picElement?.let { pic ->
                                            applyTexts()
                                            add<FrameLayout>().content {
                                                val image = add<MyImageView>()
                                                    .size(pic.picWidth, pic.picHeight)
                                                    .clickable {
                                                        showDialog(BigImageFragment(msg.msgId, pic))
                                                    }
                                                image.longClickable {
                                                    image.showHistoryMenu(
                                                        msg.msgId,
                                                        listOf(
                                                            MENU_SAVE_FAV_EMOJI,
                                                            MENU_SHARE,
                                                            MENU_SAVE_PIC,
                                                        )
                                                    ) { item ->
                                                        when (item) {
                                                            MENU_SAVE_FAV_EMOJI -> saveFavEmoji(group.context, pic)
                                                            MENU_SHARE -> sharePic(group.context, pic)
                                                            MENU_SAVE_PIC -> savePic(group.context, pic)
                                                            else -> {}
                                                        }
                                                    }
                                                }
                                                val progress = add<ProgressBar>()
                                                progress.layoutParams = FrameLayout.LayoutParams(
                                                    28.dp,
                                                    28.dp,
                                                    Gravity.CENTER
                                                )
                                                image.loadPicElement(pic) { ok ->
                                                    progress.visibility = View.GONE
                                                    if (!ok) {
                                                        Utils.log("history image load failed md5=${pic.md5HexStr}")
                                                    }
                                                }
                                            }
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
