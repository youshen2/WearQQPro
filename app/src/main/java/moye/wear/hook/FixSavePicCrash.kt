package moye.wear.hook

import android.content.Context
import androidx.core.util.Consumer
import com.tencent.biz.richframework.util.RFWSaveUtil
import com.tencent.biz.richframework.util.bean.RFWSaveMediaResultBean
import momoi.anno.mixin.StaticHook

@StaticHook(RFWSaveUtil::class)
fun a(context: Context, mediaPath: String, consumer: Consumer<RFWSaveMediaResultBean>?) {
    val albumName = runCatching {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(context.packageName, 0)
        pm.getApplicationLabel(appInfo).toString().takeUnless { it.isEmpty() }
    }.getOrNull() ?: "QQ"

    RFWSaveUtil.b(context, mediaPath, albumName, consumer)
}
