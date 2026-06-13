package moye.wear.hook

import android.os.Bundle
import android.view.View
import java.lang.ref.WeakReference
import com.tencent.qqnt.watch.add.QQAddFriendFragment
import momoi.anno.mixin.Mixin
import momoi.mod.qqpro.util.Utils
const val EXTRA_SEARCH_PREFILL = "qqpro_search_prefill"

fun View.openAddSearch(code: String, type: Int = 1) {
    try {
        val nav = findNavControllerFromTree() ?: run {
            Utils.log("openAddSearch: NavController not found in view tree")
            return
        }
        val actionId = resources.getIdentifier(
            "select_fragment_to_add_friend", "id", context.packageName
        )
        if (actionId == 0) {
            Utils.log("openAddSearch: select_fragment_to_add_friend id not found")
            return
        }
        val args = Bundle().apply {
            putInt("type", type)
            putString(EXTRA_SEARCH_PREFILL, code)
        }
        val navigate = nav.javaClass.methods.firstOrNull { m ->
            val p = m.parameterTypes
            p.size == 3 && p[0] == Int::class.javaPrimitiveType && p[1] == Bundle::class.java
        } ?: run {
            Utils.log("openAddSearch: navigate(int,Bundle,..) not found on ${nav.javaClass.name}")
            return
        }
        navigate.invoke(nav, actionId, args, null)
    } catch (e: Exception) {
        Utils.log("openAddSearch error: ${e.message}")
    }
}

private fun View.findNavControllerFromTree(): Any? {
    val tagId = resources.getIdentifier("nav_controller_view_tag", "id", context.packageName)
    if (tagId == 0) return null
    var v: View? = this
    while (v != null) {
        when (val tag = v.getTag(tagId)) {
            is WeakReference<*> -> tag.get()?.let { return it }
            null -> {}
            else -> return tag
        }
        v = v.parent as? View
    }
    return null
}

@Mixin
class AddFriendPrefill : QQAddFriendFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val code = arguments?.getString(EXTRA_SEARCH_PREFILL)
        if (code.isNullOrEmpty()) return
        try {
            g = code
            e.text = code
        } catch (ex: Exception) {
            Utils.log("AddFriendPrefill error: ${ex.message}")
        }
    }
}
