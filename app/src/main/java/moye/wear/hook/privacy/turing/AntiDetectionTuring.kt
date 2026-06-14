package moye.wear.hook.privacy.turing

import android.content.Context
import com.tencent.turingfd.sdk.xq.Virgo
import momoi.anno.mixin.StaticHook
import momoi.mod.qqpro.Settings

@StaticHook(Virgo::class)
fun a(context: Context): Int = if (Settings.enableAntiDetection.value) 0 else Virgo.a(context)

@StaticHook(Virgo::class)
fun e(context: Context): Boolean = if (Settings.enableAntiDetection.value) false else Virgo.e(context)
