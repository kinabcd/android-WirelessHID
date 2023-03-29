package tw.lospot.kin.wirelesshid.bluetooth.gatt

import android.os.ParcelUuid
import java.util.*

enum class GattUUID(private val uuidString: String) {
    SERVICE_DEVICE_INFORMATION(fromShort(0x180A)),
    CHARACTERISTIC_MANUFACTURER_NAME(fromShort(0x2A29)),
    CHARACTERISTIC_MODEL_NUMBER(fromShort(0x2A24)),
    CHARACTERISTIC_SERIAL_NUMBER(fromShort(0x2A25)),

    SERVICE_BATTERY(fromShort(0x180F)),
    CHARACTERISTIC_BATTERY_LEVEL(fromShort(0x2A19)),
    DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION(fromShort(0x2902)),

    SERVICE_BLE_HID(fromShort(0x1812)),
    CHARACTERISTIC_HID_INFORMATION(fromShort(0x2A4A)),
    CHARACTERISTIC_REPORT_MAP(fromShort(0x2A4B)),
    CHARACTERISTIC_HID_CONTROL_POINT(fromShort(0x2A4C)),
    CHARACTERISTIC_REPORT(fromShort(0x2A4D)),
    CHARACTERISTIC_PROTOCOL_MODE(fromShort(0x2A4E)),
    DESCRIPTOR_REPORT_REFERENCE(fromShort(0x2908))
    ;

    val uuid get() = UUID.fromString(uuidString)
    val parcelUuid get() = ParcelUuid.fromString(uuidString)
}
private fun fromShort(v: Int): String = "0000%04X-0000-1000-8000-00805F9B34FB".format(v.and(0xffff))