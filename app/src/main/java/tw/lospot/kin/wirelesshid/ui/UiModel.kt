package tw.lospot.kin.wirelesshid.ui

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.HandlerThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import tw.lospot.kin.wirelesshid.BtConnection
import tw.lospot.kin.wirelesshid.BtSettings
import tw.lospot.kin.wirelesshid.bluetooth.State

class UiModel : ViewModel() {
    private val backgroundThread by lazy { HandlerThread("UiBackground").apply { start() } }
    private val handler by lazy { Handler(backgroundThread.looper) }
    var selectedDevice: String? by mutableStateOf(null)
    var currentDevice: String? by mutableStateOf(null)
    var state: State by mutableStateOf(State.INITIALIZED)
    var isConnected by mutableStateOf(false)
    var isRunning by mutableStateOf(false)
    var orientation by mutableStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    var isMainPanel by mutableStateOf(false)

    var connection: BtConnection? = null

    fun init(outCtx: Context) {
        val context = outCtx.applicationContext
        if (connection == null) {
            connection = BtConnection(context, handler, ::updateConnectionState).also {
                handler.post { it.bind() }
            }
        }
        viewModelScope.launch {
            BtSettings(context).requestedOrientation.collect {
                orientation = it
            }
        }
    }

    private fun updateConnectionState() {
        connection?.let { connection ->
            isConnected = connection.isConnected
            isRunning = connection.isRunning
            selectedDevice = connection.selectedDevice
            currentDevice = connection.currentDevice
            state = connection.state
        }
    }

    override fun onCleared() {
        handler.post {
            connection?.unbind()
            backgroundThread.quitSafely()
        }
    }
}