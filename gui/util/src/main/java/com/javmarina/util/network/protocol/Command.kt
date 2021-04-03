package com.javmarina.util.network.protocol


data class Command(val id: Byte, val payload: ByteArray) {

    companion object Factory {
        fun fromByteArray(buffer: ByteArray): Command {
            val payload = ByteArray(buffer.size-1)
            System.arraycopy(buffer, 1, payload, 0, payload.size)
            return Command(buffer[0], payload)
        }
    }

    constructor(id: Byte) : this(id, ByteArray(0))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Command

        if (id != other.id) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.toInt()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
