package momoi.mod.qqpro.hook

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.tencent.qqnt.watch.selftab.ui.SelfFragment
import momoi.anno.mixin.Mixin
import moye.wear.DisplayConfig
import moye.wear.hook.SelfUpdateLogEntry

@Mixin
class 版权信息 : SelfFragment() {
    @SuppressLint("ResourceType", "SetTextI18n")
    override fun Y(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val result = super.Y(inflater, container, savedInstanceState)
        val tv = result.findViewById<TextView>(2114521808)
        tv.text = DisplayConfig.selfFragmentText
        SelfUpdateLogEntry.install(this, result)
        return result
    }
}
