package tw.lospot.kin.wirelesshid.bluetooth.report

import tw.lospot.kin.wirelesshid.util.BitBoolean
import tw.lospot.kin.wirelesshid.util.ByteInArray

class ScrollableTrackpadMouseReport(val bytes: ByteArray = ByteArray(7)) {
    var leftButton by BitBoolean(bytes, 0, 0)
    var rightButton by BitBoolean(bytes, 0, 1)
    var middleButton by BitBoolean(bytes, 0, 2)
    var dxLsb by ByteInArray(bytes, 1)
    var dxMsb by ByteInArray(bytes, 2)
    var dyLsb by ByteInArray(bytes, 3)
    var dyMsb by ByteInArray(bytes, 4)
    var vScroll by ByteInArray(bytes, 5)
    var hScroll by ByteInArray(bytes, 6)

    fun setMove(dx: Int, dy: Int) {
        dxMsb = dx.shr(8).toByte()
        dyMsb = dy.shr(8).toByte()
        dxLsb = dx.toByte()
        dyLsb = dy.toByte()
    }

    companion object {
        const val ID = 4
    }
}