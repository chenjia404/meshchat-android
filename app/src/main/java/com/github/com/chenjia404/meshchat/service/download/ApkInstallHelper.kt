package com.github.com.chenjia404.meshchat.service.download

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

/**
 * 使用已下载 APK 的 [Uri]（content:// 或 FileProvider content://）调起系统安装界面。
 */
object ApkInstallHelper {
    fun tryStartInstall(context: Context, apkUri: Uri) {
        val app = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!app.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(app, "请在设置中允许「安装未知应用」后再试", Toast.LENGTH_LONG).show()
                runCatching {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${app.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    app.startActivity(intent)
                }
                return
            }
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            app.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(app, "无法打开安装程序", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(app, e.message ?: "无法安装", Toast.LENGTH_SHORT).show()
        }
    }
}
