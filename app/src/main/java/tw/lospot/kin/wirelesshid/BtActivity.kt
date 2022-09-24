package tw.lospot.kin.wirelesshid

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.*
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import tw.lospot.kin.wirelesshid.bluetooth.State
import tw.lospot.kin.wirelesshid.ui.keyboard.QwertKeyboard
import tw.lospot.kin.wirelesshid.ui.mouse.TouchPad
import tw.lospot.kin.wirelesshid.ui.theme.ToolkitTheme

class BtActivity : ComponentActivity() {
    companion object {
        private const val TAG = "BtActivity"
        private val DEBUG = Log.isLoggable(TAG, Log.VERBOSE)
        private val backgroundThread by lazy { HandlerThread("BtActivity").apply { start() } }
        private val backgroundHandler by lazy { Handler(backgroundThread.looper) }
    }

    private val connection by lazy {
        BtConnection(this, backgroundHandler, ::updateConnectionState)
    }

    private var isRunning by mutableStateOf(false)
    private var isConnected by mutableStateOf(false)
    private var selectedDevice: String? by mutableStateOf(null)
    private var currentDevice: String? by mutableStateOf(null)
    private var state: State by mutableStateOf(State.INIT)
    private var showDevices by mutableStateOf(false)
    private val devices = mutableListOf<Pair<String, String>>()

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContent {
            ToolkitTheme {
                Surface(
                    color = MaterialTheme.colors.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        Panel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        backgroundHandler.post {
            connection.bind()
        }
        updateDevices()
    }

    override fun onStop() {
        super.onStop()
        backgroundHandler.post {
            connection.unbind()
        }
    }

    private fun clickStart() {
        backgroundHandler.post {
            connection.sendMessage(BtService.ACTION_START)
        }
    }

    private fun clickStop() {
        backgroundHandler.post {
            connection.sendMessage(BtService.ACTION_STOP)
        }
    }

    private fun sendKey(keycode: Int, down: Boolean) {
        backgroundHandler.post {
            connection.sendMessage(BtService.ACTION_KEY, keycode, if (down) 1 else 0)
        }
    }

    private fun sendMouseKey(keycode: Int, down: Boolean) {
        backgroundHandler.post {
            connection.sendMessage(BtService.ACTION_MOUSE_KEY, keycode, if (down) 1 else 0)
        }
    }

    private fun moveMouse(dx: Int, dy: Int) {
        backgroundHandler.post {
            connection.sendMessage(BtService.ACTION_MOUSE_MOVE, dx, dy)
        }
    }

    private fun clickDevice(address: String?) {
        backgroundHandler.post {
            connection.sendMessage(BtService.ACTION_SELECT_DEVICE, inData = Bundle().apply {
                putString("address", address)
            })
        }
    }

    private fun updateDevices() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val btManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val btAdapter = btManager.adapter
        val bonded = btAdapter.bondedDevices.map {
            it.address to it.name
        }
        devices.removeAll { it !in bonded }
        devices.addAll(bonded.filter { it !in devices })
    }

    private fun updateConnectionState() {
        if (DEBUG)
            Log.v(TAG, "updateConnectionState: ${connection.isConnected}, ${connection.isRunning}")
        isConnected = connection.isConnected
        isRunning = connection.isRunning
        selectedDevice = connection.selectedDevice
        currentDevice = connection.currentDevice
        state = connection.state
        if (selectedDevice == null) {
            showDevices = true
        }
    }

    @Composable
    private fun Panel(
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (showDevices) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), reverseLayout = true) {
                        items(devices, key = { it.first }) {
                            DeviceItem(
                                it.first, it.second,
                                it.first == selectedDevice,
                                it.first == currentDevice,
                            )
                        }
                        item {
                            DeviceItem(address = null, name = "None", selectedDevice == null)
                        }
                    }
                } else {
                    val orientation = LocalConfiguration.current.orientation
                    if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
                        Column(Modifier.fillMaxSize()) {
                            TouchPad(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(0.7f)
                                    .padding(bottom = 8.dp),
                                onKey = { keycode, down -> sendMouseKey(keycode, down) },
                                onMove = { dx, dy -> moveMouse(dx, dy) }
                            )
                            QwertKeyboard(
                                Modifier
                                    .fillMaxWidth()
                                    .weight(0.3f)
                            ) { keycode, down -> sendKey(keycode, down) }
                        }
                    } else {
                        Row(Modifier.fillMaxSize()) {
                            QwertKeyboard(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(0.7f)
                            ) { keycode, down -> sendKey(keycode, down) }
                            TouchPad(
                                Modifier
                                    .fillMaxHeight()
                                    .weight(0.3f)
                                    .padding(start = 8.dp),
                                onKey = { keycode, down -> sendMouseKey(keycode, down) },
                                onMove = { dx, dy -> moveMouse(dx, dy) }
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(36.dp)) {
                val colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.surface,
                    contentColor = MaterialTheme.colors.onSurface,
                    disabledBackgroundColor = MaterialTheme.colors.surface,
                    disabledContentColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
                )
                Spacer(modifier = Modifier.width(32.dp))
                Text(text = state.name, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    modifier = Modifier.size(36.dp),
                    contentPadding = PaddingValues(4.dp),
                    colors = colors,
                    enabled = isConnected,
                    onClick = { if (!isRunning) clickStart() else clickStop() },
                ) {
                    val painter = if (!isRunning) {
                        painterResource(id = R.drawable.ic_power)
                    } else {
                        painterResource(id = R.drawable.ic_power_off)
                    }
                    Icon(
                        painter = painter,
                        contentDescription = null,
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    modifier = Modifier.size(36.dp),
                    contentPadding = PaddingValues(4.dp),
                    colors = colors,
                    onClick = { showDevices = !showDevices },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_devices),
                        contentDescription = null,
                    )
                }
                Spacer(modifier = Modifier.width(32.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    @Composable
    private fun DeviceItem(
        address: String?,
        name: String?,
        checked: Boolean = false,
        checked2: Boolean = false,
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .height(48.dp)
                .padding(4.dp)
                .border(BorderStroke(1.dp, Color.Gray))
        ) {
            val addressStr = if (address.isNullOrBlank()) "" else "(${address})"
            Text(text = "${name}$addressStr", modifier = Modifier
                .let {
                    when {
                        checked -> it.background(Color(0x2200FF00))
                        checked2 -> it.background(Color(0x22FF0000))
                        else -> it
                    }
                }
                .clickable { clickDevice(address) }
                .padding(8.dp)
            )
        }
    }
}