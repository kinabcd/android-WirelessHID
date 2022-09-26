package tw.lospot.kin.wirelesshid.bluetooth

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
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
    private val onStateChanged: () -> Unit,
) : BluetoothHidDevice.Callback(), BluetoothProfile.ServiceListener {
    companion object {
        private const val TAG = "HidCallback"
    }

    var isRunning = false
        set(value) {
            val new = value && checkSelfPermission()
            if (field != new) {
                field = new
                updateTargetState()
                onStateChanged()
            }
        }
    private var device: BluetoothHidDevice? = null
    private var targetState by Delegates.observable(State.INITIALIZED) { _, old, new ->
        if (old != new) {
            Log.v(TAG, "targetState $new")
            scheduleNextState()
            onStateChanged()
        }
    }
    var targetDevice: BluetoothDevice? by Delegates.observable(null) { _, _, _ ->
        updateTargetState()
        scheduleNextState()
        onStateChanged()
    }
        private set
    var currentState by Delegates.observable(State.INITIALIZED) { _, old, new ->
        if (old != new) {
            Log.v(TAG, "currentState $new")
            scheduleNextState()
            onStateChanged()
        }
    }
        private set
    var currentDevice: BluetoothDevice? by Delegates.observable(null) { _, _, _ ->
        onStateChanged()
    }
        private set
    private val keyboardReport = KeyboardReport()
    private val consumerReport = ConsumerReport()
    private val mouseReport = ScrollableTrackpadMouseReport()
    private val downKey = HashSet<Byte>()
    private val disconnectingTimeoutRunnable = Runnable {
        currentState = State.DISCONNECT_TIMEOUT
    }

    private val retryTimeoutRunnable = Runnable {
        currentState = State.REGISTERED
    }
    private val nextStateRunnable = Runnable {
        nextState()
    }

    fun selectDevice(address: String?) {
        Log.v(TAG, "selectDevice($address)")
        if (!checkSelfPermission()) {
            Log.w(TAG, "selectDevice(), Permission denied")
            return
        }
        targetDevice = btAdapter.bondedDevices.firstOrNull { it.address == address }
    }

    private fun updateTargetState() {
        targetState = when {
            !isRunning -> State.INITIALIZED
            targetDevice != null -> State.CONNECTED
            else -> State.REGISTERED
        }
    }

    fun sendKey(keyEvent: Int, down: Boolean) {
        if (currentState != State.CONNECTED) return
        if (!checkSelfPermission()) {
            Log.w(TAG, "sendKey(), Permission denied")
            return
        }
        if (sendConsumerKey(keyEvent, down)) return
        if (sendKeyboardKey(keyEvent, down)) return
    }

    fun sendMouseKey(keyEventCode: Int, down: Boolean) {
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

        targetDevice?.let {
            device!!.sendReport(it, ScrollableTrackpadMouseReport.ID, mouseReport.bytes)
        }
    }

    fun sendMouseMove(dx: Int, dy: Int) {
        if (currentState != State.CONNECTED) return
        if (!checkSelfPermission()) {
            Log.w(TAG, "moveMouse(), Permission denied")
            return
        }
        mouseReport.setMove(dx, dy)
        currentDevice?.let {
            device!!.sendReport(currentDevice, ScrollableTrackpadMouseReport.ID, mouseReport.bytes)
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
            State.INITIALIZED -> if (targetState != State.INITIALIZED) startProxy()
            State.PROXYING -> {}
            State.STOPPED -> if (targetState != State.INITIALIZED) registerApp() else closeProxy()
            State.REGISTERING -> {}
            State.REGISTERED -> when (targetState) {
                State.INITIALIZED -> unregisterApp()
                State.CONNECTED -> connect()
                else -> {}
            }
            State.REGISTER_FAIL -> {
                handler.postDelayed({ currentState = State.STOPPED }, 5000)
            }
            State.CONNECTING -> {}
            State.DISCONNECTING -> {
                handler.postDelayed(disconnectingTimeoutRunnable, 5000)
            }
            State.CONNECTED -> when (targetState) {
                State.INITIALIZED -> unregisterApp()
                State.REGISTERED -> disconnect()
                State.CONNECTED -> if (targetDevice != currentDevice) disconnect()
                else -> {}
            }
            State.STOPPING -> {}
            State.CONNECT_FAIL -> when (targetState) {
                State.INITIALIZED -> {
                    handler.removeCallbacks(retryTimeoutRunnable)
                    unregisterApp()
                }
                State.CONNECTED -> {
                    Log.w(TAG, "Retry after 5s")
                    handler.postDelayed(retryTimeoutRunnable, 5000)
                }
                else -> {}
            }
            State.DISCONNECT_TIMEOUT -> {
                Log.w(TAG, "Disconnecting timeout")
                unregisterApp()
            }
        }
    }

    private fun startProxy() {
        if (currentState != State.INITIALIZED) return
        Log.v(TAG, "startProxy()")
        if (!checkSelfPermission()) {
            Log.w(TAG, "startProxy(), Permission denied")
            return
        }
        if (btAdapter.getProfileProxy(context, this, BluetoothProfile.HID_DEVICE)) {
            currentState = State.PROXYING
        }
    }

    private fun closeProxy() {
        if (currentState in arrayOf(State.INITIALIZED, State.PROXYING)) return
        Log.v(TAG, "closeProxy()")
        if (!checkSelfPermission()) {
            Log.w(TAG, "closeProxy(), Permission denied")
            return
        }
        btAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, device)
        device = null
        currentState = State.INITIALIZED
    }

    private fun registerApp() {
        if (currentState != State.STOPPED) return
        Log.v(TAG, "registerApp()")
        if (!checkSelfPermission()) {
            Log.w(TAG, "registerApp(), Permission denied")
            return
        }
        if (device!!.registerApp(sdpRecord, null, qosOut, context.mainExecutor, this)) {
            currentState = State.REGISTERING
        } else {
            Log.w(TAG, "registerApp(), Failed")
            currentState = State.REGISTER_FAIL
            isRunning = false
        }

    }

    private fun unregisterApp() {
        if (currentState in arrayOf(State.STOPPED, State.STOPPING)) return
        Log.v(TAG, "unregisterApp()")
        if (!checkSelfPermission()) {
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
        if (!checkSelfPermission()) {
            Log.w(TAG, "connect(), Permission denied")
            return
        }
        currentDevice = targetDevice
        currentState = State.CONNECTING
        device!!.connect(targetDevice)
    }

    private fun disconnect() {
        Log.v(TAG, "disconnect(), current=$currentDevice target=$targetDevice")
        if (!checkSelfPermission()) {
            Log.w(TAG, "disconnect(), Permission denied")
            return
        }
        currentState = State.DISCONNECTING
        device!!.disconnect(currentDevice)
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
        targetDevice?.let {
            device!!.sendReport(it, KeyboardReport.ID, keyboardReport.bytes)
        }
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
                BluetoothProfile.STATE_DISCONNECTED -> currentState = when (currentState) {
                    State.CONNECT_FAIL -> State.CONNECT_FAIL
                    State.CONNECTING -> State.CONNECT_FAIL
                    else -> State.REGISTERED
                }
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

    private fun BluetoothDevice?.nameAddress() = when {
        this == null -> null
        !checkSelfPermission() -> address
        else -> "$name($address)"
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
        currentState = State.INITIALIZED
    }

    private fun checkSelfPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        checkSelfPermission(context, BLUETOOTH_CONNECT) == PERMISSION_GRANTED else true
}