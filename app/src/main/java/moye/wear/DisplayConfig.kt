package moye.wear

object DisplayConfig {
    const val appName = "WearQQ Pro"
    const val versionName = "1.1"

    val selfFragmentText: String
        get() = buildString {
            appendLine("$appName - v$versionName")
            appendLine()
            appendLine("更新日志：")
            appendLine("修复从相册选图后返回时页面与状态不同步的问题（即选择图片后向右滑会直接退出会话页面的问题）")
            appendLine()
            appendLine("特别鸣谢：")
            appendLine("java30433")
            appendLine("|huanli233|")
            appendLine()
            appendLine("交流群：757440701")
            appendLine("2026/06/13")
        }

    val aboutDialogText: String
        get() = buildString {
            appendLine("WearQQ Pro")
            appendLine("1.1")
            appendLine()
            appendLine("由 爅峫 制作")
            appendLine("基于QQPro v1.5.1")
            appendLine()
            appendLine("禁止删除“爅峫”署名")
            appendLine("禁止用于任何形式的商业用途")
            appendLine()
            append("反馈请加交流群")
        }
}
