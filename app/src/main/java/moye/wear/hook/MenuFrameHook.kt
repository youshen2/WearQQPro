package moye.wear.hook

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.tencent.watch.aio_impl.ui.frames.MenuFrame
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Settings
import java.util.WeakHashMap

@Mixin
class MenuFrameHook(p0: Function1<Int, Unit>, p1: Boolean) : MenuFrame(p0, p1) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AlbumMultiSelectInstaller.install(i)
    }
}

object AlbumMultiSelectInstaller {

    private const val ALBUM_LABEL = "相册"
    private val installed = WeakHashMap<RecyclerView, Boolean>()

    fun install(recyclerView: RecyclerView?) {
        recyclerView ?: return
        if (installed[recyclerView] == true) return
        recyclerView.addOnChildAttachStateChangeListener(object :
            RecyclerView.OnChildAttachStateChangeListener {
            override fun onChildViewAttachedToWindow(view: View) {
                bindAlbum(view)
            }

            override fun onChildViewDetachedFromWindow(view: View) = Unit
        })
        installed[recyclerView] = true
    }

    private fun bindAlbum(row: View) {
        if (!Settings.multiSelectAlbum.value) return
        val group = row as? ViewGroup ?: return
        val label = group.findTextView()?.text?.toString() ?: return
        if (label != ALBUM_LABEL) return
        val icon = group.findImageView() ?: return
        icon.setOnClickListener {
            val activity = icon.context.findFragmentActivity() ?: return@setOnClickListener
            AlbumPickerFragment.show(activity.supportFragmentManager)
        }
    }

    private fun ViewGroup.findTextView(): TextView? {
        for (i in 0 until childCount) {
            when (val child = getChildAt(i)) {
                is TextView -> return child
                is ViewGroup -> child.findTextView()?.let { return it }
            }
        }
        return null
    }

    private fun ViewGroup.findImageView(): ImageView? {
        for (i in 0 until childCount) {
            when (val child = getChildAt(i)) {
                is ImageView -> return child
                is ViewGroup -> child.findImageView()?.let { return it }
            }
        }
        return null
    }

    private fun Context.findFragmentActivity(): FragmentActivity? {
        var ctx: Context? = this
        while (ctx is ContextWrapper) {
            if (ctx is FragmentActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
