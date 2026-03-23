package com.github.com.chenjia404.meshchat.domain.usecase

import android.content.Context
import com.github.com.chenjia404.meshchat.core.util.AttachmentRenderType
import com.github.com.chenjia404.meshchat.domain.repository.DirectChatRepository
import com.github.com.chenjia404.meshchat.domain.repository.GroupRepository
import com.github.com.chenjia404.meshchat.service.download.FileDownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

sealed class ForwardDestination {
    data class Direct(val conversationId: String) : ForwardDestination()
    data class Group(val groupId: String) : ForwardDestination()
}

class ForwardMessageUseCase @Inject constructor(
    private val directChatRepository: DirectChatRepository,
    private val groupRepository: GroupRepository,
    private val fileDownloadService: FileDownloadService,
    @ApplicationContext private val context: Context,
) {
    /**
     * @param downloadUrl 附件类消息用于下载的完整 URL；文本消息可为 null
     * @param plainText 文本/系统消息的正文
     */
    suspend fun forward(
        renderType: AttachmentRenderType,
        plainText: String,
        subtitle: String,
        fileName: String?,
        downloadUrl: String?,
        destinations: List<ForwardDestination>,
    ) {
        if (destinations.isEmpty()) return
        when (renderType) {
            AttachmentRenderType.TEXT,
            AttachmentRenderType.SYSTEM,
            -> {
                val text = when (renderType) {
                    AttachmentRenderType.TEXT -> plainText
                    AttachmentRenderType.SYSTEM -> plainText.ifBlank { subtitle }
                    else -> ""
                }
                require(text.isNotBlank()) { "无可转发的文本" }
                destinations.forEach { dest ->
                    when (dest) {
                        is ForwardDestination.Direct ->
                            directChatRepository.sendText(dest.conversationId, text)
                        is ForwardDestination.Group ->
                            groupRepository.sendText(dest.groupId, text)
                    }
                }
            }
            AttachmentRenderType.IMAGE,
            AttachmentRenderType.VIDEO,
            AttachmentRenderType.AUDIO,
            AttachmentRenderType.FILE,
            -> {
                val url = downloadUrl ?: error("附件缺少下载地址")
                val baseName = fileName?.takeIf { it.isNotBlank() } ?: "meshchat-attachment"
                val safeName = baseName.replace("/", "_")
                val downloaded = fileDownloadService.downloadToTempForForward(url, safeName)
                try {
                    destinations.forEach { dest ->
                        val copy = File(
                            context.cacheDir,
                            "forward-${UUID.randomUUID()}-$safeName",
                        )
                        downloaded.copyTo(copy, overwrite = true)
                        try {
                            when (dest) {
                                is ForwardDestination.Direct ->
                                    directChatRepository.sendFile(dest.conversationId, copy)
                                is ForwardDestination.Group ->
                                    groupRepository.sendFile(dest.groupId, copy)
                            }
                        } finally {
                            copy.delete()
                        }
                    }
                } finally {
                    downloaded.delete()
                }
            }
        }
    }
}
