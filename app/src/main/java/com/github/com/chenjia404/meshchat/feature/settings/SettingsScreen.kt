package com.github.com.chenjia404.meshchat.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.core.ui.AvatarImage
import com.github.com.chenjia404.meshchat.domain.repository.ProfileRepository
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import com.github.com.chenjia404.meshchat.service.storage.UriFileResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val nickname: String = "",
    val bio: String = "",
    val peerId: String = "",
    val avatarUrl: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
    private val uriFileResolver: UriFileResolver,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = profileRepository.myProfile
        .map { profile ->
            SettingsUiState(
                nickname = profile?.nickname.orEmpty(),
                bio = profile?.bio.orEmpty(),
                peerId = profile?.peerId.orEmpty(),
                avatarUrl = attachmentUrlBuilder.avatarUrl(profile?.avatar),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun updateProfile(nickname: String, bio: String) {
        viewModelScope.launch { profileRepository.updateProfile(nickname, bio) }
    }

    fun refreshMyProfile() {
        viewModelScope.launch { profileRepository.refreshMyProfile() }
    }

    fun uploadAvatar(uri: Uri) {
        viewModelScope.launch {
            val file = uriFileResolver.copyToCache(uri)
            profileRepository.uploadAvatar(file)
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var nickname by remember(uiState.nickname) { mutableStateOf(uiState.nickname) }
    var bio by remember(uiState.bio) { mutableStateOf(uiState.bio) }
    val clipboardManager = LocalClipboardManager.current
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::uploadAvatar)
    }

    LaunchedEffect(Unit) {
        if (uiState.peerId.isBlank()) {
            viewModel.refreshMyProfile()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("个人资料", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))
        AvatarImage(
            title = uiState.nickname.ifBlank { uiState.peerId.ifBlank { "我" } },
            avatarUrl = uiState.avatarUrl,
            modifier = Modifier.size(88.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = uiState.peerId,
            onValueChange = {},
            label = { Text("peer_id") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (uiState.peerId.isNotBlank()) {
                    clipboardManager.setText(AnnotatedString(uiState.peerId))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = uiState.peerId.isNotBlank(),
        ) {
            Text("复制 peer_id")
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = nickname,
            onValueChange = { nickname = it },
            label = { Text("昵称") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("简介") },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { avatarPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
            Text("更新头像")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.updateProfile(nickname, bio) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("保存资料")
        }
    }
}
