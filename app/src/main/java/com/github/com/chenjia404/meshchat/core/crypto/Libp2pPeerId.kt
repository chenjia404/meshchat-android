package com.github.com.chenjia404.meshchat.core.crypto

import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters

/**
 * 与 go-libp2p [github.com/libp2p/go-libp2p/core/peer.IDFromPublicKey] 一致：
 * protobuf [PublicKey] → multihash（短密钥 identity，否则 sha2-256）→ Base58。
 */
internal object Libp2pPeerId {
    private const val MAX_INLINE_KEY_LENGTH = 42

    fun fromEd25519PublicRaw(pub32: ByteArray): String {
        require(pub32.size == 32) { "ed25519_pub_len" }
        val marshalled = marshalledPublicKeyProtobuf(pub32)
        val mh = multihashFromMarshalledPub(marshalled)
        return Libp2pBase58.encode(mh)
    }

    /** 与 go [crypto.MarshalPublicKey] 一致，用于 meshchat-server `POST /auth/login` 的 `public_key`。 */
    internal fun marshalledPublicKeyProtobuf(ed25519Pub32: ByteArray): ByteArray = marshalLibp2pPublicKey(ed25519Pub32)

    fun fromPublicKeyParameters(pub: Ed25519PublicKeyParameters): String =
        fromEd25519PublicRaw(pub.encoded)

    /** libp2p protobuf PublicKey { Type=Ed25519, Data=32 bytes } */
    private fun marshalLibp2pPublicKey(ed25519Pub32: ByteArray): ByteArray {
        val out = ByteArray(2 + 2 + ed25519Pub32.size)
        var i = 0
        out[i++] = 0x08
        out[i++] = 0x01 // KeyType_Ed25519
        out[i++] = 0x12
        out[i++] = ed25519Pub32.size.toByte()
        System.arraycopy(ed25519Pub32, 0, out, i, ed25519Pub32.size)
        return out
    }

    private fun multihashFromMarshalledPub(marshalledPub: ByteArray): ByteArray {
        return if (marshalledPub.size <= MAX_INLINE_KEY_LENGTH) {
            identityMultihash(marshalledPub)
        } else {
            sha256Multihash(marshalledPub)
        }
    }

    /** multihash identity：code=0 + digest */
    private fun identityMultihash(digest: ByteArray): ByteArray {
        val code = encodeVarint(0L)
        val len = encodeVarint(digest.size.toLong())
        return code + len + digest
    }

    /** multihash sha2-256：code=0x12 + digest */
    private fun sha256Multihash(data: ByteArray): ByteArray {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        val code = encodeVarint(0x12L)
        val len = encodeVarint(digest.size.toLong())
        return code + len + digest
    }

    private fun encodeVarint(v: Long): ByteArray {
        var x = v
        val out = ArrayList<Byte>(10)
        while (true) {
            val b = (x and 0x7f).toInt()
            x = x ushr 7
            if (x == 0L) {
                out.add(b.toByte())
                break
            }
            out.add((b or 0x80).toByte())
        }
        return out.toByteArray()
    }

    private operator fun ByteArray.plus(other: ByteArray): ByteArray {
        val r = ByteArray(this.size + other.size)
        System.arraycopy(this, 0, r, 0, this.size)
        System.arraycopy(other, 0, r, this.size, other.size)
        return r
    }
}
