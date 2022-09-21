package tw.lospot.kin.wirelesshid.bluetooth

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.*
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import tw.lospot.kin.wirelesshid.bluetooth.report.KeyboardReport

class HidCallback(
    private val context: Context,
    private val device: BluetoothHidDevice,
) : BluetoothHidDevice.Callback() {
    companion object {
        private const val TAG = "HidCallback"
    }

    private var registered = false
    private val pairedDevices = HashSet<BluetoothDevice>()
    private val connectedDevices = HashSet<BluetoothDevice>()
    private val keyboardReport = KeyboardReport()
    private val downKey = HashSet<Byte>()

    fun registerApp() {
        if (registered) return
        Log.v(TAG, "registerApp()")
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "registerApp(), Permission denied")
            return
        }
        registered = device.registerApp(sdpRecord, null, qosOut, context.mainExecutor, this)
        if (!registered) {
            Log.w(TAG, "registerApp(), failed")
        }
    }

    fun unregisterApp() {
        if (!registered) return
        Log.v(TAG, "unregisterApp(), connectedDevices=$connectedDevices")
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "unregisterApp(), Permission denied")
            return
        }
        connectedDevices.forEach {
            device.disconnect(it)
        }
        device.unregisterApp()
        registered = false
    }

    fun sendKey(keyEvent: Int, down: Boolean) {
        Log.v(TAG, "sendKey($keyEvent, $down)")
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "sendKey(), Permission denied")
            return
        }
        when (keyEvent) {
            KeyEvent.KEYCODE_SHIFT_LEFT -> keyboardReport.leftShift = down
            KeyEvent.KEYCODE_SHIFT_RIGHT -> keyboardReport.rightShift = down
            KeyEvent.KEYCODE_CTRL_LEFT -> keyboardReport.leftControl = down
            KeyEvent.KEYCODE_CTRL_RIGHT -> keyboardReport.rightControl = down
            KeyEvent.KEYCODE_ALT_LEFT -> keyboardReport.leftAlt = down
            KeyEvent.KEYCODE_ALT_RIGHT -> keyboardReport.rightAlt = down
            KeyEvent.KEYCODE_META_LEFT -> keyboardReport.leftMeta = down
            KeyEvent.KEYCODE_META_RIGHT -> keyboardReport.rightMeta = down
        }
        val hidKey = KeyboardReport.KeyEventMap[keyEvent]?.toByte() ?: 0
        if (hidKey != 0.toByte()) {
            if (down) downKey.add(hidKey) else downKey.remove(hidKey)
        }
        keyboardReport.setKeys(downKey.toByteArray())
        connectedDevices.forEach {
            device.sendReport(it, KeyboardReport.ID, keyboardReport.bytes)
        }
    }

    override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
        super.onAppStatusChanged(pluggedDevice, registered)
        Log.v(TAG, "onAppStatusChanged($pluggedDevice, $registered)")
        if (pluggedDevice == null || !registered) return
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "onAppStatusChanged(), Permission denied")
            return
        }
        pairedDevices.addAll(
            device.getDevicesMatchingConnectionStates(
                intArrayOf(
                    BluetoothProfile.STATE_CONNECTED,
                    BluetoothProfile.STATE_CONNECTING,
                    BluetoothProfile.STATE_DISCONNECTED,
                    BluetoothProfile.STATE_DISCONNECTING,
                )
            )
        )
        pairedDevices.forEach {
            Log.v(TAG, "pairedDevices: $it")
        }
        device.connect(pluggedDevice)
    }

    override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
        super.onConnectionStateChanged(device, state)
        Log.v(TAG, "onConnectionStateChanged($device, ${state.toBtStateString()})")
        if (device == null) return
        if (state == BluetoothProfile.STATE_CONNECTED) {
            connectedDevices.add(device)
        } else {
            connectedDevices.remove(device)
        }
        pairedDevices.add(device)
    }

    override fun onGetReport(device: BluetoothDevice?, type: Byte, id: Byte, bufferSize: Int) {
        super.onGetReport(device, type, id, bufferSize)
    }

    override fun onSetReport(device: BluetoothDevice?, type: Byte, id: Byte, data: ByteArray?) {
        super.onSetReport(device, type, id, data)
    }

    override fun onSetProtocol(device: BluetoothDevice?, protocol: Byte) {
        super.onSetProtocol(device, protocol)
    }

    override fun onInterruptData(device: BluetoothDevice?, reportId: Byte, data: ByteArray?) {
        super.onInterruptData(device, reportId, data)
    }

    override fun onVirtualCableUnplug(device: BluetoothDevice?) {
        super.onVirtualCableUnplug(device)
    }

    private val sdpRecord by lazy {
        BluetoothHidDeviceAppSdpSettings(
            "Pixel HID1",
            "Mobile BController",
            "bla",
            BluetoothHidDevice.SUBCLASS1_COMBO,
            DescriptorCollection.MOUSE_KEYBOARD_COMBO
        )
    }
    private val qosOut by lazy {
        BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            0,
            21,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX
        )
    }

    private fun Int.toBtStateString() = when (this) {
        BluetoothProfile.STATE_CONNECTED -> "CONNECTED"
        BluetoothProfile.STATE_CONNECTING -> "CONNECTING"
        BluetoothProfile.STATE_DISCONNECTED -> "DISCONNECTED"
        BluetoothProfile.STATE_DISCONNECTING -> "DISCONNECTING"
        else -> "$this"
    }
}