package tw.lospot.kin.wirelesshid.bluetooth

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.RequiresPermission
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import tw.lospot.kin.wirelesshid.bluetooth.report.ConsumerReport
import tw.lospot.kin.wirelesshid.bluetooth.report.KeyboardReport
import tw.lospot.kin.wirelesshid.bluetooth.report.ScrollableTrackpadMouseReport
import kotlin.properties.Delegates

class HidCallback(
    private val context: Context,
    private val btAdapter: BluetoothAdapter,
    private val handler: Handler = Handler(Looper.getMainLooper()),
) : BluetoothHidDevice.Callback(), BluetoothProfile.ServiceListener {
    companion object {
        private const val TAG = "HidCallback"
    }

    enum class State {
        INIT, PROXYING, STOPPED, STOPPING, REGISTERING, REGISTERED, CONNECTING, DISCONNECTING, CONNECTED
    }

    private var device: BluetoothHidDevice? = null
    private var targetState by Delegates.observable(State.INIT) { _, _, new ->
        Log.v(TAG, "targetState $new")
        nextState()
    }
    private var targetDevice: BluetoothDevice? by Delegates.observable(null) { _, _, _ -> nextState() }
    private var currentState by Delegates.observable(State.INIT) { _, _, new ->
        Log.v(TAG, "currentState $new")
        nextState()
    }
    private var currentDevice: BluetoothDevice? by Delegates.observable(null) { _, _, _ -> }
    private val keyboardReport = KeyboardReport()
    private val consumerReport = ConsumerReport()
    private val mouseReport = ScrollableTrackpadMouseReport()
    private val downKey = HashSet<Byte>()
    private val disconnectingTimeoutRunnable = Runnable {
        Log.w(TAG, "Disconnecting timeout")
        unregisterApp()
    }

    fun selectDevice(address: String?) {
        Log.v(TAG, "selectDevice($address)")
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "selectDevice(), Permission denied")
            return
        }
        targetDevice = btAdapter.bondedDevices.firstOrNull { it.address == address }
        targetState = if (targetDevice != null) State.CONNECTED else State.INIT
    }

    fun sendKey(keyEvent: Int, down: Boolean) {
        Log.v(TAG, "sendKey($keyEvent, $down)")
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "sendKey(), Permission denied")
            return
        }
        if (sendConsumerKey(keyEvent, down)) return
        if (sendKeyboardKey(keyEvent, down)) return
    }

    fun sendMouseKey(keyEventCode: Int, down: Boolean) {
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "moveMouse(), Permission denied")
            return
        }
        when (keyEventCode) {
            MotionEvent.BUTTON_PRIMARY -> mouseReport.leftButton = down
            MotionEvent.BUTTON_SECONDARY -> mouseReport.rightButton = down
            MotionEvent.BUTTON_TERTIARY -> mouseReport.middleButton = down
            else -> return
        }

        targetDevice?.let {
            device!!.sendReport(it, ScrollableTrackpadMouseReport.ID, mouseReport.bytes)
        }
    }

    fun moveMouse(dx: Int, dy: Int) {
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "moveMouse(), Permission denied")
            return
        }
        mouseReport.setMove(dx, dy)
        targetDevice?.let {
            device!!.sendReport(it, ScrollableTrackpadMouseReport.ID, mouseReport.bytes)
        }
    }

    private fun nextState() {
        handler.post {
            if (targetDevice == currentDevice && targetState == currentState) return@post
            handler.removeCallbacks(disconnectingTimeoutRunnable)
            when (currentState) {
                State.INIT -> if (targetState != State.INIT) startProxy()
                State.PROXYING -> {}
                State.STOPPED -> if (targetState != State.INIT) registerApp() else closeProxy()
                State.REGISTERING -> {}
                State.REGISTERED -> when (targetState) {
                    State.INIT -> unregisterApp()
                    State.CONNECTED -> connect()
                    else -> {}
                }
                State.CONNECTING -> {}
                State.DISCONNECTING -> {
                    handler.postDelayed(disconnectingTimeoutRunnable, 5000)
                }
                State.CONNECTED -> when (targetState) {
                    State.INIT -> unregisterApp()
                    State.CONNECTED -> if (targetDevice != currentDevice) disconnect()
                    else -> {}
                }
                State.STOPPING -> {}
            }
        }
    }

    private fun startProxy() {
        if (currentState != State.INIT) return
        Log.v(TAG, "startProxy()")
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "startProxy(), Permission denied")
            return
        }
        if (btAdapter.getProfileProxy(context, this, BluetoothProfile.HID_DEVICE)) {
            currentState = State.PROXYING
        }
    }

    private fun closeProxy() {
        if (currentState in arrayOf(State.INIT, State.PROXYING)) return
        Log.v(TAG, "closeProxy()")
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "closeProxy(), Permission denied")
            return
        }
        btAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, device)
        device = null
        currentState = State.INIT
    }

    private fun registerApp() {
        if (currentState != State.STOPPED) return
        Log.v(TAG, "registerApp()")
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "registerApp(), Permission denied")
            return
        }
        if (device!!.registerApp(sdpRecord, null, qosOut, context.mainExecutor, this)) {
            currentState = State.REGISTERING
        } else {
            Log.w(TAG, "registerApp(), Failed")
            targetState = State.INIT
        }

    }

    private fun unregisterApp() {
        if (currentState in arrayOf(State.STOPPED, State.STOPPING)) return
        Log.v(TAG, "unregisterApp()")
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "unregisterApp(), Permission denied")
            return
        }
        if (currentState == State.CONNECTED) disconnect()
        currentDevice = null
        currentState = State.STOPPING
        device!!.unregisterApp()
    }

    private fun connect() {
        Log.v(TAG, "connect(), target=$targetDevice")
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "connect(), Permission denied")
            return
        }
        currentDevice = targetDevice
        currentState = State.CONNECTING
        device!!.connect(targetDevice)
    }

    private fun disconnect() {
        Log.v(TAG, "disconnect(), current=$currentDevice target=$targetDevice")
        if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            Log.w(TAG, "unregisterApp(), Permission denied")
            return
        }
        currentState = State.DISCONNECTING
        device!!.disconnect(currentDevice)
    }

    @RequiresPermission(value = BLUETOOTH_CONNECT)
    private fun sendKeyboardKey(keyEvent: Int, down: Boolean): Boolean {

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
        targetDevice?.let {
            device!!.sendReport(it, KeyboardReport.ID, keyboardReport.bytes)
        }
        return true
    }

    @RequiresPermission(value = BLUETOOTH_CONNECT)
    private fun sendConsumerKey(keyEvent: Int, down: Boolean): Boolean {
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
        targetDevice?.let {
            device!!.sendReport(it, ConsumerReport.ID, consumerReport.bytes)
        }
        return true
    }

    override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
        Log.v(TAG, "onAppStatusChanged(${pluggedDevice.nameAddress()}, $registered)")
        when {
            !registered -> currentState = State.STOPPED
            currentState == State.REGISTERING -> currentState = State.REGISTERED
        }
    }

    override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
        Log.v(TAG, "onConnectionStateChanged(${device.nameAddress()}, ${state.toBtStateString()})")
        when (device) {
            null -> return
            currentDevice -> when (state) {
                BluetoothProfile.STATE_CONNECTED -> currentState = State.CONNECTED
                BluetoothProfile.STATE_CONNECTING -> currentState = State.CONNECTING
                BluetoothProfile.STATE_DISCONNECTING -> currentState = State.DISCONNECTING
                BluetoothProfile.STATE_DISCONNECTED -> currentState = State.REGISTERED
            }
        }
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

    private fun BluetoothDevice?.nameAddress() =
        if (this == null || checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
            null
        } else {
            "$name($address)"
        }

    override fun onServiceConnected(profile: Int, bp: BluetoothProfile) {
        Log.v(TAG, "onServiceConnected($profile, $bp)")
        if (bp is BluetoothHidDevice) {
            device = bp
            currentState = State.STOPPED
        }
    }

    override fun onServiceDisconnected(profile: Int) {
        Log.v(TAG, "onServiceDisconnected($profile)")
        device = null
        currentState = State.INIT
    }
}