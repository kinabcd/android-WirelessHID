package tw.lospot.kin.wirelesshid.bluetooth.report

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

class ScrollableTrackpadMouseReport(
    val bytes: ByteArray = ByteArray(7) { 0 }
) {


    var leftButton: Boolean
        get() = bytes[0].hasBit(LEFT_BUTTON)
        set(value) {
            bytes[0] = bytes[0].setBit(LEFT_BUTTON, value)
        }

    var rightButton: Boolean
        get() = bytes[0].hasBit(RIGHT_BUTTON)
        set(value) {
            bytes[0] = bytes[0].setBit(RIGHT_BUTTON, value)
        }
    var middleButton: Boolean
        get() = bytes[0].hasBit(MIDDLE_BUTTON)
        set(value) {
            bytes[0] = bytes[0].setBit(MIDDLE_BUTTON, value)
        }

    var dxLsb: Byte
        get() = bytes[1]
        set(value) {
            bytes[1] = value
        }

    var dxMsb: Byte
        get() = bytes[2]
        set(value) {
            bytes[2] = value
        }


    var dyLsb: Byte
        get() = bytes[3]
        set(value) {
            bytes[3] = value
        }

    var dyMsb: Byte
        get() = bytes[4]
        set(value) {
            bytes[4] = value
        }

    var vScroll: Byte
        get() = bytes[5]
        set(value) {
            bytes[5] = value
        }

    var hScroll: Byte
        get() = bytes[6]
        set(value) {
            bytes[6] = value
        }


    fun reset() = bytes.fill(0)
    fun setMove(dx: Int, dy: Int) {
        dxMsb = dx.shr(8).toByte()
        dyMsb = dy.shr(8).toByte()
        dxLsb = dx.toByte()
        dyLsb = dy.toByte()
    }

    companion object {
        const val ID = 4
        const val LEFT_BUTTON: Byte = 0b00000001
        const val RIGHT_BUTTON: Byte = 0b00000010
        const val MIDDLE_BUTTON: Byte = 0b00000100
        private fun Byte.setBit(byte: Byte, value: Boolean) =
            if (value) this.or(byte) else this.and(byte.inv())

        private fun Byte.hasBit(byte: Byte) =
            this.and(byte) != 0.toByte()
    }
}