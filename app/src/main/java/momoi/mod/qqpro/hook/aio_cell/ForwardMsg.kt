package momoi.mod.qqpro.hook.aio_cell

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.provider.MediaStore
import androidx.recyclerview.widget.RecyclerView
import com.tencent.qqnt.kernel.api.impl.MsgService
import com.tencent.qqnt.kernel.nativeinterface.Contact
import com.tencent.qqnt.kernel.nativeinterface.MsgElement
import com.tencent.qqnt.kernel.nativeinterface.MsgRecord
import com.tencent.qqnt.kernel.nativeinterface.PicElement
import com.tencent.qqnt.msg.KernelServiceUtil
import com.tencent.richframework.widget.matrix.RFWMatrixImageView
import loadPicElement
import momoi.mod.qqpro.child
import momoi.mod.qqpro.MsgUtil
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.drawable.roundCornerDrawable
import momoi.mod.qqpro.hook.style.MyImageView
import momoi.mod.qqpro.hook.view.MyDialogFragment
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.background
import momoi.mod.qqpro.lib.clickable
import momoi.mod.qqpro.lib.content
import momoi.mod.qqpro.lib.create
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.dpf
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
import momoi.mod.qqpro.msg.getImageUrl
import momoi.mod.qqpro.removeAfter
import momoi.mod.qqpro.showDialog
import momoi.mod.qqpro.util.linkify
import momoi.mod.qqpro.util.Utils
import momoi.mod.qqpro.util.runOnUi
import download

class BigImageFragment(private val pic: PicElement) : MyDialogFragment() {
    private lateinit var mOverlayHost: FrameLayout

    private fun hideImageLongPressMenu() {
        if (!::mOverlayHost.isInitialized) {
            return
        }
        mOverlayHost.removeAllViews()
        mOverlayHost.visibility = View.GONE
    }

    private fun showImageLongPressMenu(anchor: View) {
        val actions = listOf(
            PreviewMenuAction("保存") {
                anchor.context.savePicToAlbum(pic, "Saved")
            }
        )
        mOverlayHost.removeAllViews()
        mOverlayHost.visibility = View.VISIBLE
        mOverlayHost.addView(
            createMenuOverlay(mOverlayHost.context, actions) {
                hideImageLongPressMenu()
            }
        )
        mOverlayHost.bringToFront()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return create<FrameLayout>(inflater.context)
            .size(FILL, FILL)
            .content {
                add(
                    RFWMatrixImageView(inflater.context, null)
                        .layoutParams(ViewGroup.LayoutParams(FILL, FILL))
                        .apply {
                            installPreviewLongClick {
                                showImageLongPressMenu(this)
                            }
                        }
                        .loadPicElement(pic)
                )
                add<View>()
                    .size(FILL, 12.dp)
                    .clickable {
                        this@BigImageFragment.dismiss()
                    }
                mOverlayHost = add<FrameLayout>()
                    .size(FILL, FILL)
                    .apply {
                        visibility = View.GONE
                    }
            }
    }
}

private data class PreviewMenuAction(
    val title: String,
    val onClick: () -> Unit
)

private fun Context.copyTextToClipboard(text: CharSequence) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("chat_preview", text))
}

private fun Context.shareText(text: CharSequence) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text.toString())
    }
    val chooser = Intent.createChooser(intent, "分享").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (chooser.resolveActivity(packageManager) != null) {
        startActivity(chooser)
    }
}

private fun createMenuOverlay(
    context: Context,
    actions: List<PreviewMenuAction>,
    onDismiss: () -> Unit
): FrameLayout {
    return create<FrameLayout>(context)
        .size(FILL, FILL)
        .background(0x77_000000)
        .clickable { onDismiss() }
        .content {
            add<LinearLayout>()
                .vertical()
                .layoutParams(
                    FrameLayout.LayoutParams(
                        FILL,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER
                        leftMargin = 18.dp
                        rightMargin = 18.dp
                    }
                )
                .content {
                    actions.forEach { action ->
                        add<TextView>()
                            .width(FILL)
                            .padding(8.dp)
                            .margin(bottom = 6.dp)
                            .gravity(Gravity.CENTER)
                            .text(action.title)
                            .textSize(16f)
                            .textColor(0xFF_FFFFFF.toInt())
                            .background(
                                roundCornerDrawable(
                                    color = 0xFF_515151.toInt(),
                                    radius = 16.dpf
                                )
                            )
                            .clickable {
                                onDismiss()
                                action.onClick()
                            }
                    }
                }
        }
}

private fun Context.savePicToAlbum(pic: PicElement, folder: String) {
    val cacheRoot = externalCacheDir ?: return
    val baseName = pic.md5HexStr?.takeIf { it.isNotEmpty() } ?: System.currentTimeMillis().toString()
    val fileName = "$baseName.jpg"
    val tempFile = cacheRoot.child("preview_export/$fileName")
    tempFile.parentFile?.mkdirs()
    val imageUrl = pic.getImageUrl()
    if (imageUrl.isEmpty()) {
        return
    }
    download(imageUrl, tempFile) { succeed ->
        if (!succeed || !tempFile.exists()) {
            Utils.log("save preview image failed: $fileName")
            return@download
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/WearQQ Pro/$folder"
                    )
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        tempFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                }
            } else {
                val targetDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    .child("WearQQ Pro/$folder")
                targetDir.mkdirs()
                val targetFile = targetDir.child(fileName)
                tempFile.inputStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        } catch (e: Exception) {
            Utils.log("save preview image error: ${e.message}")
        }
    }
}

private fun View.collectBubbleText(): String {
    return when (this) {
        is TextView -> text?.toString().orEmpty()
        is ViewGroup -> buildString {
            for (index in 0 until childCount) {
                val content = getChildAt(index).collectBubbleText().trim()
                if (content.isNotEmpty()) {
                    if (isNotEmpty()) {
                        append('\n')
                    }
                    append(content)
                }
            }
        }

        else -> ""
    }
}

private fun View.installPreviewLongClick(onLongClick: () -> Unit) {
    setOnLongClickListener {
        it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongClick()
        true
    }
    if (this is ViewGroup) {
        for (index in 0 until childCount) {
            getChildAt(index).installPreviewLongClick(onLongClick)
        }
    }
}

class DetailFragment(private val contact: Contact, private val data: ForwardMsgData) :
    MyDialogFragment() {
    private val mMsgList = mutableListOf<MsgRecord>()
    private lateinit var mRv: RecyclerView
    private lateinit var mOverlayHost: FrameLayout

    private fun hidePreviewLongPressMenu() {
        if (!::mOverlayHost.isInitialized) {
            return
        }
        mOverlayHost.removeAllViews()
        mOverlayHost.visibility = View.GONE
    }

    private fun showPreviewLongPressMenu(anchor: View, msg: MsgRecord) {
        val text = anchor.collectBubbleText().trim()
        val pic = msg.elements?.mapNotNull { it.picElement }?.firstOrNull()
        val hasForward = msg.elements?.any { it.multiForwardMsgElement != null } == true
        val actions = mutableListOf<PreviewMenuAction>().apply {
            if (text.isNotEmpty()) {
                add(
                    PreviewMenuAction("复制文本") {
                        anchor.context.copyTextToClipboard(text)
                    }
                )
            }
            if (pic != null) {
                add(
                    PreviewMenuAction("查看图片") {
                        anchor.post {
                            anchor.showDialog(BigImageFragment(pic))
                        }
                    }
                )
            }
            if (hasForward) {
                add(
                    PreviewMenuAction("打开聊天记录") {
                        anchor.post {
                            anchor.showDialog(
                                DetailFragment(contact, ForwardMsgData(contact, data.rootMsg, msg))
                            )
                        }
                    }
                )
            }
        }
        if (actions.isEmpty()) {
            return
        }
        mOverlayHost.removeAllViews()
        mOverlayHost.visibility = View.VISIBLE
        mOverlayHost.addView(
            createMenuOverlay(mOverlayHost.context, actions) {
                hidePreviewLongPressMenu()
            }
        )
        mOverlayHost.bringToFront()
    }

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
        return create<FrameLayout>(inflater.context)
            .size(FILL, FILL)
            .background(0x77_000000)
            .content {
                add<LinearLayout>()
                    .vertical()
                    .size(FILL, FILL)
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
                                    val bubble = find<LinearLayout>(1)
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
                                    bubble.installPreviewLongClick {
                                        showPreviewLongPressMenu(bubble, msg)
                                    }
                                })
                    }
                mOverlayHost = add<FrameLayout>()
                    .size(FILL, FILL)
                    .apply {
                        visibility = View.GONE
                    }
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
