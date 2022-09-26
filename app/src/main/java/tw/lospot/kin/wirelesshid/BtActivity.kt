package tw.lospot.kin.wirelesshid

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowInsetsControllerCompat
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

    private val requestPermissionLauncher =
        registerForActivityResult(RequestMultiplePermissions()) {
            showPermissionHint = !checkPermission()
        }

    private val connection by lazy {
        BtConnection(this, backgroundHandler, ::updateConnectionState)
    }

    private var isRunning by mutableStateOf(false)
    private var isConnected by mutableStateOf(false)
    private var selectedDevice: String? by mutableStateOf(null)
    private var currentDevice: String? by mutableStateOf(null)
    private var state: State by mutableStateOf(State.INITIALIZED)
    private var showDevices by mutableStateOf(false)
    private var showPermissionHint by mutableStateOf(false)
    private val devices = mutableStateListOf<Pair<String, String>>()

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContent {
            ToolkitTheme {
                val colors by rememberUpdatedState(MaterialTheme.colors)
                val windowInsetsController =
                    remember { WindowInsetsControllerCompat(window, window.peekDecorView()) }
                SideEffect {
                    window.statusBarColor = colors.primarySurface.toArgb()
                    window.navigationBarColor = colors.background.toArgb()
                    windowInsetsController.apply {
                        isAppearanceLightStatusBars = colors.isLight
                        isAppearanceLightNavigationBars = colors.isLight
                    }
                }
                Surface(
                    color = MaterialTheme.colors.background,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    Panel(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val hasPermission = checkPermission()
        showPermissionHint = !hasPermission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasPermission && !shouldShowRequestPermissionRationale(BLUETOOTH_CONNECT)
        ) {
            maybeRequestPermission()
        }

        backgroundHandler.post {
            connection.bind()
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundHandler.post {
            connection.unbind()
        }
    }

    private fun sendKey(keycode: Int, down: Boolean) = backgroundHandler.post {
        connection.sendMessage(BtService.ACTION_KEY, keycode, if (down) 1 else 0)
    }

    private fun sendMouseKey(keycode: Int, down: Boolean) = backgroundHandler.post {
        connection.sendMessage(BtService.ACTION_MOUSE_KEY, keycode, if (down) 1 else 0)
    }

    private fun moveMouse(dx: Int, dy: Int) = backgroundHandler.post {
        connection.sendMessage(BtService.ACTION_MOUSE_MOVE, dx, dy)
    }

    private fun clickPower() = backgroundHandler.post {
        connection.sendMessage(BtService.ACTION_POWER)
    }

    private fun clickDevice(address: String?) = backgroundHandler.post {
        connection.sendMessage(BtService.ACTION_SELECT_DEVICE, inData = Bundle().apply {
            putString("address", address)
        })
    }

    private fun maybeRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !checkPermission())
            requestPermissionLauncher.launch(arrayOf(BLUETOOTH_CONNECT))
    }

    private fun updateDevices() {
        if (!checkPermission()) {
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
        val stateChanged = state != connection.state
        isConnected = connection.isConnected
        isRunning = connection.isRunning
        selectedDevice = connection.selectedDevice
        currentDevice = connection.currentDevice
        state = connection.state

        if (stateChanged) {
            when (state) {
                State.REGISTERED -> if (selectedDevice == null) showDevices = true
                State.CONNECTED -> showDevices = false
                else -> {}
            }
        }
    }

    private fun checkPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ActivityCompat.checkSelfPermission(this, BLUETOOTH_CONNECT) == PERMISSION_GRANTED
    } else true

    @Composable
    private fun Panel(
        modifier: Modifier = Modifier
    ) {
        Column(modifier = modifier) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (showPermissionHint) {
                    PermissionHint()
                } else if (showDevices) {
                    LaunchedEffect(true) {
                        updateDevices()
                    }
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
                                    .padding(8.dp),
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
                                    .padding(horizontal = 8.dp),
                                onKey = { keycode, down -> sendMouseKey(keycode, down) },
                                onMove = { dx, dy -> moveMouse(dx, dy) }
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(52.dp)) {
                Spacer(modifier = Modifier.width(32.dp))
                Text(text = state.name, modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(16.dp))
                BottomButton(
                    painter = painterResource(id = R.drawable.ic_power),
                    enabled = isConnected,
                    selected = isRunning,
                    onClick = { clickPower() },
                )
                Spacer(modifier = Modifier.width(8.dp))
                BottomButton(
                    painter = painterResource(id = R.drawable.ic_devices),
                    selected = showDevices,
                    onClick = { showDevices = !showDevices && checkPermission() },
                )
                Spacer(modifier = Modifier.width(32.dp))
            }
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

    @Composable
    private fun BottomButton(
        onClick: () -> Unit,
        painter: Painter,
        contentDescription: String? = null,
        enabled: Boolean = true,
        selected: Boolean = false,
    ) {
        val backgroundColor =
            if (selected) MaterialTheme.colors.onSurface else MaterialTheme.colors.surface
        val contentColor =
            if (selected) MaterialTheme.colors.surface else MaterialTheme.colors.onSurface
        val colors = ButtonDefaults.buttonColors(
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            disabledBackgroundColor = backgroundColor,
            disabledContentColor = contentColor.copy(alpha = ContentAlpha.disabled)
        )

        OutlinedButton(
            modifier = Modifier.size(36.dp),
            contentPadding = PaddingValues(4.dp),
            enabled = enabled,
            colors = colors,
            onClick = onClick,
        ) {
            Icon(
                painter = painter,
                contentDescription = contentDescription,
            )
        }
    }

    @Composable
    fun PermissionHint() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Text(text = "Permission denied", fontWeight = FontWeight.Bold, color = Color.Red)
            Spacer(Modifier.height(8.dp))
            Text(text = "Nearby devices", fontWeight = FontWeight.Bold)
            Text(text = "Connect to other devices.", Modifier.padding(start = 16.dp))
            Spacer(Modifier.height(16.dp))
            Button(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                onClick = { maybeRequestPermission() }) {
                Text(text = "Grant permissions")
            }
        }
    }
}