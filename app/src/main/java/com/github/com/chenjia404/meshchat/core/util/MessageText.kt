package com.github.com.chenjia404.meshchat.core.util

/**
 * 统一换行符，便于 Compose [androidx.compose.material3.Text] 按行展示多行内容。
 */
fun String.normalizeMessageNewlines(): String =
    replace("\r\n", "\n").replace('\r', '\n')
