package com.github.com.chenjia404.meshchat.core.util

import android.content.res.Resources
import com.github.com.chenjia404.meshchat.R

/** 气泡内附件副标题（图片/视频/语音/文件） */
fun attachmentSubtitle(resources: Resources, renderType: AttachmentRenderType, mimeType: String?, msgType: String): String {
    return when (renderType) {
        AttachmentRenderType.IMAGE -> resources.getString(R.string.attachment_type_image)
        AttachmentRenderType.VIDEO -> resources.getString(R.string.attachment_type_video)
        AttachmentRenderType.AUDIO -> resources.getString(R.string.attachment_type_audio)
        AttachmentRenderType.FILE -> mimeType ?: resources.getString(R.string.attachment_type_file)
        AttachmentRenderType.SYSTEM -> msgType
        AttachmentRenderType.TEXT -> ""
    }
}
