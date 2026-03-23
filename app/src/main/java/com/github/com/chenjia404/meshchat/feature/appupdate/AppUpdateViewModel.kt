package com.github.com.chenjia404.meshchat.feature.appupdate

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.core.update.AppUpdateInfo
import com.github.com.chenjia404.meshchat.core.update.GitHubAppUpdateChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AppUpdateViewModel @Inject constructor() : ViewModel() {

    private val _appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val appUpdateInfo: StateFlow<AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    private var hasCheckedThisSession = false

    /** 每个进程生命周期内只自动检查一次，避免反复弹窗 */
    fun checkForAppUpdate(context: Context) {
        if (hasCheckedThisSession) return
        hasCheckedThisSession = true
        val appContext = context.applicationContext
        viewModelScope.launch {
            _appUpdateInfo.value = GitHubAppUpdateChecker.fetchLatestReleaseIfNewer(appContext)
        }
    }

    fun dismissAppUpdatePrompt() {
        _appUpdateInfo.value = null
    }
}
