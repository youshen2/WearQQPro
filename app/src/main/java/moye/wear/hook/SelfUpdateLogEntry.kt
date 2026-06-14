package moye.wear.hook

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.tencent.qqnt.watch.selftab.impl.databinding.ItemSelfOperationBinding
import momoi.mod.qqpro.drawable.roundCornerDrawable
import momoi.mod.qqpro.hook.view.MyDialogFragment
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.dpf
import moye.wear.DisplayConfig
import moye.wear.lib.SwipeBackLayout

object SelfUpdateLogEntry {
    private const val ENTRY_TAG = "wearqq_update_log_entry"
    private const val PAGE_TAG = "wearqq_update_log_page"
    private const val OPERATION_CONTAINER_ID = 2114520542
    private const val UPDATE_ICON_RES_ID = 2114454995

    fun install(
        fragment: Fragment,
        root: View
    ) {
        addOperationEntry(fragment, root)
    }

    private fun addOperationEntry(
        fragment: Fragment,
        root: View
    ) {
        val container = root.findViewById<LinearLayout>(OPERATION_CONTAINER_ID) ?: return
        if (container.findViewWithTag<View>(ENTRY_TAG) != null) {
            return
        }
        val binding = ItemSelfOperationBinding.a(
            LayoutInflater.from(container.context),
            container,
            false
        )
        binding.a.tag = ENTRY_TAG
        binding.c.setImageResource(UPDATE_ICON_RES_ID)
        binding.b.text = "更新日志"
        bindOpenAction(fragment, binding.a)
        val aboutIndex = findAboutIndex(container)
        if (aboutIndex >= 0 && aboutIndex < container.childCount) {
            container.addView(binding.a, aboutIndex + 1)
        } else {
            container.addView(binding.a)
        }
    }

    private fun findAboutIndex(container: LinearLayout): Int {
        for (index in 0 until container.childCount) {
            val label = findLabelView(container.getChildAt(index))?.text?.toString()?.trim()
            if (label == "关于") {
                return index
            }
        }
        return -1
    }

    private fun findLabelView(view: View): TextView? {
        if (view is TextView) {
            return view
        }
        val group = view as? ViewGroup ?: return null
        for (index in 0 until group.childCount) {
            findLabelView(group.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun bindOpenAction(
        fragment: Fragment,
        view: View?
    ) {
        view?.setOnClickListener {
            openPage(fragment)
        }
    }

    private fun openPage(fragment: Fragment) {
        val existing = fragment.childFragmentManager.findFragmentByTag(PAGE_TAG)
        if (existing != null) {
            return
        }
        UpdateLogFragment().show(fragment.childFragmentManager, PAGE_TAG)
    }
}

class UpdateLogFragment : MyDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = inflater.context
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF_121212.toInt())
            setPadding(16.dp, 14.dp, 16.dp, 18.dp)
            addView(
                TextView(context).apply {
                    text = "更新日志"
                    textSize = 17f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(0xFF_FFFFFF.toInt())
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
            addView(
                TextView(context).apply {
                    text = "右滑返回"
                    textSize = 11f
                    setTextColor(0x99_FFFFFF.toInt())
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4.dp
                    bottomMargin = 14.dp
                }
            )
            addView(
                TextView(context).apply {
                    text = DisplayConfig.updateLogText
                    textSize = 11f
                    setLineSpacing(6.dp.toFloat(), 1f)
                    setTextColor(0xFF_F2F2F2.toInt())
                    setPadding(7.dp, 6.dp, 7.dp, 6.dp)
                },
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        val scrollView = ScrollView(context).apply {
            isFillViewport = true
            addView(
                content,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        return SwipeBackLayout(context).apply {
            addView(
                scrollView,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            onSwipeBack = { dismiss() }
        }
    }
}
