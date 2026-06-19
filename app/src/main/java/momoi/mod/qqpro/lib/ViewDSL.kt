package momoi.mod.qqpro.lib

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import momoi.mod.qqpro.findMethod
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

private val layoutParamsMethodCache = ConcurrentHashMap<Class<*>, Method>()
private val viewConstructorCache = ConcurrentHashMap<Class<*>, Constructor<*>>()

@PublishedApi
internal fun defaultLayoutParamsFor(group: ViewGroup): ViewGroup.LayoutParams {
    val method = layoutParamsMethodCache.getOrPut(group.javaClass) {
        group.javaClass.findMethod("generateDefaultLayoutParams").apply { isAccessible = true }
    }
    return method.invoke(group) as ViewGroup.LayoutParams
}

@PublishedApi
internal fun <T : View> instantiateView(clazz: Class<T>, context: Context): T {
    val ctor = viewConstructorCache.getOrPut(clazz) {
        clazz.getConstructor(Context::class.java)
    }
    @Suppress("UNCHECKED_CAST")
    return ctor.newInstance(context) as T
}

open class GroupScope(val group: ViewGroup) {
    inline fun <reified T : View> create(): T {
        return create<T>(
            group.context,
            defaultLayoutParamsFor(group)
        ).size(WRAP, WRAP)
    }
    inline fun <reified T : View> add(): T {
        val view = create<T>()
        group.addView(view)
        return view
    }

    fun add(view: View) {
        group.addView(view)
    }
}

inline fun <reified T : View> create(
    context: Context,
    params: ViewGroup.LayoutParams = ViewGroup.LayoutParams(
        WRAP_CONTENT,
        WRAP_CONTENT
    )
): T {
    val view = instantiateView(T::class.java, context)
    view.layoutParams = params
    return view
}

fun <T : ViewGroup> T.content(block: GroupScope.() -> Unit): T =
    apply { GroupScope(this).apply(block) }