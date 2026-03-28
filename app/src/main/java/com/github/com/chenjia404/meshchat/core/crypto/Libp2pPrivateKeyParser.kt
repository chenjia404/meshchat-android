package com.github.com.chenjia404.meshchat.core.crypto

import android.util.Base64
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.util.PrivateKeyFactory
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader

/**
 * 解析与 mesh-proxy 共用的身份私钥（`filesDir/data/identity.key`）。
 *
 * 与 QuarkPayAndroid `MeshchatIdentitySigner.loadEd25519Seed32` 行为对齐：**优先**将 PEM 段内 Base64 解码为
 * **原始 Ed25519 种子**（32 字节，或 Go 风格 64 字节私钥取前 32 字节），适用于
 * `-----BEGIN … ED25519 … PRIVATE KEY-----` 等 mesh-proxy 常见 PEM。
 *
 * 若上述不匹配，再尝试：PKCS#8 DER（`0x30`）、libp2p protobuf `PrivateKey`、单行 Base64、裸 32/64 字节。
 */
internal object Libp2pPrivateKeyParser {

    /** 与 QuarkPay `MeshchatIdentitySigner` 中 PEM 正文正则等价，用于提取 PEM 段内 Base64 */
    private val PEM_BODY_REGEX = Regex(
        "-----BEGIN[^\\n]+-----\\s*([\\s\\S]*?)-----END[^\\n]+-----",
        RegexOption.IGNORE_CASE,
    )

    fun parse(fileBytes: ByteArray): Ed25519PrivateKeyParameters {
        val withoutBom = stripUtf8Bom(fileBytes)
        val text = runCatching { withoutBom.toString(Charsets.UTF_8) }.getOrNull()
        if (text != null && text.contains("-----BEGIN")) {
            tryParseMeshProxyPemRawEd25519Seed(text)?.let { return it }
            return parsePemBody(text)
        }
        if (text != null) {
            val oneLine = text.trim().replace("\n", "").replace("\r", "")
            if (oneLine.length >= 44 && isLikelyBase64KeyLine(oneLine)) {
                runCatching {
                    val decoded = Base64.decode(oneLine, Base64.DEFAULT)
                    if (decoded.isNotEmpty()) return parseBinary(decoded)
                }
            }
        }
        return parseBinary(withoutBom)
    }

    private fun stripUtf8Bom(bytes: ByteArray): ByteArray {
        if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        ) {
            return bytes.copyOfRange(3, bytes.size)
        }
        return bytes
    }

    /** 单行 Base64（无 PEM 头）常见于导出私钥 */
    private fun isLikelyBase64KeyLine(s: String): Boolean {
        if (s.length % 4 != 0) return false
        return s.all { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=" }
    }

    /**
     * mesh-proxy / QuarkPay：PEM 段内为 **原始 Ed25519 密钥字节** 的 Base64（解码后 32 或 64 字节），
     * 非 PKCS#8 ASN.1、非 libp2p protobuf。
     */
    private fun tryParseMeshProxyPemRawEd25519Seed(text: String): Ed25519PrivateKeyParameters? {
        val match = PEM_BODY_REGEX.find(text) ?: return null
        val b64 = match.groupValues[1].replace(Regex("\\s+"), "")
        val raw = runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull() ?: return null
        return when (raw.size) {
            64 -> Ed25519PrivateKeyParameters(raw.copyOfRange(0, 32), 0)
            32 -> Ed25519PrivateKeyParameters(raw, 0)
            else -> null
        }
    }

    private fun parsePemBody(text: String): Ed25519PrivateKeyParameters {
        PemReader(StringReader(text.trim())).use { reader ->
            val obj = reader.readPemObject() ?: error("empty_pem")
            return parseBinary(obj.content)
        }
    }

    private fun parseBinary(content: ByteArray): Ed25519PrivateKeyParameters {
        if (content.isEmpty()) error("empty_key_bytes")
        when (content.size) {
            32 -> return Ed25519PrivateKeyParameters(content, 0)
            64 -> return Ed25519PrivateKeyParameters(content.copyOfRange(0, 32), 0)
        }
        // PKCS#8 / X.509 私钥 DER 以 ASN.1 SEQUENCE 开头 0x30
        if (content[0] == 0x30.toByte()) {
            return parsePkcs8(content)
        }
        // libp2p protobuf PrivateKey 通常以字段 tag 开头（如 0x08），绝不能当 PKCS#8 解析
        return parseLibp2pProtobufPrivateKey(content)
    }

    private fun parseLibp2pProtobufPrivateKey(content: ByteArray): Ed25519PrivateKeyParameters {
        val parsed = parseProtobufPrivateKey(content)
        require(parsed.type == 1) { "non_ed25519_key" }
        return dataToEd25519Params(parsed.data)
    }

    private data class ProtoPrivateKey(val type: Int, val data: ByteArray)

    /**
     * 解析 libp2p protobuf [PrivateKey]；未知 wire 类型会按 protobuf 规则跳过，避免死循环或误读。
     */
    private fun parseProtobufPrivateKey(bytes: ByteArray): ProtoPrivateKey {
        var i = 0
        var keyType = -1
        var data: ByteArray? = null
        while (i < bytes.size) {
            val tag = bytes[i++].toInt() and 0xff
            val field = tag shr 3
            val wire = tag and 0x7
            when (wire) {
                0 -> {
                    var v = 0L
                    var shift = 0
                    while (i < bytes.size) {
                        val b = bytes[i++].toInt() and 0xff
                        v = v or ((b and 0x7f).toLong() shl shift)
                        if ((b and 0x80) == 0) break
                        shift += 7
                    }
                    if (field == 1) keyType = v.toInt()
                }
                1 -> {
                    require(i + 8 <= bytes.size) { "truncated_proto_fixed64" }
                    i += 8
                }
                2 -> {
                    var len = 0
                    var shift = 0
                    while (i < bytes.size) {
                        val b = bytes[i++].toInt() and 0xff
                        len = len or ((b and 0x7f) shl shift)
                        if ((b and 0x80) == 0) break
                        shift += 7
                    }
                    require(i + len <= bytes.size) { "truncated_proto_len" }
                    if (field == 2) {
                        data = bytes.copyOfRange(i, i + len)
                    }
                    i += len
                }
                5 -> {
                    require(i + 4 <= bytes.size) { "truncated_proto_fixed32" }
                    i += 4
                }
                else -> error("unsupported_protobuf_wire_$wire")
            }
        }
        val d = data ?: error("bad_libp2p_private_proto")
        require(keyType >= 0) { "bad_libp2p_private_proto" }
        return ProtoPrivateKey(keyType, d)
    }

    private fun dataToEd25519Params(data: ByteArray): Ed25519PrivateKeyParameters {
        val seed = when (data.size) {
            64 -> data.copyOfRange(0, 32)
            32 -> data
            else -> error("bad_ed25519_private_len")
        }
        return Ed25519PrivateKeyParameters(seed, 0)
    }

    private fun parsePkcs8(content: ByteArray): Ed25519PrivateKeyParameters {
        val pki = PrivateKeyInfo.getInstance(content)
        val asym = PrivateKeyFactory.createKey(pki)
        return asym as? Ed25519PrivateKeyParameters
            ?: error("not_ed25519_pkcs8")
    }
}
