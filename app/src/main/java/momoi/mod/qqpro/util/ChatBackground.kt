package momoi.mod.qqpro.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.widget.ImageView
import momoi.mod.qqpro.Settings
import java.io.File

object ChatBackground {
    private const val FILE_NAME = "chat_bg.jpg"
    private const val MAX_SIZE = 1080

    private fun file(context: Context): File =
        File(context.filesDir, FILE_NAME)

    fun isSet(): Boolean =
        file(Utils.application).exists()

    fun save(context: Context, uri: Uri): Boolean {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return false
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, opts)
            input.close()

            val scale = maxOf(1, maxOf(opts.outWidth, opts.outHeight) / MAX_SIZE)
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = scale }
            val input2 = context.contentResolver.openInputStream(uri) ?: return false
            val bitmap = BitmapFactory.decodeStream(input2, null, decodeOpts) ?: return false
            input2.close()

            val out = file(context)
            out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            bitmap.recycle()
            true
        } catch (e: Exception) {
            Utils.log("ChatBackground.save failed: ${e.message}")
            false
        }
    }

    fun clear() {
        file(Utils.application).delete()
    }

    fun applyTo(imageView: ImageView?) {
        val iv = imageView ?: return
        val ctx = iv.context ?: return
        val f = file(ctx)
        if (!f.exists()) return
        try {
            val bitmap = BitmapFactory.decodeFile(f.absolutePath) ?: return
            val darken = Settings.chatBgDarken.value
            if (darken > 0.01f) {
                val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
                val canvas = Canvas(result)
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                val overlay = Paint().apply {
                    color = Color.BLACK
                    alpha = (darken * 255).toInt().coerceIn(0, 255)
                }
                canvas.drawRect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat(), overlay)
                iv.setImageBitmap(result)
                if (result != bitmap) bitmap.recycle()
            } else {
                iv.setImageBitmap(bitmap)
            }
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
        } catch (e: Exception) {
            Utils.log("ChatBackground.applyTo failed: ${e.message}")
        }
    }
}
