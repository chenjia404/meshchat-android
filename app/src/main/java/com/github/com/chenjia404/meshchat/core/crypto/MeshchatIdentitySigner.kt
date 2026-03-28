package com.github.com.chenjia404.meshchat.core.crypto

import android.util.Base64
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * 与 QuarkPayAndroid [com.quarkpay.wallet.utils.MeshchatIdentitySigner] 行为一致：
 * 仅接受 PEM 段内 Base64 解码为 32 或 64 字节原始 Ed25519 材料；签名与 libp2p protobuf 公钥编码。
 */
object MeshchatIdentitySigner {

    private val PEM_BODY = Regex(
        "-----BEGIN[^\\n]+-----\\s*([\\s\\S]*?)-----END[^\\n]+-----",
        RegexOption.IGNORE_CASE,
    )

    @Throws(IOException::class)
    fun loadEd25519Seed32(pemFile: File): ByteArray {
        if (!pemFile.isFile) {
            throw IOException("identity key missing")
        }
        val text = pemFile.readText(StandardCharsets.UTF_8)
        val m = PEM_BODY.find(text) ?: throw IOException("invalid PEM")
        val b64 = m.groupValues[1].replace(Regex("\\s+"), "")
        val raw = Base64.decode(b64, Base64.DEFAULT)
        if (raw.size == 64) {
            return raw.copyOfRange(0, 32)
        }
        if (raw.size == 32) {
            return raw
        }
        throw IOException("unexpected ed25519 key length: ${raw.size}")
    }

    /** libp2p protobuf PublicKey：Type=Ed25519(1), Data=32 字节公钥。 */
    fun marshalLibp2pEd25519PublicKeyProtobuf(pub32: ByteArray): ByteArray {
        require(pub32.size == 32) { "ed25519 public key must be 32 bytes" }
        val out = ByteArray(4 + 32)
        out[0] = 0x08
        out[1] = 0x01
        out[2] = 0x12
        out[3] = 0x20
        System.arraycopy(pub32, 0, out, 4, 32)
        return out
    }

    @Throws(IOException::class)
    fun signChallengeBase64(seed32: ByteArray, challengeUtf8: String): String {
        val sk = Ed25519PrivateKeyParameters(seed32, 0)
        val signer = Ed25519Signer()
        signer.init(true, sk)
        val msg = challengeUtf8.toByteArray(StandardCharsets.UTF_8)
        signer.update(msg, 0, msg.size)
        val sig = signer.generateSignature()
        return Base64.encodeToString(sig, Base64.NO_WRAP)
    }

    fun publicKey32FromSeed(seed32: ByteArray): ByteArray {
        val sk = Ed25519PrivateKeyParameters(seed32, 0)
        val pk = sk.generatePublicKey() as Ed25519PublicKeyParameters
        return pk.encoded
    }
}
