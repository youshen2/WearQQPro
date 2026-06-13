package momoi.mod.qqpro.util

import android.annotation.SuppressLint
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.tencent.mobileqq.utils.TimeFormatterUtils
import com.tencent.mobileqq.widget.QQToast
import androidx.core.net.toUri

object Utils {
    @SuppressLint("PrivateApi")
    val application = Class.forName("android.app.ActivityThread").getMethod("currentApplication")
        .invoke(null) as Application
    val isDebug =
        try {
            val info = application.applicationInfo
            (info.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }

    fun formatTime(timestamp: Long): CharSequence =
        TimeFormatterUtils.a(application, 3, timestamp, true, true)!!

    private var debugWatcher: Any? = null
    fun debugger(catch: Any?) {
        debugWatcher = catch
        Log.e("QQQQQQQQQQ", "debugger!")
    }

    fun log(msg: String) {
        Log.e("WearQQ", msg)
    }

    val heightPixels = Resources.getSystem().displayMetrics.heightPixels
    val isRoundScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Resources.getSystem().configuration.isScreenRound
    } else {
        isDebug
    }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(application.packageManager) != null) {
            application.startActivity(intent)
        }
    }

    fun toast(context: Context, text: CharSequence, longDuration: Boolean = false) {
        val duration = if (longDuration) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        QQToast.i(context, text, duration).l()
    }

    fun copyToClipboard(context: Context, text: CharSequence, toastText: CharSequence = "已复制") {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("label", text))
        toast(context, toastText)
    }
}