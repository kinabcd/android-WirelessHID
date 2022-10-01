package tw.lospot.kin.wirelesshid.bluetooth.report

import tw.lospot.kin.wirelesshid.util.BitBoolean

class ConsumerReport(val bytes: ByteArray = ByteArray(1)) {
    var scanNextTrack by BitBoolean(bytes, 0, 0)
    var scanPrevTrack by BitBoolean(bytes, 0, 1)
    var stop by BitBoolean(bytes, 0, 2)
    var playPause by BitBoolean(bytes, 0, 3)
    var mute by BitBoolean(bytes, 0, 4)
    var volumeUp by BitBoolean(bytes, 0, 5)
    var volumeDown by BitBoolean(bytes, 0, 6)

    companion object {
        const val ID = 3
    }
}