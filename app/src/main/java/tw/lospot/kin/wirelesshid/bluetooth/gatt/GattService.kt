package tw.lospot.kin.wirelesshid.bluetooth.gatt

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.*
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY
import tw.lospot.kin.wirelesshid.bluetooth.gatt.GattUUID.*

object GattService {
    fun deviceInformationService() = BluetoothGattService(
        SERVICE_DEVICE_INFORMATION.uuid,
        SERVICE_TYPE_PRIMARY
    ).apply {
        characteristic(
            CHARACTERISTIC_MANUFACTURER_NAME,
            PROPERTY_READ,
            PERMISSION_READ_ENCRYPTED
        )
        characteristic(
            CHARACTERISTIC_MODEL_NUMBER,
            PROPERTY_READ,
            PERMISSION_READ_ENCRYPTED
        )
        characteristic(
            CHARACTERISTIC_SERIAL_NUMBER,
            PROPERTY_READ,
            PERMISSION_READ_ENCRYPTED
        )

    }

    fun batteryService() = BluetoothGattService(SERVICE_BATTERY.uuid, SERVICE_TYPE_PRIMARY).apply {
        characteristic(
            CHARACTERISTIC_BATTERY_LEVEL,
            PROPERTY_NOTIFY or PROPERTY_READ,
            PERMISSION_READ_ENCRYPTED
        ) {
            descriptor(
                DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
        }

    }

    fun hidService() = BluetoothGattService(SERVICE_BLE_HID.uuid, SERVICE_TYPE_PRIMARY).apply {
        characteristic(
            CHARACTERISTIC_HID_INFORMATION,
            PROPERTY_READ,
            PERMISSION_READ_ENCRYPTED
        )

        characteristic(
            CHARACTERISTIC_REPORT_MAP,
            PROPERTY_READ,
            PERMISSION_READ_ENCRYPTED
        )
        characteristic(
            CHARACTERISTIC_PROTOCOL_MODE,
            PROPERTY_READ or PROPERTY_WRITE_NO_RESPONSE,
            PERMISSION_READ_ENCRYPTED or PERMISSION_WRITE_ENCRYPTED
        ) {
            writeType = WRITE_TYPE_NO_RESPONSE
        }

        characteristic(
            CHARACTERISTIC_HID_CONTROL_POINT,
            PROPERTY_WRITE_NO_RESPONSE,
            PERMISSION_WRITE_ENCRYPTED
        ) {
            writeType = WRITE_TYPE_NO_RESPONSE
        }
        characteristic(
            CHARACTERISTIC_REPORT,
            PROPERTY_NOTIFY or PROPERTY_READ or PROPERTY_WRITE,
            PERMISSION_READ_ENCRYPTED or PERMISSION_WRITE_ENCRYPTED
        ) {
            descriptor(
                DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED
            )
            descriptor(
                DESCRIPTOR_REPORT_REFERENCE,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED
            )
        }
        characteristic(
            CHARACTERISTIC_REPORT,
            PROPERTY_READ or PROPERTY_WRITE or PROPERTY_WRITE_NO_RESPONSE,
            PERMISSION_READ_ENCRYPTED or PERMISSION_WRITE_ENCRYPTED
        ) {
            writeType = WRITE_TYPE_NO_RESPONSE
            descriptor(
                DESCRIPTOR_REPORT_REFERENCE,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED
            )
        }

        characteristic(
            CHARACTERISTIC_REPORT,
            PROPERTY_READ or PROPERTY_WRITE,
            PERMISSION_READ_ENCRYPTED or PERMISSION_WRITE_ENCRYPTED
        ) {
            descriptor(
                DESCRIPTOR_REPORT_REFERENCE,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED or BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED
            )
        }
    }

    private fun BluetoothGattService.characteristic(
        uuid: GattUUID, properties: Int, permissions: Int,
        block: BluetoothGattCharacteristic.() -> Unit = {}
    ) = BluetoothGattCharacteristic(uuid.uuid, properties, permissions).also {
        it.block()
        addCharacteristic(it)
    }

    private fun BluetoothGattCharacteristic.descriptor(
        uuid: GattUUID, permissions: Int
    ) = BluetoothGattDescriptor(uuid.uuid, permissions).also {
        addDescriptor(it)
    }
}