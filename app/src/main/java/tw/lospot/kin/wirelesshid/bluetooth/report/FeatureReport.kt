package tw.lospot.kin.wirelesshid.bluetooth.report

import tw.lospot.kin.wirelesshid.util.BitBoolean

class FeatureReport(val bytes: ByteArray = ByteArray(1)) {
    var wheelResolutionMultiplier by BitBoolean(bytes, 0, 0)
    var acPanResolutionMultiplier by BitBoolean(bytes, 0, 2)

    companion object {
        const val ID = 6.toByte()
    }
}