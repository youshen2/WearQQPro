package momoi.mod.qqpro.lib

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import momoi.mod.qqpro.util.Utils
import java.io.File
import java.io.InputStream

fun <T : ImageView> T.imageResource(resId: Int) = apply {
    setImageResource(resId)
}
fun <T : ImageView> T.scaleType(scaleType: ImageView.ScaleType) = apply {
    setScaleType(scaleType)
}
fun <T : ImageView> T.adjustViewBounds(adjust: Boolean = true) = apply {
    adjustViewBounds = adjust
}

private fun File.isGif(): Boolean = try {
    inputStream().use { s ->
        val head = ByteArray(4)
        s.read(head) == 4 && head[0] == 'G'.code.toByte() && head[1] == 'I'.code.toByte() &&
            head[2] == 'F'.code.toByte() && head[3] == '8'.code.toByte()
    }
} catch (e: Exception) {
    false
}

fun ImageView.bitmapDecodeFile(file: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && file.isGif()) {
        try {
            val limit = maxHeight
            val src = ImageDecoder.createSource(file)
            val drawable = ImageDecoder.decodeDrawable(src) { decoder, info, _ ->
                val h = info.size.height
                val w = info.size.width
                if (h > 300 && h > limit && limit > 0) {
                    decoder.setTargetSize((w.toFloat() / h * limit).toInt(), limit)
                }
            }
            post {
                setImageDrawable(drawable)
                if (drawable is AnimatedImageDrawable) drawable.start()
            }
            return
        } catch (e: Exception) {
            Utils.log("GIF decode failed, falling back to bitmap: ${e.message}")
        }
    }
    var s: InputStream? = null
    bitmapDecodeStream {
        s?.close()
        s = file.inputStream()
        s!!
    }
    s?.close()
}

fun ImageView.bitmapDecodeAssets(path: String) =
    Utils.application.assets.open(path).use {
        bitmapDecodeStream { reread ->
            if (reread) {
                it.reset()
                it
            } else {
                it.mark(0)
                it
            }
        }
        this
    }

inline fun ImageView.bitmapDecodeStream(streamProvider: (reread: Boolean)->InputStream): Bitmap? {
    val rect = Rect()
    BitmapFactory.decodeStream(streamProvider(false), rect, BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    })
    val bitmap = BitmapFactory.decodeStream(streamProvider(true), null, BitmapFactory.Options().apply {
        if (rect.height() > 300 && rect.height() > maxHeight) {
            outHeight = maxHeight
            outWidth = (rect.width().toFloat() / rect.height().toFloat() * maxHeight.toFloat()).toInt()
            inSampleSize = rect.height() / maxHeight
        }
    })
    post {
        setImageBitmap(bitmap)
    }
    return bitmap
}