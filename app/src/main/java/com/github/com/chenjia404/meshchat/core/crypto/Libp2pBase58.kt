package com.github.com.chenjia404.meshchat.core.crypto

/**
 * Bitcoin/libp2p 使用的 Base58 编码（与 Go `github.com/mr-tron/base58` 一致）。
 */
internal object Libp2pBase58 {
    private const val ALPHABET =
        "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun encode(input: ByteArray): String {
        if (input.isEmpty()) return ""
        var zeros = 0
        while (zeros < input.size && input[zeros].toInt() == 0) zeros++
        val size = (input.size - zeros) * 138 / 100 + 1
        val buf = ByteArray(size)
        var outLen = 0
        for (i in zeros until input.size) {
            var carry = input[i].toInt() and 0xff
            var j = 0
            while (j < outLen || carry != 0) {
                carry += 256 * (if (j < outLen) buf[outLen - 1 - j].toInt() and 0xff else 0)
                val rem = carry % 58
                carry /= 58
                if (j < outLen) {
                    buf[outLen - 1 - j] = rem.toByte()
                } else {
                    buf[outLen++] = rem.toByte()
                }
                j++
            }
        }
        val str = CharArray(zeros + outLen)
        var k = 0
        repeat(zeros) { str[k++] = '1' }
        for (i in 0 until outLen) {
            str[k++] = ALPHABET[buf[outLen - 1 - i].toInt() and 0xff]
        }
        return String(str)
    }
}
