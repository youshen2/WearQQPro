package moye.wear.hook

import android.os.Bundle
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.tencent.qqnt.watch.ui.componet.tablayout.CircleIndicator
import com.tencent.watch.aio_impl.ui.WatchAIOFragment
import com.tencent.watch.aio_impl.ui.frames.FrameAdapter
import com.tencent.watch.aio_impl.ui.frames.MenuFrame
import com.tencent.watch.aio_impl.ui.frames.MenuItem
import momoi.mod.qqpro.drawable.roundCornerDrawable
import momoi.mod.qqpro.hook.view.CallConfirmFragment
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.dpf
import momoi.mod.qqpro.Settings
import java.lang.ref.WeakReference
import java.util.WeakHashMap

object ExtraMenuOverlay {
    private const val HOST_TAG = "wearqq_extra_menu_overlay_host"
    private const val PANEL_TAG = "wearqq_extra_menu_overlay_panel"
    private const val MENU_TAG = "wearqq_extra_menu_overlay_fragment"
    private val hostId = View.generateViewId()
    private val panelId = View.generateViewId()
    private val autoHideInstalled = WeakHashMap<RecyclerView, Boolean>()
    private val styleInstalled = WeakHashMap<RecyclerView, Boolean>()
    private var currentFragmentRef: WeakReference<WatchAIOFragment>? = null

    fun attach(fragment: WatchAIOFragment, root: ViewGroup) {
        currentFragmentRef = WeakReference(fragment)
        ensureHost(fragment, root)
        syncPagerUi(root)
    }

    fun detach(fragment: WatchAIOFragment) {
        hideHost(fragment)
        removeMenu(fragment)
        val current = currentFragmentRef?.get()
        if (current === fragment) {
            currentFragmentRef = null
        }
    }

    fun toggleFromCurrent(): Boolean {
        val fragment = currentFragmentRef?.get() ?: return false
        return toggle(fragment)
    }

    private fun toggle(fragment: WatchAIOFragment): Boolean {
        val host = fragment.view?.findViewWithTag<FrameLayout>(HOST_TAG) ?: return false
        return if (host.visibility == View.VISIBLE) {
            hideHost(fragment)
            true
        } else {
            show(fragment, host)
            true
        }
    }

    private fun ensureHost(fragment: WatchAIOFragment, root: ViewGroup) {
        if (root.findViewWithTag<View>(HOST_TAG) != null) {
            return
        }
        val host = FrameLayout(root.context).apply {
            id = hostId
            tag = HOST_TAG
            visibility = View.GONE
            setBackgroundColor(0x66_000000)
            isClickable = true
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setOnClickListener {
                hideHost(fragment)
            }
        }
        val panel = FrameLayout(root.context).apply {
            id = panelId
            tag = PANEL_TAG
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        host.addView(panel)
        root.addView(host)
    }

    private fun show(fragment: WatchAIOFragment, host: FrameLayout) {
        val adapter = fragment.f.adapter as? FrameAdapter ?: return
        val args = fragment.arguments ?: return
        val existing = fragment.childFragmentManager.findFragmentByTag(MENU_TAG) as? MenuFrame
        val menu = existing ?: MenuFrame(
            {
                hideHost(fragment)
            },
            adapter.l
        ).apply {
            arguments = Bundle(args)
        }
        if (existing == null) {
            fragment.childFragmentManager.beginTransaction()
                .replace(panelId, menu, MENU_TAG)
                .commitAllowingStateLoss()
            fragment.childFragmentManager.executePendingTransactions()
        }
        styleMenuList(menu)
        installAutoHide(menu, fragment)
        host.visibility = View.VISIBLE
        host.bringToFront()
    }

    private fun hideHost(fragment: WatchAIOFragment) {
        val host = fragment.view?.findViewWithTag<FrameLayout>(HOST_TAG)
        host?.visibility = View.GONE
    }

    private fun removeMenu(fragment: WatchAIOFragment) {
        val menu = fragment.childFragmentManager.findFragmentByTag(MENU_TAG) ?: return
        fragment.childFragmentManager.beginTransaction()
            .remove(menu)
            .commitAllowingStateLoss()
        fragment.childFragmentManager.executePendingTransactions()
    }

    private fun syncPagerUi(root: ViewGroup) {
        if (!Settings.enableExtraMenu.value) {
            return
        }
        val indicator = root.findCircleIndicator() ?: return
        val pager = root.findViewPager() ?: return
        val count = pager.adapter?.itemCount ?: return
        indicator.e = when (count) {
            1 -> hashMapOf(0 to 2114453684)
            2 -> hashMapOf(
                0 to 2114453684,
                1 to 2114453687
            )
            else -> indicator.e
        }
        indicator.c(count, pager.currentItem)
    }

    private fun styleMenuList(menu: MenuFrame) {
        val recyclerView = menu.i ?: return
        recyclerView.layoutManager = LinearLayoutManager(recyclerView.context)
        recyclerView.setBackgroundColor(0)
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        recyclerView.alpha = 0f
        recyclerView.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
            leftMargin = 18.dp
            rightMargin = 18.dp
        }
        recyclerView.setPadding(0, 12.dp, 0, 12.dp)
        recyclerView.clipToPadding = false
        if (styleInstalled[recyclerView] != true) {
            recyclerView.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(view: View) {
                    restyleMenuItem(view, recyclerView)
                }

                override fun onChildViewDetachedFromWindow(view: View) = Unit
            })
            styleInstalled[recyclerView] = true
        }
        recyclerView.post {
            restyleVisibleMenuItems(recyclerView)
            recyclerView.alpha = 1f
        }
    }

    private fun installAutoHide(menu: MenuFrame, fragment: WatchAIOFragment) {
        val recyclerView = menu.i ?: return
        if (autoHideInstalled[recyclerView] == true) {
            return
        }
        val gestureDetector = GestureDetector(
            recyclerView.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onSingleTapUp(e: MotionEvent): Boolean = true
            }
        )
        recyclerView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                if (
                    e.actionMasked == MotionEvent.ACTION_UP &&
                    gestureDetector.onTouchEvent(e) &&
                    rv.findChildViewUnder(e.x, e.y) != null
                ) {
                    rv.post {
                        hideHost(fragment)
                    }
                }
                return false
            }
        })
        autoHideInstalled[recyclerView] = true
    }

    private fun restyleVisibleMenuItems(recyclerView: RecyclerView) {
        for (index in 0 until recyclerView.childCount) {
            restyleMenuItem(recyclerView.getChildAt(index), recyclerView)
        }
    }

    private fun restyleMenuItem(view: View, recyclerView: RecyclerView) {
        val row = view as? LinearLayout ?: return
        val icon = row.getChildAt(0) as? ImageView
        val text = row.getChildAt(1) as? TextView ?: return
        row.orientation = LinearLayout.VERTICAL
        row.gravity = Gravity.CENTER
        row.layoutParams = (row.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            bottomMargin = 8.dp
        }
        row.background = roundCornerDrawable(0x44_000000, 16.dpf)
        row.setPadding(0, 6.dp, 0, 6.dp)
        icon?.visibility = View.GONE
        text.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        text.gravity = Gravity.CENTER
        text.textSize = 16f
        text.setTextColor(0xFF_FFFFFF.toInt())
        text.background = null
        text.setPadding(0, 0, 0, 0)
        val pos = recyclerView.getChildAdapterPosition(row)
        val menuItem = recyclerView.adapter?.let { adapter ->
            val field = adapter.javaClass.getDeclaredField("a")
            field.isAccessible = true
            (field.get(adapter) as? ArrayList<*>)?.getOrNull(pos) as? MenuItem
        }
        val isCall = menuItem?.d() == 3 || menuItem?.d() == 4
        if (isCall) {
            val confirmClick = View.OnClickListener {
                val fm = currentFragmentRef?.get()?.childFragmentManager ?: return@OnClickListener
                val target = icon ?: return@OnClickListener
                val label = text.text?.toString().orEmpty()
                runCatching {
                    CallConfirmFragment("确定要发起$label 吗？", target)
                        .show(fm, "qqpro_call_confirm")
                }
            }
            row.setOnClickListener(confirmClick)
            text.setOnClickListener(confirmClick)
        } else {
            row.setOnClickListener {
                icon?.performClick()
            }
            text.setOnClickListener {
                icon?.performClick()
            }
        }
    }

    private fun ViewGroup.findCircleIndicator(): CircleIndicator? {
        for (index in 0 until childCount) {
            when (val child = getChildAt(index)) {
                is CircleIndicator -> return child
                is ViewGroup -> child.findCircleIndicator()?.let { return it }
            }
        }
        return null
    }

    private fun ViewGroup.findViewPager(): ViewPager2? {
        for (index in 0 until childCount) {
            when (val child = getChildAt(index)) {
                is ViewPager2 -> return child
                is ViewGroup -> child.findViewPager()?.let { return it }
            }
        }
        return null
    }
}
