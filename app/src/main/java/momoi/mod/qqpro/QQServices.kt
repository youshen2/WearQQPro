package momoi.mod.qqpro

import com.tencent.qqnt.kernel.nativeinterface.IKernelGroupService
import com.tencent.qqnt.msg.KernelServiceUtil

object QQServices {
    val group: IKernelGroupService
        get() = KernelServiceUtil.f()!!.wrapperSession!!.groupService
}
