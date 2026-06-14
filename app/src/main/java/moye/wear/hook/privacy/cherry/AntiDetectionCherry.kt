package moye.wear.hook.privacy.cherry

import android.content.Context
import com.tencent.turingfd.sdk.xq.Cherry
import momoi.anno.mixin.StaticHook
import momoi.mod.qqpro.Settings

@StaticHook(Cherry::class)
fun a(): Boolean = if (Settings.enableAntiDetection.value) false else Cherry.a()

@StaticHook(Cherry::class)
fun a(context: Context): Boolean = if (Settings.enableAntiDetection.value) false else Cherry.a(context)
