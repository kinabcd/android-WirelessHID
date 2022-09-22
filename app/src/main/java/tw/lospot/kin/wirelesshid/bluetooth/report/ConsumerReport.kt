package tw.lospot.kin.wirelesshid.bluetooth.report

import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

class ConsumerReport(val bytes: ByteArray = ByteArray(1)) {

    var scanNextTrack: Boolean
        get() = bytes[0].hasBit(SCAN_NEXT_TRACK)
        set(value) {
            bytes[0] = bytes[0].setBit(SCAN_NEXT_TRACK, value)
        }
    var scanPrevTrack: Boolean
        get() = bytes[0].hasBit(SCAN_PREV_TRACK)
        set(value) {
            bytes[0] = bytes[0].setBit(SCAN_PREV_TRACK, value)
        }
    var stop: Boolean
        get() = bytes[0].hasBit(STOP)
        set(value) {
            bytes[0] = bytes[0].setBit(STOP, value)
        }
    var playPause: Boolean
        get() = bytes[0].hasBit(PLAY_PAUSE)
        set(value) {
            bytes[0] = bytes[0].setBit(PLAY_PAUSE, value)
        }
    var mute: Boolean
        get() = bytes[0].hasBit(MUTE)
        set(value) {
            bytes[0] = bytes[0].setBit(MUTE, value)
        }
    var volumeUp: Boolean
        get() = bytes[0].hasBit(VOLUME_UP)
        set(value) {
            bytes[0] = bytes[0].setBit(VOLUME_UP, value)
        }
    var volumeDown: Boolean
        get() = bytes[0].hasBit(VOLUME_DOWN)
        set(value) {
            bytes[0] = bytes[0].setBit(VOLUME_DOWN, value)
        }

    companion object {
        const val ID = 3
        const val SCAN_NEXT_TRACK: Byte = 0b00000001
        const val SCAN_PREV_TRACK: Byte = 0b00000010
        const val STOP: Byte = 0b00000100
        const val PLAY_PAUSE: Byte = 0b00001000
        const val MUTE: Byte = 0b00010000
        const val VOLUME_UP: Byte = 0b00100000
        const val VOLUME_DOWN: Byte = 0b01000000
        private fun Byte.setBit(byte: Byte, value: Boolean) =
            if (value) this.or(byte) else this.and(byte.inv())

        private fun Byte.hasBit(byte: Byte) =
            this.and(byte) != 0.toByte()
    }
}