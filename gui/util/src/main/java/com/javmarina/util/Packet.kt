package com.javmarina.util

import kotlin.math.abs


/**
 * A packet encapsulates the controller state at a given instant. Can be converted to a byte array that is suitable for
 * communication between parties (client-server and PC-MCU).
 */
class Packet constructor(val buttons: Buttons, val dpad: Dpad,
                         val leftJoystick: Joystick, val rightJoystick: Joystick) {

    companion object {
        val EMPTY_PACKET: Packet = empty()
        val EMPTY_PACKET_BUFFER: ByteArray = EMPTY_PACKET.getBuffer()
        val PACKET_BUFFER_LENGTH = EMPTY_PACKET_BUFFER.size

        const val VENDORSPEC: Byte = 0x00

        private fun empty(): Packet {
            return Packet(Buttons.noButtons(), Dpad.center(), Joystick.centered(), Joystick.centered())
        }
    }

    private lateinit var buffer: ByteArray

    constructor(buffer: ByteArray) : this(
            buttons=Buttons.fromBytes(buffer[0], buffer[1]),
            dpad=Dpad.fromByte(buffer[2]),
            leftJoystick=Joystick.fromBytes(buffer[3], buffer[4]),
            rightJoystick= Joystick.fromBytes(buffer[5], buffer[6])
    ) {
        assert(buffer.size == 8)
        assert(buffer[7] == VENDORSPEC)
        this.buffer = buffer
    }

    /**
     * Convert this packet to a byte array representation
     * * B[0]: 0,0,CAPTURE,HOME,RCLICK,LCLICK,PLUS,MINUS
     * * B[1]: ZR,ZL,R,L,X,A,B,Y
     * * B[2]: 0,0,0,0,DPAD (see [Dpad])
     * * B[3]: value between 0 and 255 for X axis of left joystick
     * * B[4]: value between 0 and 255 for Y axis of left joystick
     * * B[5]: value between 0 and 255 for X axis of right joystick
     * * B[6]: value between 0 and 255 for Y axis of right joystick
     * * B[7]: vendor spec (in this case, 0000 0000)
     *
     * [Further reference](https://github.com/ItsDeidara/CommunityController/blob/master/Required%20Library/switch_controller.py)
     */
    fun getBuffer(): ByteArray {
        if (!::buffer.isInitialized) {
            // Generate buffer
            val temp = ByteArray(8)

            // Buttons
            val buttonsValue: Short = buttons.toShort()
            temp[0] = (buttonsValue.toInt() ushr 8).toByte()
            temp[1] = buttonsValue.toByte()

            // DPAD
            temp[2] = dpad.toByte()

            // Left joystick
            leftJoystick.toBytes().copyInto(temp, 3, 0, 2)

            // Right joystick
            rightJoystick.toBytes().copyInto(temp, 5, 0, 2)

            // Vendorspec
            temp[7] = VENDORSPEC

            buffer = temp
        }
        return buffer
    }

    data class Buttons(
            val y: Boolean,
            val b: Boolean,
            val a: Boolean,
            val x: Boolean,
            val l: Boolean,
            val r: Boolean,
            val zl: Boolean,
            val zr: Boolean,
            val minus: Boolean,
            val plus: Boolean,
            val lclick: Boolean,
            val rclick: Boolean,
            val home: Boolean,
            val capture: Boolean
    ) {

        // Buttons take up 14 bits (there are 14 buttons)
        enum class Code(val value: Int) {
            NONE(0x00),
            Y(0x01),
            B(0x02),
            A(0x04),
            X(0x08),
            L(0x10),
            R(0x20),
            ZL(0x40),
            ZR(0x80),
            MINUS(0x100),
            PLUS(0x200),
            LCLICK(0x400),
            RCLICK(0x800),
            HOME(0x1000),
            CAPTURE(0x2000)
        }

        constructor(provider: Provider) : this(
                y=provider.isButtonPressed(Code.Y),
                b=provider.isButtonPressed(Code.B),
                a=provider.isButtonPressed(Code.A),
                x=provider.isButtonPressed(Code.X),
                l=provider.isButtonPressed(Code.L),
                r=provider.isButtonPressed(Code.R),
                zl=provider.isButtonPressed(Code.ZL),
                zr=provider.isButtonPressed(Code.ZR),
                minus=provider.isButtonPressed(Code.MINUS),
                plus=provider.isButtonPressed(Code.PLUS),
                lclick=provider.isButtonPressed(Code.LCLICK),
                rclick=provider.isButtonPressed(Code.RCLICK),
                home=provider.isButtonPressed(Code.HOME),
                capture=provider.isButtonPressed(Code.CAPTURE)
        )

        fun toShort(): Short {
            var i = 0
            i = i or if (y) Code.Y.value else 0
            i = i or if (b) Code.B.value else 0
            i = i or if (a) Code.A.value else 0
            i = i or if (x) Code.X.value else 0
            i = i or if (l) Code.L.value else 0
            i = i or if (r) Code.R.value else 0
            i = i or if (zl) Code.ZL.value else 0
            i = i or if (zr) Code.ZR.value else 0
            i = i or if (minus) Code.MINUS.value else 0
            i = i or if (plus) Code.PLUS.value else 0
            i = i or if (lclick) Code.LCLICK.value else 0
            i = i or if (rclick) Code.RCLICK.value else 0
            i = i or if (home) Code.HOME.value else 0
            i = i or if (capture) Code.CAPTURE.value else 0

            return i.toShort()
        }

        companion object {
            fun noButtons(): Buttons {
                return Buttons(y = false, b = false, a = false, x = false, l = false, r = false, zl = false, zr = false,
                        minus = false, plus = false, lclick = false, rclick = false, home = false, capture = false)
            }

            @Suppress("EXPERIMENTAL_API_USAGE")
            fun fromBytes(b0: Byte, b1: Byte): Buttons {
                val s = (b0.toUByte().toInt() shl 8 or b1.toUByte().toInt()).toShort()
                return fromShort(s)
            }

            private fun fromShort(s: Short): Buttons {
                val i: Int = s.toInt() and 0xFFFF
                return Buttons(
                        y = i and Code.Y.value  != 0,
                        b = i and Code.B.value  != 0,
                        a = i and Code.A.value  != 0,
                        x = i and Code.X.value  != 0,
                        l = i and Code.L.value  != 0,
                        r = i and Code.R.value  != 0,
                        zl = i and Code.ZL.value  != 0,
                        zr = i and Code.ZR.value  != 0,
                        minus = i and Code.MINUS.value  != 0,
                        plus = i and Code.PLUS.value  != 0,
                        lclick = i and Code.LCLICK.value  != 0,
                        rclick = i and Code.RCLICK.value  != 0,
                        home = i and Code.HOME.value  != 0,
                        capture = i and Code.CAPTURE.value  != 0,
                )
            }
        }

        interface Provider {
            fun isButtonPressed(buttonCode: Code): Boolean
        }
    }

    data class Dpad(
            val up: Boolean,
            val right: Boolean,
            val down: Boolean,
            val left: Boolean
    ) {

        fun toByte(): Byte {
            return when {
                left -> {
                    when {
                        up -> UP_LEFT
                        down -> DOWN_LEFT
                        else -> LEFT
                    }
                }
                right -> {
                    when {
                        up -> UP_RIGHT
                        down -> DOWN_RIGHT
                        else -> RIGHT
                    }
                }
                up -> UP
                down -> DOWN
                else -> CENTER
            }
        }

        companion object {
            // Only one option at a time: from 0000 to 1000
            // In theory, it takes 4 bits, but packet converts it to a byte (adds 4 leading zeros)
            const val UP: Byte         = 0x00
            const val UP_RIGHT: Byte   = 0x01
            const val RIGHT: Byte      = 0x02
            const val DOWN_RIGHT: Byte = 0x03
            const val DOWN: Byte       = 0x04
            const val DOWN_LEFT: Byte  = 0x05
            const val LEFT: Byte       = 0x06
            const val UP_LEFT: Byte    = 0x07
            const val CENTER: Byte     = 0x08

            @JvmStatic
            fun center(): Dpad {
                return Dpad(up = false, right = false, down = false, left = false)
            }

            internal fun fromByte(b: Byte): Dpad {
                return when (b) {
                    UP         -> Dpad(up = true,  right = false, down = false, left = false)
                    UP_RIGHT   -> Dpad(up = true,  right = true,  down = false, left = false)
                    RIGHT      -> Dpad(up = false, right = true,  down = false, left = false)
                    DOWN_RIGHT -> Dpad(up = false, right = true,  down = true,  left = false)
                    DOWN       -> Dpad(up = false, right = false, down = true,  left = false)
                    DOWN_LEFT  -> Dpad(up = false, right = false, down = true,  left = true)
                    LEFT       -> Dpad(up = false, right = false, down = false, left = true)
                    UP_LEFT    -> Dpad(up = true,  right = false, down = false, left = true)
                    CENTER     -> Dpad(up = false, right = false, down = false, left = false)
                    else       -> throw IllegalArgumentException("Invalid value: $b")
                }
            }
        }
    }

    data class Joystick(
            val x: Float,
            val y: Float
    ) {
        fun toBytes(): ByteArray {
            assert(x in MIN..MAX)
            assert(y in MIN..MAX)

            var bx = ((x + 1.0) / 2.0 * 255).toInt().toByte()
            var by = ((y + 1.0) / 2.0 * 255).toInt().toByte()

            // If value is too close to center position, use it instead
            // This tries to emulate a real controller, and it also avoids
            // drifting when the stick is not touched
            bx = if (abs(bx - CENTER_INTEGER) < 10) CENTER else bx
            by = if (abs(by - CENTER_INTEGER) < 10) CENTER else by

            return byteArrayOf(bx, by)
        }

        companion object {
            const val CENTER = 0x80.toByte()
            const val CENTER_INTEGER = 0x80 // 128

            const val MIN = -1.0f
            const val MAX = 1.0f

            @JvmStatic
            fun centered(): Joystick {
                return Joystick(0.0f, 0.0f)
            }

            @Suppress("EXPERIMENTAL_API_USAGE")
            internal fun fromBytes(bx: Byte, by: Byte): Joystick {
                val x: Float = 2 * bx.toUByte().toInt() / 255.0f - 1.0f
                val y: Float = 2 * by.toUByte().toInt() / 255.0f - 1.0f
                return Joystick(x=x, y=y)
            }
        }
    }
}
