package moye.wear.hook.privacy.bugly

import android.content.Context
import com.tencent.bugly.proguard.au
import momoi.anno.mixin.StaticHook
import momoi.mod.qqpro.Settings

@StaticHook(au::class)
fun a(context: Context) {
    if (!Settings.enableAntiDetection.value) {
        au.a(context)
    }
}
