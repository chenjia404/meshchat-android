package com.github.com.chenjia404.meshchat.core.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

/**
 * 从 GitHub [releases/latest](https://docs.github.com/en/rest/releases/releases#get-the-latest-release) 拉取版本，
 * 与当前 [android.content.pm.PackageInfo.versionName] 比较；若有更新则返回信息供弹窗与打开下载页。
 *
 * 与 meshproxyandroid 中逻辑一致，仓库为 chenjia404/meshchat-android。
 */
data class AppUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val releaseUrl: String,
)

private const val HTTP_TIMEOUT_MS = 12_000

private const val GITHUB_LATEST_RELEASE_API =
    "https://api.github.com/repos/chenjia404/meshchat-android/releases/latest"

private const val GITHUB_RELEASES_PAGE =
    "https://github.com/chenjia404/meshchat-android/releases"

object GitHubAppUpdateChecker {

    suspend fun fetchLatestReleaseIfNewer(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val body = httpGetJson(GITHUB_LATEST_RELEASE_API) ?: return@runCatching null
            parseLatestReleaseInfo(body, context)
        }.getOrNull()
    }

    private fun httpGetJson(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = HTTP_TIMEOUT_MS
            readTimeout = HTTP_TIMEOUT_MS
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "meshchat-android")
        }
        return try {
            if (connection.responseCode !in 200..299) return null
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseLatestReleaseInfo(responseBody: String, context: Context): AppUpdateInfo? {
        val json = JSONObject(responseBody)
        val latestVersion = json.optStringValue("tag_name", "name") ?: return null
        val releaseUrl = json.optStringValue("html_url") ?: GITHUB_RELEASES_PAGE
        val currentVersion = currentAppVersion(context)
        if (!isRemoteVersionNewer(latestVersion, currentVersion)) {
            return null
        }
        return AppUpdateInfo(
            currentVersion = currentVersion,
            latestVersion = latestVersion,
            releaseUrl = releaseUrl,
        )
    }

    private fun JSONObject.optStringValue(vararg keys: String): String? {
        for (key in keys) {
            val value = optString(key, "").trim()
            if (value.isNotEmpty()) {
                return value
            }
        }
        return null
    }

    private fun currentAppVersion(context: Context): String =
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
        } catch (_: Exception) {
            "0"
        }

    private fun isRemoteVersionNewer(remoteVersion: String, localVersion: String): Boolean {
        val remoteParts = versionParts(remoteVersion)
        val localParts = versionParts(localVersion)
        val maxSize = max(remoteParts.size, localParts.size)
        for (index in 0 until maxSize) {
            val remote = remoteParts.getOrElse(index) { 0 }
            val local = localParts.getOrElse(index) { 0 }
            if (remote != local) {
                return remote > local
            }
        }
        return remoteVersion.trim() != localVersion.trim() &&
            remoteVersion.trim().removePrefix("v") != localVersion.trim().removePrefix("v")
    }

    private fun versionParts(version: String): List<Int> =
        version
            .trim()
            .removePrefix("v")
            .split('.', '-', '_')
            .mapNotNull { it.toIntOrNull() }
}
