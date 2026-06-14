package moye.wear.hook.privacy

import android.content.Context
import com.tencent.beacon.core.info.BeaconPubParams
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.Settings

@Mixin
class AntiDetectionBeacon(context: Context) : BeaconPubParams(context) {
    override fun getIsRooted(): String {
        return if (Settings.enableAntiDetection.value) "0" else super.getIsRooted()
    }
}
