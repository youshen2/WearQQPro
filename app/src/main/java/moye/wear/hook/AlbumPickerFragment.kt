package moye.wear.hook

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.LruCache
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import momoi.mod.qqpro.drawable.roundCornerDrawable
import momoi.mod.qqpro.hook.view.MyDialogFragment
import momoi.mod.qqpro.lib.dp
import momoi.mod.qqpro.lib.dpf
import momoi.mod.qqpro.util.Utils
import momoi.mod.qqpro.util.runOnUi
import java.util.concurrent.Executors

class AlbumPickerFragment : MyDialogFragment() {

    private val images = ArrayList<Uri>()
    private val selected = ArrayList<Uri>()
    private val executor = Executors.newFixedThreadPool(3)
    private val thumbCache = object : LruCache<Uri, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: Uri, value: Bitmap): Int = value.byteCount / 1024
    }

    private var doneLabel: TextView? = null
    private var grid: RecyclerView? = null
    private var emptyLabel: TextView? = null
    private var cellSize: Int = 0

    private val maxCount = 9

    private val permission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    // BODY_PLACEHOLDER

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = inflater.context
        cellSize = ctx.resources.displayMetrics.widthPixels / 2
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF_000000.toInt())
            layoutParams = ViewGroup.LayoutParams(FILL, FILL)
            grid = RecyclerView(ctx).apply {
                layoutManager = GridLayoutManager(ctx, 2)
                overScrollMode = View.OVER_SCROLL_NEVER
                adapter = GridAdapter()
                layoutParams = LinearLayout.LayoutParams(FILL, 0).apply { weight = 1f }
            }
            addView(grid)
            emptyLabel = TextView(ctx).apply {
                text = "没有可显示的图片"
                setTextColor(0xFF_FFFFFF.toInt())
                textSize = 13f
                gravity = Gravity.CENTER
                visibility = View.GONE
                layoutParams = LinearLayout.LayoutParams(FILL, 0).apply { weight = 1f }
            }
            addView(emptyLabel)
            addView(buildBottomBar(ctx))
        }
    }
    // STEP2

    @SuppressLint("SetTextI18n")
    private fun buildBottomBar(ctx: android.content.Context): View {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8.dp, 8.dp, 8.dp, 8.dp)
            layoutParams = LinearLayout.LayoutParams(FILL, WRAP)
            addView(TextView(ctx).apply {
                text = "取消"
                setTextColor(0xFF_FFFFFF.toInt())
                textSize = 13f
                gravity = Gravity.CENTER
                background = roundCornerDrawable(0xFF_2A2A2A.toInt(), 18.dpf)
                setPadding(0, 9.dp, 0, 9.dp)
                layoutParams = LinearLayout.LayoutParams(0, WRAP).apply {
                    weight = 1f
                    rightMargin = 4.dp
                }
                setOnClickListener { dismissAllowingStateLoss() }
            })
            doneLabel = TextView(ctx).apply {
                text = "完成"
                setTextColor(0xFF_FFFFFF.toInt())
                textSize = 13f
                gravity = Gravity.CENTER
                background = roundCornerDrawable(0xFF_1B9AF7.toInt(), 18.dpf)
                setPadding(0, 9.dp, 0, 9.dp)
                layoutParams = LinearLayout.LayoutParams(0, WRAP).apply {
                    weight = 1f
                    leftMargin = 4.dp
                }
                setOnClickListener { onDone() }
            }
            addView(doneLabel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (ContextCompat.checkSelfPermission(requireContext(), permission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            loadImages()
        } else {
            requestPermissions(arrayOf(permission), REQ_PERM)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != REQ_PERM) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            loadImages()
        } else {
            Utils.toast(requireContext(), "未授予读取相册权限")
            dismissAllowingStateLoss()
        }
    }
    // STEP3

    private fun loadImages() {
        executor.execute {
            val list = ArrayList<Uri>()
            runCatching {
                val projection = arrayOf(MediaStore.Images.Media._ID)
                val sort = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"
                requireContext().contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection, null, null, sort
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    while (cursor.moveToNext() && list.size < MAX_LOAD) {
                        val id = cursor.getLong(idCol)
                        list.add(
                            Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString()
                            )
                        )
                    }
                }
            }.onFailure {
                Utils.log("AlbumPicker loadImages failed: ${it.message}")
            }
            runOnUi {
                if (!isAdded) return@runOnUi
                images.clear()
                images.addAll(list)
                grid?.adapter?.notifyDataSetChanged()
                if (images.isEmpty()) {
                    grid?.visibility = View.GONE
                    emptyLabel?.visibility = View.VISIBLE
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateDoneLabel() {
        doneLabel?.text = if (selected.isEmpty()) "完成" else "完成(${selected.size})"
    }

    private fun onDone() {
        if (selected.isEmpty()) {
            Utils.toast(requireContext(), "请先选择图片")
            return
        }
        AlbumMultiSelect.deliver(requireContext().applicationContext, ArrayList(selected))
        dismissAllowingStateLoss()
    }

    private fun toggle(uri: Uri): Boolean {
        if (selected.contains(uri)) {
            selected.remove(uri)
        } else {
            if (selected.size >= maxCount) {
                Utils.toast(requireContext(), "最多选择 $maxCount 张")
                return false
            }
            selected.add(uri)
        }
        updateDoneLabel()
        return true
    }
    // STEP4

    private fun loadThumb(uri: Uri, target: ImageView) {
        target.tag = uri
        thumbCache.get(uri)?.let {
            target.setImageBitmap(it)
            return
        }
        target.setImageBitmap(null)
        executor.execute {
            val bitmap = decodeThumb(uri) ?: return@execute
            thumbCache.put(uri, bitmap)
            runOnUi {
                if (target.tag == uri) {
                    target.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun decodeThumb(uri: Uri): Bitmap? {
        return try {
            val resolver = requireContext().contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            val target = cellSize.coerceAtLeast(120)
            while (bounds.outWidth / sample > target * 2 || bounds.outHeight / sample > target * 2) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (e: Exception) {
            Utils.log("AlbumPicker decodeThumb failed: ${e.message}")
            null
        }
    }

    private inner class GridAdapter : RecyclerView.Adapter<GridHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridHolder {
            val ctx = parent.context
            val frame = FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(cellSize, cellSize)
            }
            val image = ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(0xFF_1A1A1A.toInt())
                layoutParams = FrameLayout.LayoutParams(FILL, FILL).apply { setMargins(2.dp, 2.dp, 2.dp, 2.dp) }
            }
            val badge = TextView(ctx).apply {
                gravity = Gravity.CENTER
                textSize = 11f
                setTextColor(0xFF_FFFFFF.toInt())
                layoutParams = FrameLayout.LayoutParams(20.dp, 20.dp).apply {
                    gravity = Gravity.TOP or Gravity.END
                    setMargins(0, 6.dp, 6.dp, 0)
                }
            }
            frame.addView(image)
            frame.addView(badge)
            return GridHolder(frame, image, badge)
        }

        override fun getItemCount(): Int = images.size

        override fun onBindViewHolder(holder: GridHolder, position: Int) {
            val uri = images[position]
            loadThumb(uri, holder.image)
            bindBadge(holder.badge, uri)
            holder.itemView.setOnClickListener {
                if (toggle(uri)) notifyDataSetChanged()
            }
        }
    }
    // STEP5

    @SuppressLint("SetTextI18n")
    private fun bindBadge(badge: TextView, uri: Uri) {
        val index = selected.indexOf(uri)
        if (index >= 0) {
            badge.text = (index + 1).toString()
            badge.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF_1B9AF7.toInt())
                setStroke(1.dp, Color.WHITE)
            }
        } else {
            badge.text = ""
            badge.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0x55_000000)
                setStroke(1.dp, 0xCC_FFFFFF.toInt())
            }
        }
    }

    private class GridHolder(
        itemView: View,
        val image: ImageView,
        val badge: TextView
    ) : RecyclerView.ViewHolder(itemView)

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(FILL, FILL)
            setDimAmount(0f)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        executor.shutdownNow()
        thumbCache.evictAll()
    }

    companion object {
        private const val TAG = "qqpro_album_picker"
        private const val REQ_PERM = 0x9B02
        private const val MAX_LOAD = 2000

        fun show(fm: androidx.fragment.app.FragmentManager) {
            if (fm.findFragmentByTag(TAG) != null) return
            AlbumPickerFragment().show(fm, TAG)
        }
    }
}

private const val FILL = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
