package moye.wear.hook.privacy.bugly

import com.tencent.bugly.proguard.aj
import momoi.anno.mixin.StaticHook
import momoi.mod.qqpro.Settings

@StaticHook(aj::class)
fun q(): Boolean = if (Settings.enableAntiDetection.value) false else aj.q()

@StaticHook(aj::class)
fun r(): Boolean = if (Settings.enableAntiDetection.value) false else aj.r()

@StaticHook(aj::class)
fun s(): Boolean = if (Settings.enableAntiDetection.value) false else aj.s()
