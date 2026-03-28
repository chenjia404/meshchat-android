package com.github.com.chenjia404.meshchat.core.crypto

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 从应用私有目录 [data/identity.key]（与 mesh-proxy 共用）加载 libp2p Ed25519 私钥（多格式见 [Libp2pPrivateKeyParser]）。
 * meshchat-server 登录与签名已改为 [MeshchatIdentitySigner]（与 Quark 一致）；本类保留作备用。
 */
@Singleton
class Libp2pIdentity @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val lock = Any()

    @Volatile
    private var cached: Ed25519PrivateKeyParameters? = null

    fun privateKeyOrThrow(): Ed25519PrivateKeyParameters {
        cached?.let { return it }
        synchronized(lock) {
            cached?.let { return it }
            val f = File(context.filesDir, "data/identity.key")
            if (!f.isFile) {
                error("missing_identity_key:${f.absolutePath}")
            }
            val params = Libp2pPrivateKeyParser.parse(f.readBytes())
            cached = params
            return params
        }
    }

    fun peerIdString(): String {
        val pub = privateKeyOrThrow().generatePublicKey() as Ed25519PublicKeyParameters
        return Libp2pPeerId.fromPublicKeyParameters(pub)
    }

    fun signChallengeUtf8(challenge: String): ByteArray {
        val msg = challenge.toByteArray(Charsets.UTF_8)
        val priv = privateKeyOrThrow()
        val signer = Ed25519Signer()
        signer.init(true, priv)
        signer.update(msg, 0, msg.size)
        return signer.generateSignature()
    }

    /** go [crypto.MarshalPublicKey] 的 Base64，用于 `POST /auth/login` 的 `public_key`。 */
    fun marshalledPublicKeyBase64(): String {
        val pub = privateKeyOrThrow().generatePublicKey() as Ed25519PublicKeyParameters
        val pb = Libp2pPeerId.marshalledPublicKeyProtobuf(pub.encoded)
        return Base64.encodeToString(pb, Base64.NO_WRAP)
    }
}
