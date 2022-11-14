package tw.lospot.kin.wirelesshid.ui

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Handler
import android.os.HandlerThread
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import tw.lospot.kin.wirelesshid.BtConnection
import tw.lospot.kin.wirelesshid.bluetooth.State

class UiModel : ViewModel(), SharedPreferences.OnSharedPreferenceChangeListener {
    private val backgroundThread by lazy { HandlerThread("UiBackground").apply { start() } }
    private val handler by lazy { Handler(backgroundThread.looper) }
    var selectedDevice: String? by mutableStateOf(null)
    var currentDevice: String? by mutableStateOf(null)
    var state: State by mutableStateOf(State.INITIALIZED)
    var isConnected by mutableStateOf(false)
    var isRunning by mutableStateOf(false)
    var orientation by mutableStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
    var isMainPanel by mutableStateOf(false)

    var preferences: SharedPreferences? = null
    var connection: BtConnection? = null

    var requestedOrientation
        get() = orientation
        set(value) {
            preferences?.edit {
                putInt("requestedOrientation", value)
            }
        }

    fun init(outCtx: Context) {
        val context = outCtx.applicationContext
        if (connection == null) {
            connection = BtConnection(context, handler, ::updateConnectionState).also {
                handler.post { it.bind() }
            }
        }
        if (preferences == null) {
            preferences = PreferenceManager.getDefaultSharedPreferences(context).also {
                it.registerOnSharedPreferenceChangeListener(this)
                onSharedPreferenceChanged(it, "")
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            "" -> {
                onSharedPreferenceChanged(sharedPreferences, "requestedOrientation")
            }
            "requestedOrientation" -> orientation =
                sharedPreferences.getInt(key, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
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
        preferences?.unregisterOnSharedPreferenceChangeListener(this)
        handler.post {
            connection?.unbind()
            backgroundThread.quitSafely()
        }
    }
}