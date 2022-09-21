package tw.lospot.kin.wirelesshid.bluetooth.report

class AbsMouseReport(
    val bytes: ByteArray = ByteArray(4)
) {

    var x: Int
        get() = (bytes[1].toUByte().toInt() shl 8) or bytes[0].toUByte().toInt()
        set(value) {
            bytes[0] = (value and 0xff).toByte()
            bytes[1] = ((value shr 8) and 0xff).toByte()
        }

    var y: Int
        get() = (bytes[3].toUByte().toInt() shl 8) or bytes[2].toUByte().toInt()
        set(value) {
            bytes[2] = (value and 0xff).toByte()
            bytes[3] = ((value shr 8) and 0xff).toByte()
        }

}