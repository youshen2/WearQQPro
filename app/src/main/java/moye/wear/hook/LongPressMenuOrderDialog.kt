package moye.wear.hook

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tencent.qqlive.module.videoreport.inject.dialog.ReportDialog
import momoi.mod.qqpro.Settings
import momoi.mod.qqpro.lib.FILL
import momoi.mod.qqpro.lib.clickable
import momoi.mod.qqpro.lib.content
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.gravity
import momoi.mod.qqpro.lib.margin
import momoi.mod.qqpro.lib.padding
import momoi.mod.qqpro.lib.text
import momoi.mod.qqpro.lib.textColor
import momoi.mod.qqpro.lib.textSize
import momoi.mod.qqpro.lib.vertical
import momoi.mod.qqpro.lib.width
import moye.wear.lib.SwipeBackLayout
import java.util.Collections

object LongPressMenuOrderManager {
    private const val PREF_KEY = "longPressMenuOrder"

    val defaultOrder = listOf(
        "回复",
        "转发",
        "@Ta",
        "自由复制",
        "复制文本",
        "复读文本",
        "去聊天",
        "加好友",
        "撤回",
        "删除",
    )

    fun getOrder(): MutableList<String> {
        val saved = Settings.sp.getString(PREF_KEY, null)
            ?.split('\n')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
        val merged = LinkedHashSet<String>()
        saved.forEach { merged.add(it) }
        defaultOrder.forEach { merged.add(it) }
        return merged.toMutableList()
    }

    fun saveOrder(order: List<String>) {
        val merged = LinkedHashSet<String>()
        order.forEach {
            val label = it.trim()
            if (label.isNotEmpty()) {
                merged.add(label)
            }
        }
        defaultOrder.forEach { merged.add(it) }
        Settings.sp.edit().putString(PREF_KEY, merged.joinToString("\n")).apply()
    }

    fun reset() {
        saveOrder(defaultOrder)
    }

    fun rememberLabels(labels: Collection<String>) {
        val normalized = labels.map { it.trim() }.filter { it.isNotEmpty() }
        if (normalized.isEmpty()) {
            return
        }
        val order = getOrder()
        var changed = false
        normalized.forEach { label ->
            if (label !in order) {
                order.add(label)
                changed = true
            }
        }
        if (changed) {
            saveOrder(order)
        }
    }

    fun sortLabels(labels: Collection<String>): List<String> {
        val observed = labels.map { it.trim() }.filter { it.isNotEmpty() }
        rememberLabels(observed)
        val remaining = observed.toMutableList()
        val sorted = mutableListOf<String>()
        getOrder().forEach { label ->
            if (remaining.remove(label)) {
                sorted.add(label)
            }
        }
        sorted += remaining
        return sorted
    }
}

class LongPressMenuOrderDialog(context: Context) : ReportDialog(context) {
    private val items = LongPressMenuOrderManager.getOrder()
    private lateinit var adapter: OrderAdapter
    private lateinit var touchHelper: ItemTouchHelper
    private var orderChanged = false

    companion object {
        fun show(context: Context) {
            LongPressMenuOrderDialog(context).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(createContentView())
        applyFullscreenWindow()
    }

    override fun onStart() {
        super.onStart()
        applyFullscreenWindow()
    }

    private fun applyFullscreenWindow() {
        window?.apply {
            setBackgroundDrawable(ColorDrawable(0xF0_121212.toInt()))
            decorView.setPadding(0, 0, 0, 0)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            attributes = attributes.apply {
                width = WindowManager.LayoutParams.MATCH_PARENT
                height = WindowManager.LayoutParams.MATCH_PARENT
                gravity = Gravity.CENTER
                horizontalMargin = 0f
                verticalMargin = 0f
            }
            addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    private fun createContentView(): View {
        val edgePadding = if (momoi.mod.qqpro.util.Utils.isRoundScreen) 14.dp else 8.dp
        val compactGap = if (momoi.mod.qqpro.util.Utils.isRoundScreen) 6.dp else 4.dp
        val root = LinearLayout(context).vertical().apply {
            layoutParams = ViewGroup.LayoutParams(FILL, FILL)
            setBackgroundColor(0xF0_121212.toInt())
            setPadding(edgePadding, 0, edgePadding, compactGap)
        }
        root.content {
            add<TextView>()
                .text("长按菜单排序")
                .textSize(14f)
                .textColor(0xFF_FFFFFF.toInt())
                .gravity(Gravity.CENTER)
                .width(FILL)
                .padding(left = 8.dp, top = 2.dp, right = 8.dp, bottom = 2.dp)
            add<TextView>()
                .text("按住右侧手柄上下拖动，松手后自动保存。")
                .textSize(10f)
                .textColor(0xCC_FFFFFF.toInt())
                .gravity(Gravity.CENTER)
                .width(FILL)
                .padding(left = 6.dp, top = 0, right = 6.dp, bottom = 6.dp)
            val list = add<RecyclerView>().apply {
                layoutManager = LinearLayoutManager(context)
                layoutParams = LinearLayout.LayoutParams(FILL, 0, 1f)
                overScrollMode = View.OVER_SCROLL_NEVER
                clipToPadding = false
                itemAnimator = null
                setPadding(0, 0, 0, compactGap)
            }
            adapter = OrderAdapter(items)
            list.adapter = adapter
            touchHelper = ItemTouchHelper(OrderTouchCallback(adapter))
            touchHelper.attachToRecyclerView(list)
            add(
                ActionBarView(context).bind(
                    onReset = {
                        items.clear()
                        items.addAll(LongPressMenuOrderManager.defaultOrder)
                        persist()
                        adapter.notifyDataSetChanged()
                    },
                    onClose = { this@LongPressMenuOrderDialog.dismiss() }
                )
            )
        }
        return SwipeBackLayout(context).apply {
            addView(root, FILL, FILL)
            onSwipeBack = { this@LongPressMenuOrderDialog.dismiss() }
        }
    }

    private fun persist() {
        LongPressMenuOrderManager.saveOrder(items)
    }

    private inner class OrderTouchCallback(
        private val adapter: OrderAdapter
    ) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
                return false
            }
            if (from < to) {
                for (index in from until to) {
                    Collections.swap(items, index, index + 1)
                }
            } else {
                for (index in from downTo to + 1) {
                    Collections.swap(items, index, index - 1)
                }
            }
            orderChanged = true
            adapter.notifyItemMoved(from, to)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun isLongPressDragEnabled(): Boolean = false

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView
            itemView.alpha = if (isCurrentlyActive) 0.88f else 1f
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            viewHolder.itemView.alpha = 1f
            super.clearView(recyclerView, viewHolder)
            if (orderChanged) {
                orderChanged = false
                persist()
                adapter.notifyDataSetChanged()
            }
        }
    }

    private inner class OrderAdapter(
        private val data: MutableList<String>
    ) : RecyclerView.Adapter<OrderHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderHolder {
            return OrderHolder(createRow(parent))
        }

        override fun getItemCount(): Int = data.size

        override fun onBindViewHolder(holder: OrderHolder, position: Int) {
            holder.bind(position, data[position])
        }
    }

    private inner class OrderHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val row = view as LinearLayout
        private val indexView = row.getChildAt(0) as TextView
        private val titleView = row.getChildAt(1) as TextView
        private val handleView = row.getChildAt(2) as TextView

        @SuppressLint("SetTextI18n")
        fun bind(position: Int, label: String) {
            indexView.text = "${position + 1}."
            titleView.text = label
            handleView.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(this)
                    false
                } else {
                    false
                }
            }
        }
    }

    private fun createRow(parent: ViewGroup): View {
        val ctx = parent.context
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = RecyclerView.LayoutParams(FILL, ViewGroup.LayoutParams.WRAP_CONTENT)
            background = GradientDrawable().apply {
                setColor(0x99_303030.toInt())
                cornerRadius = 12.dp.toFloat()
            }
            setPadding(12.dp, 9.dp, 12.dp, 9.dp)
            addView(TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setTextColor(0xFF_9E9E9E.toInt())
                textSize = 10f
            })
            addView(TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 8.dp
                }
                setTextColor(0xFF_FFFFFF.toInt())
                textSize = 13f
                maxLines = 1
            })
            addView(TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setTextColor(0xFF_7FC8FF.toInt())
                text = "≡"
                textSize = 15f
                setPadding(6.dp, 2.dp, 6.dp, 2.dp)
            })
            margin(left = 0, top = 0, right = 0, bottom = 8.dp)
        }
    }

    private class ActionBarView(context: android.content.Context) : LinearLayout(context) {
        init {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(FILL, ViewGroup.LayoutParams.WRAP_CONTENT)
            setPadding(0, 4.dp, 0, 8.dp)
        }

        fun bind(onReset: () -> Unit, onClose: () -> Unit): ActionBarView = apply {
            removeAllViews()
            addView(actionButton("恢复默认", 0xFF_2A2A2A.toInt(), onReset).apply {
                layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = 6.dp
                }
            })
            addView(actionButton("完成", 0xFF_4FC3F7.toInt(), onClose).apply {
                layoutParams = LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 6.dp
                }
            })
        }

        private fun actionButton(label: String, color: Int, onClick: () -> Unit): TextView {
            return TextView(context).apply {
                gravity = Gravity.CENTER
                setTextColor(0xFF_FFFFFF.toInt())
                text = label
                textSize = 12f
                background = GradientDrawable().apply {
                    setColor(color)
                    cornerRadius = 16.dp.toFloat()
                }
                setPadding(0, 8.dp, 0, 8.dp)
                clickable(onClick)
            }
        }
    }
}
