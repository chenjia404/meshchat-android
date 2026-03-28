package com.github.com.chenjia404.meshchat.core.crypto

import android.content.Context
import java.io.File

object MeshProxyIdentityPaths {
    /** 与 mesh-proxy / Quark 一致：`filesDir/data/identity.key` */
    fun identityKeyFile(context: Context): File = File(context.filesDir, "data/identity.key")
}
