package com.github.com.chenjia404.meshchat.feature.groups

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.R
import com.github.com.chenjia404.meshchat.domain.repository.GroupRepository
import com.github.com.chenjia404.meshchat.service.storage.ChatAttachmentUrlBuilder
import com.github.com.chenjia404.meshchat.service.storage.UriFileResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** meshchat-server `member_list_visibility` / `join_mode` 取值，与 API 一致 */
object SuperGroupServerSettings {
    const val MEMBER_LIST_VISIBLE = "visible"
    const val MEMBER_LIST_HIDDEN = "hidden"
    const val JOIN_INVITE_ONLY = "invite_only"
    const val JOIN_OPEN = "open"
}

data class SuperGroupIntroUiState(
    val groupId: String = "",
    val title: String = "",
    val about: String? = null,
    val avatarUrl: String? = null,
    val canInvite: Boolean = false,
    val canEditGroupInfo: Boolean = false,
    val editing: Boolean = false,
    val saving: Boolean = false,
    val draftTitle: String = "",
    val draftAbout: String = "",
    /** API 原始值，用于展示与编辑 */
    val memberListVisibility: String? = null,
    val joinMode: String? = null,
    val draftMemberListVisibility: String = SuperGroupServerSettings.MEMBER_LIST_VISIBLE,
    val draftJoinMode: String = SuperGroupServerSettings.JOIN_INVITE_ONLY,
    val pendingAvatarUri: Uri? = null,
    val loading: Boolean = true,
    val error: String? = null,
    /** 可显示「退出群聊」：加载成功为超级群，或加载失败但可能仍为本地超级群会话（如 502） */
    val canLeave: Boolean = false,
    val leaving: Boolean = false,
)

@HiltViewModel
class SuperGroupIntroViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val groupRepository: GroupRepository,
    private val attachmentUrlBuilder: ChatAttachmentUrlBuilder,
    private val uriFileResolver: UriFileResolver,
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _uiState = MutableStateFlow(SuperGroupIntroUiState(groupId = groupId))
    val uiState: StateFlow<SuperGroupIntroUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching {
                val snap = groupRepository.loadSuperGroupIntroSnapshot(groupId)
                if (snap == null) {
                    _uiState.update {
                        it.copy(loading = false, error = "not_super_group", canLeave = false)
                    }
                } else {
                    val vis = snap.memberListVisibility ?: SuperGroupServerSettings.MEMBER_LIST_VISIBLE
                    val jm = snap.joinMode ?: SuperGroupServerSettings.JOIN_INVITE_ONLY
                    _uiState.update {
                        it.copy(
                            loading = false,
                            title = snap.title,
                            about = snap.about,
                            avatarUrl = attachmentUrlBuilder.ipfsBlobAbsoluteUrl(snap.avatarCid),
                            memberListVisibility = snap.memberListVisibility,
                            joinMode = snap.joinMode,
                            draftMemberListVisibility = vis,
                            draftJoinMode = jm,
                            canInvite = snap.canInvite,
                            canEditGroupInfo = snap.canEditGroupInfo,
                            draftTitle = snap.title,
                            draftAbout = snap.about.orEmpty(),
                            pendingAvatarUri = null,
                            editing = false,
                            canLeave = true,
                        )
                    }
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(loading = false, error = e.message, canLeave = true)
                }
            }
        }
    }

    fun enterEdit() {
        _uiState.update { s ->
            val vis = s.memberListVisibility ?: SuperGroupServerSettings.MEMBER_LIST_VISIBLE
            val jm = s.joinMode ?: SuperGroupServerSettings.JOIN_INVITE_ONLY
            s.copy(
                editing = true,
                draftTitle = s.title,
                draftAbout = s.about.orEmpty(),
                draftMemberListVisibility = vis,
                draftJoinMode = jm,
                pendingAvatarUri = null,
            )
        }
    }

    fun cancelEdit() {
        _uiState.update { s ->
            val vis = s.memberListVisibility ?: SuperGroupServerSettings.MEMBER_LIST_VISIBLE
            val jm = s.joinMode ?: SuperGroupServerSettings.JOIN_INVITE_ONLY
            s.copy(
                editing = false,
                draftTitle = s.title,
                draftAbout = s.about.orEmpty(),
                draftMemberListVisibility = vis,
                draftJoinMode = jm,
                pendingAvatarUri = null,
            )
        }
    }

    fun updateDraftTitle(value: String) {
        _uiState.update { it.copy(draftTitle = value) }
    }

    fun updateDraftAbout(value: String) {
        _uiState.update { it.copy(draftAbout = value) }
    }

    fun setPendingAvatarUri(uri: Uri?) {
        _uiState.update { it.copy(pendingAvatarUri = uri) }
    }

    fun updateDraftMemberListVisibility(value: String) {
        _uiState.update { it.copy(draftMemberListVisibility = value) }
    }

    fun updateDraftJoinMode(value: String) {
        _uiState.update { it.copy(draftJoinMode = value) }
    }

    fun saveEdits(onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value
            if (s.draftTitle.isBlank()) {
                onDone(false, appContext.getString(R.string.super_group_intro_error_title_empty))
                return@launch
            }
            _uiState.update { it.copy(saving = true) }
            runCatching {
                var avatarCid: String? = null
                s.pendingAvatarUri?.let { uri ->
                    val file = uriFileResolver.copyToCache(uri)
                    avatarCid = groupRepository.uploadSuperGroupAvatarToIpfs(groupId, file)
                }
                groupRepository.updateSuperGroupInfo(
                    groupId = groupId,
                    title = s.draftTitle.trim(),
                    about = s.draftAbout.trim(),
                    avatarCid = avatarCid,
                    memberListVisibility = s.draftMemberListVisibility,
                    joinMode = s.draftJoinMode,
                )
            }.onSuccess {
                _uiState.update { it.copy(saving = false, editing = false, pendingAvatarUri = null) }
                load()
                onDone(true, null)
            }.onFailure { e ->
                _uiState.update { it.copy(saving = false) }
                onDone(false, e.message)
            }
        }
    }

    fun inviteMembers(peerIds: List<String>, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            runCatching {
                groupRepository.inviteSuperGroupMembersByPeerIds(groupId, peerIds)
            }.onSuccess {
                load()
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.message)
            }
        }
    }

    /** 退出超级群：远端尽力而为；本地始终删除会话（见 [GroupRepository.leave]）。 */
    fun leaveGroup(onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(leaving = true) }
            runCatching {
                groupRepository.leave(groupId, "")
            }.onSuccess {
                _uiState.update { it.copy(leaving = false) }
                onDone(true, null)
            }.onFailure { e ->
                _uiState.update { it.copy(leaving = false) }
                onDone(false, e.message)
            }
        }
    }
}
