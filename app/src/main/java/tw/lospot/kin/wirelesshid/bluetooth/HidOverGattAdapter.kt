package tw.lospot.kin.wirelesshid.bluetooth

import android.Manifest.permission.BLUETOOTH_ADVERTISE
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import tw.lospot.kin.wirelesshid.bluetooth.gatt.GattService
import tw.lospot.kin.wirelesshid.bluetooth.gatt.GattUUID.*
import tw.lospot.kin.wirelesshid.bluetooth.report.ConsumerReport
import tw.lospot.kin.wirelesshid.bluetooth.report.FeatureReport
import tw.lospot.kin.wirelesshid.bluetooth.report.KeyboardReport
import tw.lospot.kin.wirelesshid.bluetooth.report.ScrollableTrackpadMouseReport
import kotlin.properties.Delegates

class HidOverGattAdapter(
    private val context: Context,
    private val btManager: BluetoothManager,
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val onStateChanged: () -> Unit,
) : HidController {
    companion object {
        private const val TAG = "HidOverGattAdapter"
    }

    private val btAdapter = btManager.adapter
    private val btLeAdvertiser = btAdapter.bluetoothLeAdvertiser
    private var gattServer: BluetoothGattServer? = null
    private var batteryLevel: Byte = 99
    private val hidNotificationValue = HashMap<String, ByteArray?>()
    private val batteryNotificationValue = HashMap<String, ByteArray?>()
    private var protocolMode: Byte = 1
    private val keyboardReport = KeyboardReport()
    private val consumerReport = ConsumerReport()
    private val mouseReport = ScrollableTrackpadMouseReport()
    private val featureReport = FeatureReport()
    private val downKey = HashSet<Byte>()
    private val disconnectingTimeoutRunnable = Runnable { currentState = State.DISCONNECT_TIMEOUT }
    private val retryTimeoutRunnable = Runnable { currentState = State.REGISTERED }
    private val nextStateRunnable = Runnable { nextState() }
    override var isRunning = false
        set(value) {
            val new = value && checkSelfPermission()
            if (field != new) {
                field = new
                updateTargetState()
                onStateChanged()
            }
        }
    override var targetDevice: BluetoothDevice? by Delegates.observable(null) { _, _, _ ->
        updateTargetState()
        scheduleNextState()
        onStateChanged()
    }
        private set
    override var currentState by Delegates.observable(State.INITIALIZED) { _, old, new ->
        if (old != new) {
            Log.v(TAG, "currentState $new")
            scheduleNextState()
            onStateChanged()
        }
    }
        private set
    override var currentDevice: BluetoothDevice? by Delegates.observable(null) { _, _, _ ->
        onStateChanged()
    }
        private set
    private var targetState by Delegates.observable(State.INITIALIZED) { _, old, new ->
        if (old != new) {
            Log.v(TAG, "targetState $new")
            scheduleNextState()
            onStateChanged()
        }
    }

    private fun updateTargetState() {
        targetState = when {
            !isRunning -> State.INITIALIZED
            targetDevice != null -> State.CONNECTED
            else -> State.REGISTERED
        }
    }

    override fun selectDevice(address: String?) {
        Log.v(TAG, "selectDevice($address)")
        if (!checkSelfPermission()) {
            Log.w(TAG, "selectDevice(), Permission denied")
            return
        }
        targetDevice = if (address.isNullOrEmpty()) null else btAdapter.getRemoteDevice(address)
    }

    override fun sendKey(keyEvent: Int, down: Boolean) {
        if (currentState != State.CONNECTED) return
        if (!checkSelfPermission()) {
            Log.w(TAG, "sendKey(), Permission denied")
            return
        }
        if (sendConsumerKey(keyEvent, down)) return
        if (sendKeyboardKey(keyEvent, down)) return
    }

    override fun sendMouseKey(keyEventCode: Int, down: Boolean) {
        if (currentState != State.CONNECTED) return
        if (!checkSelfPermission()) {
            Log.w(TAG, "moveMouse(), Permission denied")
            return
        }
        when (keyEventCode) {
            MotionEvent.BUTTON_PRIMARY -> mouseReport.leftButton = down
            MotionEvent.BUTTON_SECONDARY -> mouseReport.rightButton = down
            MotionEvent.BUTTON_TERTIARY -> mouseReport.middleButton = down
            else -> return
        }
        sendReport(targetDevice, ScrollableTrackpadMouseReport.ID, mouseReport.bytes)
    }

    override fun sendMouseMove(dx: Int, dy: Int) {
        if (currentState != State.CONNECTED) return
        if (!checkSelfPermission()) {
            Log.w(TAG, "moveMouse(), Permission denied")
            return
        }
        mouseReport.setMove(dx, dy)
        sendReport(targetDevice, ScrollableTrackpadMouseReport.ID, mouseReport.bytes)
        mouseReport.setMove(0, 0)
    }

    override fun sendMouseScroll(dx: Int, dy: Int) {
        if (currentState != State.CONNECTED) return
        if (!checkSelfPermission()) {
            Log.w(TAG, "moveMouse(), Permission denied")
            return
        }
        mouseReport.hScroll = dx.toByte()
        mouseReport.vScroll = dy.toByte()
        sendReport(targetDevice, ScrollableTrackpadMouseReport.ID, mouseReport.bytes)
        mouseReport.hScroll = 0
        mouseReport.vScroll = 0
    }

    private fun sendKeyboardKey(keyEvent: Int, down: Boolean): Boolean {
        if (!checkSelfPermission()) {
            Log.w(TAG, "sendKeyboardKey(), Permission denied")
            return false
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
        sendReport(targetDevice, KeyboardReport.ID, keyboardReport.bytes)
        return true
    }

    private fun sendConsumerKey(keyEvent: Int, down: Boolean): Boolean {
        if (!checkSelfPermission()) {
            Log.w(TAG, "sendConsumerKey(), Permission denied")
            return false
        }

        when (keyEvent) {
            KeyEvent.KEYCODE_MEDIA_NEXT -> consumerReport.scanNextTrack = down
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> consumerReport.scanPrevTrack = down
            KeyEvent.KEYCODE_MEDIA_STOP -> consumerReport.stop = down
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> consumerReport.playPause = down
            KeyEvent.KEYCODE_VOLUME_MUTE -> consumerReport.mute = down
            KeyEvent.KEYCODE_VOLUME_UP -> consumerReport.volumeUp = down
            KeyEvent.KEYCODE_VOLUME_DOWN -> consumerReport.volumeDown = down
            else -> return false
        }
        sendReport(targetDevice, ConsumerReport.ID, consumerReport.bytes)
        return true
    }

    @RequiresPermission(BLUETOOTH_CONNECT)
    private fun sendReport(device: BluetoothDevice?, id: Int, data: ByteArray) {
        val gattServer = gattServer ?: return
        device ?: return
        val characteristic =
            gattServer.getService(SERVICE_BLE_HID.uuid)
                ?.getCharacteristic(CHARACTERISTIC_REPORT.uuid) ?: return
        val bytes = byteArrayOf(id.toByte()) + data
        notifyCharacteristicChanged(gattServer, device, characteristic, false, bytes)
    }

    @RequiresPermission(BLUETOOTH_CONNECT)
    private fun notifyCharacteristicChanged(
        gattServer: BluetoothGattServer,
        device: BluetoothDevice,
        characteristic: BluetoothGattCharacteristic, confirm: Boolean, bytes: ByteArray
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            gattServer.notifyCharacteristicChanged(device, characteristic, confirm, bytes)
        } else {
            characteristic.value = bytes
            gattServer.notifyCharacteristicChanged(device, characteristic, confirm)
        }
    }

    private fun scheduleNextState() {
        handler.post(nextStateRunnable)
    }

    private fun nextState() {
        handler.removeCallbacks(nextStateRunnable)
        if (!checkSelfPermission()) {
            Log.w(TAG, "nextState(), Permission denied")
            return
        }
        if (targetDevice == currentDevice && targetState == currentState) return
        handler.removeCallbacks(disconnectingTimeoutRunnable)
        when (currentState) {
            State.INITIALIZED -> if (targetState != State.INITIALIZED) registerApp()
            State.PROXYING -> {}
            State.STOPPED -> if (targetState != State.INITIALIZED) startAdvertising() else unregisterApp()
            State.REGISTERING -> {}
            State.REGISTERED -> when (targetState) {
                State.INITIALIZED -> stopAdvertising()
                State.CONNECTED -> connect()
                else -> {}
            }
            State.REGISTER_FAIL -> {
                handler.postDelayed({ currentState = State.INITIALIZED }, 5000)
            }
            State.CONNECTING -> {}
            State.DISCONNECTING -> {
                handler.postDelayed(disconnectingTimeoutRunnable, 5000)
            }
            State.CONNECTED -> {
                when (targetState) {
                    State.INITIALIZED -> disconnect()
                    State.REGISTERED -> disconnect()
                    State.CONNECTED -> if (targetDevice != currentDevice) disconnect()
                    else -> {}
                }
            }
            State.STOPPING -> {}
            State.CONNECT_FAIL -> when (targetState) {
                State.INITIALIZED -> {
                    handler.removeCallbacks(retryTimeoutRunnable)
                    stopAdvertising()
                }
                State.CONNECTED -> {
                    handler.postDelayed(retryTimeoutRunnable, 5000)
                }
                else -> {}
            }
            State.DISCONNECT_TIMEOUT -> {
                Log.w(TAG, "Disconnecting timeout")
                stopAdvertising()
            }
        }
    }

    private fun startAdvertising() {
        Log.v(TAG, "startAdvertising()")
        if (!checkSelfPermission()) {
            Log.w(TAG, "startAdvertising(), Permission denied")
            return
        }
        val advertiseSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        // set up advertising data
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(SERVICE_BLE_HID.parcelUuid)
            .build()
        currentState = State.REGISTERING
        btLeAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
    }

    private fun stopAdvertising() {
        Log.v(TAG, "stopAdvertising()")
        if (!checkSelfPermission()) {
            Log.w(TAG, "stopAdvertising(), Permission denied")
            return
        }
        try {
            btLeAdvertiser.stopAdvertising(advertiseCallback)
        } catch (e: IllegalStateException) {
            // BT Adapter is not turned ON
            Log.w(TAG, e)
        }
        currentState = State.STOPPED
    }

    private var waitingForAddedService = emptyList<BluetoothGattService>()
    private fun registerApp() {
        Log.v(TAG, "registerApp()")
        if (!checkSelfPermission()) {
            Log.w(TAG, "registerApp(), Permission denied")
            return
        }
        gattServer = btManager.openGattServer(context, gattServerCallback)
        waitingForAddedService = arrayListOf(
            GattService.deviceInformationService(),
            GattService.batteryService(),
            GattService.hidService(),
        )
        if (gattServer != null) {
            currentState = State.PROXYING
            ContextCompat.registerReceiver(
                context, batteryObserver, IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )?.let { updateBatteryLevel(it) }
            handler.post { addServices() }
        } else {
            isRunning = false
            currentState = State.INITIALIZED
        }
    }

    private fun addServices() {
        if (!checkSelfPermission()) {
            Log.w(TAG, "addServices(), Permission denied")
            return
        }
        if (waitingForAddedService.isEmpty()) {
            currentState = State.STOPPED
            return
        }
        gattServer?.addService(waitingForAddedService.last())
        waitingForAddedService = waitingForAddedService.dropLast(1)
    }

    private fun unregisterApp() {
        Log.v(TAG, "unregisterApp()")
        if (!checkSelfPermission()) {
            Log.w(TAG, "unregisterApp(), Permission denied")
            return
        }
        context.unregisterReceiver(batteryObserver)
        gattServer?.let {gattServer ->
            gattServer.clearServices()
            gattServer.close()
        }
        gattServer = null
        currentState = State.INITIALIZED
    }

    private fun connect() {
        Log.v(TAG, "connect(), target=$targetDevice, bond=${targetDevice?.bondState}")
        if (!checkSelfPermission()) {
            Log.w(TAG, "connect(), Permission denied")
            return
        }
        currentDevice = targetDevice
        currentState = State.CONNECTING
        gattServer?.connect(targetDevice, false)
        if (targetDevice?.bondState == BluetoothDevice.BOND_NONE) {
            targetDevice?.createBond()
        }
    }

    private fun disconnect() {
        Log.v(TAG, "disconnect(), current=$currentDevice target=$targetDevice")
        if (!checkSelfPermission()) {
            Log.w(TAG, "disconnect(), Permission denied")
            return
        }
        currentState = State.DISCONNECTING
        gattServer?.cancelConnection(currentDevice)
    }


    private val advertiseCallback: AdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.v(TAG, "onStartSuccess: $settingsInEffect")
            currentState = State.REGISTERED
        }

        override fun onStartFailure(errorCode: Int) {
            Log.v(TAG, "onStartFailure: $errorCode")
            isRunning = false
            currentState = State.STOPPED
        }
    }

    private fun onCharacteristicReadRequest(
        device: BluetoothDevice?, requestId: Int, offset: Int,
        characteristic: BluetoothGattCharacteristic?
    ) {
        Log.v(
            TAG,
            "onCharacteristicReadRequest: $device, $requestId, $offset, ${characteristic?.uuid}"
        )
        if (device == null || characteristic == null) return
        if (!checkSelfPermission()) return
        val response: ByteArray? = when (characteristic.service.uuid) {
            SERVICE_BATTERY.uuid -> byteArrayOf(batteryLevel)
            SERVICE_DEVICE_INFORMATION.uuid -> when (characteristic.uuid) {
                CHARACTERISTIC_MANUFACTURER_NAME.uuid -> "LoSpot".toByteArray()
                CHARACTERISTIC_MODEL_NUMBER.uuid -> "BLE HID".toByteArray()
                CHARACTERISTIC_SERIAL_NUMBER.uuid -> "1".toByteArray()
                else -> byteArrayOf()
            }
            SERVICE_BLE_HID.uuid -> when (characteristic.uuid) {
                CHARACTERISTIC_HID_INFORMATION.uuid -> byteArrayOf(0x11, 0x01, 0x00, 0x03)
                CHARACTERISTIC_REPORT_MAP.uuid ->
                    if (DescriptorCollection.MOUSE_KEYBOARD_COMBO.size - offset <= 0) byteArrayOf()
                    else DescriptorCollection.MOUSE_KEYBOARD_COMBO.drop(offset).toByteArray()
                CHARACTERISTIC_PROTOCOL_MODE.uuid -> byteArrayOf(protocolMode)
                CHARACTERISTIC_HID_CONTROL_POINT.uuid -> byteArrayOf(0x00)
                CHARACTERISTIC_REPORT.uuid -> byteArrayOf()
                else -> byteArrayOf()
            }
            else -> byteArrayOf()
        }
        gattServer?.sendResponse(
            device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response
        )
    }

    private fun onCharacteristicWriteRequest(
        device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?,
        preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
    ) {
        Log.v(
            TAG, "onCharacteristicWriteRequest: " +
                    "$device, $requestId, ${characteristic?.uuid}, " +
                    "$preparedWrite, $responseNeeded, $offset, $value"
        )
        if (device == null || characteristic == null) return
        if (!responseNeeded) return
        if (!checkSelfPermission()) return
        gattServer?.sendResponse(
            device, requestId, BluetoothGatt.GATT_SUCCESS, offset, byteArrayOf()
        )
    }

    private fun onDescriptorReadRequest(
        device: BluetoothDevice?, requestId: Int, offset: Int,
        descriptor: BluetoothGattDescriptor?
    ) {
        Log.v(TAG, "onDescriptorReadRequest: $device, $requestId, $offset, ${descriptor?.uuid}")
        if (device == null || descriptor == null) return
        if (!checkSelfPermission()) return
        val response: ByteArray? = when (descriptor.characteristic.service.uuid) {
            SERVICE_BATTERY.uuid -> when (descriptor.uuid) {
                DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION.uuid ->
                    batteryNotificationValue[device.address] ?: BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                else -> byteArrayOf()
            }
            SERVICE_BLE_HID.uuid -> when (descriptor.uuid) {
                DESCRIPTOR_REPORT_REFERENCE.uuid -> byteArrayOf(0x00, 0x01)
                DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION.uuid ->
                    hidNotificationValue[device.address] ?: BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                else -> byteArrayOf()
            }
            else -> byteArrayOf()
        }
        gattServer?.sendResponse(
            device, requestId, BluetoothGatt.GATT_SUCCESS, offset, response
        )
    }

    private fun onDescriptorWriteRequest(
        device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?,
        preparedWrite: Boolean, responseNeeded: Boolean, offset: Int,
        value: ByteArray?
    ) {
        Log.v(
            TAG, "onDescriptorWriteRequest: " +
                    "$device, $requestId, ${descriptor?.uuid}, " +
                    "$preparedWrite, $responseNeeded, $offset, $value"
        )
        if (device == null || descriptor == null) return
        when (descriptor.characteristic.service.uuid) {
            SERVICE_BATTERY.uuid -> when (descriptor.uuid) {
                DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION.uuid ->
                    batteryNotificationValue[device.address] = value
            }
            SERVICE_BLE_HID.uuid -> when (descriptor.uuid) {
                DESCRIPTOR_CLIENT_CHARACTERISTIC_CONFIGURATION.uuid ->
                    hidNotificationValue[device.address] = value
            }
            else -> byteArrayOf()
        }
        if (!responseNeeded) return
        if (!checkSelfPermission()) return
        gattServer?.sendResponse(
            device, requestId, BluetoothGatt.GATT_SUCCESS, offset, byteArrayOf()
        )
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            Log.v(TAG, "onConnectionStateChange: $device, $status, $newState")
            if (device == null || device != currentDevice) return
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> currentState = State.CONNECTED
                BluetoothProfile.STATE_CONNECTING -> currentState = State.CONNECTING
                BluetoothProfile.STATE_DISCONNECTING -> currentState = State.DISCONNECTING
                BluetoothProfile.STATE_DISCONNECTED -> currentState = when (currentState) {
                    State.CONNECT_FAIL -> State.CONNECT_FAIL
                    State.CONNECTING -> State.CONNECT_FAIL
                    State.CONNECTED -> State.REGISTERED
                    State.DISCONNECTING -> State.REGISTERED
                    else -> currentState
                }
            }
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            super.onServiceAdded(status, service)
            Log.v(TAG, "onServiceAdded: $status, $service")
            handler.post { addServices() }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?, requestId: Int, offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            handler.post {
                this@HidOverGattAdapter.onCharacteristicReadRequest(
                    device, requestId, offset, characteristic
                )
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device, requestId, characteristic, preparedWrite, responseNeeded, offset, value
            )
            handler.post {
                this@HidOverGattAdapter.onCharacteristicWriteRequest(
                    device, requestId, characteristic, preparedWrite, responseNeeded, offset, value
                )
            }
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice?, requestId: Int, offset: Int,
            descriptor: BluetoothGattDescriptor?
        ) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor)
            handler.post {
                this@HidOverGattAdapter.onDescriptorReadRequest(
                    device, requestId, offset, descriptor
                )
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?, requestId: Int, descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean, responseNeeded: Boolean, offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(
                device, requestId, descriptor, preparedWrite, responseNeeded, offset, value
            )
            handler.post {
                this@HidOverGattAdapter.onDescriptorWriteRequest(
                    device, requestId, descriptor, preparedWrite, responseNeeded, offset, value
                )
            }
        }

        override fun onExecuteWrite(device: BluetoothDevice?, requestId: Int, execute: Boolean) {
            super.onExecuteWrite(device, requestId, execute)
            Log.v(TAG, "onExecuteWrite: $device, $requestId, $execute")
        }

        override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
            super.onNotificationSent(device, status)
            //Log.v(TAG, "onNotificationSent: $device, $status")
        }

        override fun onMtuChanged(device: BluetoothDevice?, mtu: Int) {
            super.onMtuChanged(device, mtu)
            Log.v(TAG, "onMtuChanged: $device, $mtu")
        }

        override fun onPhyUpdate(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyUpdate(device, txPhy, rxPhy, status)
            Log.v(TAG, "onPhyUpdate: $device, $txPhy, $rxPhy, $status")
        }

        override fun onPhyRead(device: BluetoothDevice?, txPhy: Int, rxPhy: Int, status: Int) {
            super.onPhyRead(device, txPhy, rxPhy, status)
            Log.v(TAG, "onPhyRead: $device, $txPhy, $rxPhy, $status")
        }
    }

    private val batteryObserver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            intent?.let { updateBatteryLevel(it) }
        }
    }

    private fun updateBatteryLevel(intent: Intent) {
        batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1).toByte()
        if (!checkSelfPermission()) {
            Log.w(TAG, "sendConsumerKey(), Permission denied")
            return
        }
        val gattServer = gattServer ?: return
        val characteristic = gattServer.getService(SERVICE_BATTERY.uuid)
            ?.getCharacteristic(CHARACTERISTIC_BATTERY_LEVEL.uuid) ?: return
        btManager.getConnectedDevices(BluetoothProfile.GATT_SERVER).forEach {
            Log.v(TAG, "${it.address} - notify battery($batteryLevel)")
            notifyCharacteristicChanged(
                gattServer, it, characteristic, false, byteArrayOf(batteryLevel)
            )
        }
    }

    private fun checkSelfPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        checkAllPermissions(BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT) else true

    private fun checkAllPermissions(vararg permissions: String) = permissions.all {
        checkSelfPermission(context, it) == PERMISSION_GRANTED
    }
}