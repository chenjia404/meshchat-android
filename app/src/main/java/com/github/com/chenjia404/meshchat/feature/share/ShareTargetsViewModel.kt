package com.github.com.chenjia404.meshchat.feature.share

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.com.chenjia404.meshchat.domain.repository.DirectChatRepository
import com.github.com.chenjia404.meshchat.domain.repository.GroupRepository
import com.github.com.chenjia404.meshchat.domain.repository.PublicChannelRepository
import com.github.com.chenjia404.meshchat.domain.usecase.ForwardDestination
import com.github.com.chenjia404.meshchat.feature.forward.ForwardTargetRowItem
import com.github.com.chenjia404.meshchat.feature.forward.toForwardDestination
import com.github.com.chenjia404.meshchat.service.storage.UriFileResolver
import com.github.com.chenjia404.meshchat.share.IncomingShareManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ShareTargetsViewModel @Inject constructor(
    private val incomingShareManager: IncomingShareManager,
    private val uriFileResolver: UriFileResolver,
    private val directChatRepository: DirectChatRepository,
    private val groupRepository: GroupRepository,
    private val publicChannelRepository: PublicChannelRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    fun sendToTargets(
        chosen: List<ForwardTargetRowItem>,
        onFinished: (ok: Boolean, err: String?) -> Unit,
    ) {
        val payload = incomingShareManager.pending.value
        if (payload == null || payload.uris.isEmpty()) {
            onFinished(false, null)
            return
        }
        val destinations = chosen.mapNotNull { it.toForwardDestination() }
        if (destinations.isEmpty()) {
            onFinished(false, null)
            return
        }
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    for (uri in payload.uris) {
                        val source = uriFileResolver.copyToCache(uri)
                        try {
                            for (dest in destinations) {
                                val copy = File(
                                    appContext.cacheDir,
                                    "share-${UUID.randomUUID()}-${source.name}",
                                )
                                source.copyTo(copy, overwrite = true)
                                try {
                                    when (dest) {
                                        is ForwardDestination.Direct ->
                                            directChatRepository.sendFile(dest.conversationId, copy)
                                        is ForwardDestination.Group ->
                                            groupRepository.sendFile(dest.groupId, copy)
                                        is ForwardDestination.PublicChannel ->
                                            publicChannelRepository.sendFile(dest.channelId, copy)
                                    }
                                } finally {
                                    copy.delete()
                                }
                            }
                        } finally {
                            source.delete()
                        }
                    }
                }
            }
            result.onSuccess { onFinished(true, null) }
                .onFailure { e -> onFinished(false, e.message) }
        }
    }
}
