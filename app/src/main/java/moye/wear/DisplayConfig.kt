package moye.wear

object DisplayConfig {
    const val appName = "WearQQ Pro"
    const val versionName = "2.1"

    val selfFragmentText: String
        get() = buildString {
            appendLine("$appName - v$versionName")
            appendLine()
            appendLine("由 爅峫 制作")
            appendLine("基于QQPro v1.5.1")
            appendLine("参考自QQ Max")
            appendLine()
            appendLine("特别鸣谢：")
            appendLine("AILife")
            appendLine("java30433")
            appendLine("|huanli233|")
            appendLine()
            appendLine("交流群：757440701")
            appendLine("2026/06/14")
            appendLine()
            appendLine("仅供学习参考使用，请于24小时内删除")
            appendLine()
        }

    val updateLogText: String
        get() = buildString {
            appendLine("V2.1：")
            appendLine("支持了更多消息类型的分享")
            appendLine("修复了无法回复消息的问题")
            appendLine("修复了长按菜单排序调整对话框没有全屏的问题")
            appendLine("修复了长按菜单中的UI样式不统一的问题")
            appendLine("回退了QQProV1.5.1中导致异常的头衔获取方式改动")
            appendLine("---")
            appendLine("V2.0：")
            appendLine("统一了设置页面的UI样式")
            appendLine("增加了反ROOT等检测功能")
            appendLine("增加了查找聊天记录")
            appendLine("增加了回复消息定位失败时的Toast提示")
            appendLine("增加了聊天记录中消息和查看大图的长按菜单")
            appendLine("增加了聊天记录中图片的加载提示")
            appendLine("增加了“启用附加菜单”设置项，支持聊天页面使用加号按钮代替左滑第二屏")
            appendLine("增加了图片最大高度设置项")
            appendLine("增加了隐藏语音键功能")
            appendLine("增加了链接预览卡片功能")
            appendLine("增加了链接的无前缀识别功能")
            appendLine("增加了私聊会话详情页的跳转QQ空间选项")
            appendLine("增加了显示消息头像功能")
            appendLine("增加了查看群头像大图功能")
            appendLine("增加了合并连续消息头功能")
            appendLine("增加了消息长按菜单排序功能")
            appendLine("增加了热门GIF搜索功能")
            appendLine("增加了消息长按自由复制功能")
            appendLine("增加了昵称称号双行开关")
            appendLine("支持了好友推荐卡片的展示")
            appendLine("支持了缩放倍率实时生效")
            appendLine("修复了从相册选图后返回时页面与状态不同步的问题（即选择图片后向右滑会直接退出会话页面的问题）")
            appendLine("修复了未滚动聊天记录时无法回到未读的问题")
            appendLine("修复了聊天记录中的GIF无法正常播放的问题")
            appendLine("修复了聊天记录页面无法右滑退出的问题")
            appendLine("修复了会话列表中表情被显示为乱码的问题")
            appendLine("修复了滚动过程中图片大小乱跳的问题")
            appendLine("修复了图片文字组合消息无法双击回复的问题")
            appendLine("修复了小程序卡片的显示问题")
            appendLine("优化了消息转发功能")
            appendLine("去除了QQPro的检查更新逻辑")
        }

    val aboutDialogText: String
        get() = buildString {
            appendLine("WearQQ Pro")
            appendLine("2.0")
            appendLine()
            appendLine("基于QQPro v1.5.1")
            appendLine("参考自QQ Max")
            appendLine()
            appendLine("禁止删除“爅峫”署名")
            appendLine("禁止用于任何形式的商业用途")
            appendLine()
            appendLine("仅供学习参考使用，请于24小时内删除")
            append("")
        }
}
