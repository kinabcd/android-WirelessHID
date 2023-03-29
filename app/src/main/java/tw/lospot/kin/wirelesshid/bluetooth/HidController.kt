package tw.lospot.kin.wirelesshid.bluetooth

import android.bluetooth.BluetoothDevice

interface HidController {
    var isRunning: Boolean
    val targetDevice: BluetoothDevice?
    val currentState:State
    val currentDevice: BluetoothDevice?
    fun selectDevice(address: String?)
    fun sendKey(keyEvent: Int, down: Boolean)
    fun sendMouseKey(keyEventCode: Int, down: Boolean)
    fun sendMouseMove(dx: Int, dy: Int)
    fun sendMouseScroll(dx: Int, dy: Int)
}