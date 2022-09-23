package tw.lospot.kin.wirelesshid

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import tw.lospot.kin.wirelesshid.ui.keyboard.QwertKeyboard
import tw.lospot.kin.wirelesshid.ui.theme.ToolkitTheme
import java.util.concurrent.Executor
import kotlin.properties.Delegates

class BtActivity : ComponentActivity() {
    companion object {
        private const val TAG = "ActivityMain"
        private val backgroundThread by lazy { HandlerThread("BtActivity").apply { start() } }
        private val backgroundHandler by lazy { Handler(backgroundThread.looper) }
    }

    private val connection by lazy {
        BtConnection(this, backgroundHandler, ::updateConnectionState)
    }

    private var isRunning by mutableStateOf(false)
    private var isConnected by mutableStateOf(false)
    private var selected: String? by mutableStateOf(null)
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
        WindowInsetsControllerCompat(window, window.peekDecorView()).apply {
            systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(navigationBars())
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

            connection.sendMessage(
                action = BtService.ACTION_START, arg = 1
            )
        }
    }

    private fun clickStop() {
        backgroundHandler.post {
            connection.sendMessage(BtService.ACTION_STOP)
        }
    }

    private fun sendKey(keycode: Int, down: Boolean) {
        backgroundHandler.post {
            if (down) {
                connection.sendMessage(BtService.ACTION_KEY_DOWN, keycode)
            } else {
                connection.sendMessage(BtService.ACTION_KEY_UP, keycode)
            }
        }
    }

    private fun selectDevice(address: String) {
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
        Log.v(TAG, "updateConnectionState: ${connection.isConnected}, ${connection.isRunning}")
        isConnected = connection.isConnected
        isRunning = connection.isRunning
        selected = connection.selected
    }

    @Composable
    private fun Panel(
        modifier: Modifier = Modifier
    ) {
        var showDevices by remember { mutableStateOf(false) }
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (showDevices) {
                LazyColumn {
                    items(devices, key = { it.first }) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 32.dp)
                                .fillMaxWidth()
                                .clickable { selectDevice(it.first) }
                                .height(48.dp)
                                .padding(4.dp)
                                .border(BorderStroke(1.dp, Color.Gray))
                                .padding(8.dp)
                        ) {
                            val checkStr = if (it.first == selected) "v " else ""
                            Text(text = "$checkStr${it.second}(${it.first})")
                        }
                    }
                }
            } else {
                QwertKeyboard { keycode, down -> sendKey(keycode, down) }
            }
            Row(horizontalArrangement = Arrangement.Center) {
                Button(
                    modifier = Modifier.size(48.dp),
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
                Button(
                    modifier = Modifier.size(48.dp),
                    onClick = { showDevices = !showDevices },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_devices),
                        contentDescription = null,
                    )
                }
            }
        }
    }

    private class BtConnection(
        private val context: Context,
        private val handler: Handler,
        private val callback: () -> Unit
    ) : ServiceConnection, Handler.Callback {
        companion object {
            private const val TAG = "BtConnection"
        }

        private val intent = Intent().apply { setClass(context, BtService::class.java) }
        private val executor = Executor { handler.post(it) }
        private var messenger by Delegates.observable<Messenger?>(null) { _, old, new ->
            if (old != new) callback()
        }
        private val reply = Messenger(Handler(handler.looper, this))
        var isBound = false
            private set(value) {
                if (field != value) {
                    field = value
                    callback()
                }
            }
        val isConnected get() = messenger != null

        var isRunning = false
            private set(value) {
                field = value
                if (isConnected) callback()
            }
        var selected: String? = null
            private set(value) {
                field = value
                if (isConnected) callback()
            }

        fun bind() {
            if (!handler.looper.isCurrentThread) throw RuntimeException("Wrong thread")
            if (!isBound) {
                isBound = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.bindService(intent, Context.BIND_AUTO_CREATE, executor, this)
                } else {
                    context.bindService(intent, this, Context.BIND_AUTO_CREATE)
                }
            } else {
                Log.w(TAG, "bind when bound")
            }
        }

        fun unbind() {
            if (!handler.looper.isCurrentThread) throw RuntimeException("Wrong thread")
            if (isBound) {
                context.unbindService(this)
            }
            sendMessage(BtService.ACTION_STATUS, 0)
            messenger = null
            isBound = false
        }

        fun sendMessage(action: Int, arg: Int = 0, inData: Bundle? = null) {
            if (!handler.looper.isCurrentThread) throw RuntimeException("Wrong thread")
            Log.v(TAG, "sendMessage: $action, $arg")
            try {
                messenger?.send(Message.obtain().apply {
                    what = action
                    arg1 = arg
                    replyTo = reply
                    if (inData != null) data = inData
                })
            } catch (e: RemoteException) {
                Log.w(TAG, "sendMessage: $e")
            }
        }

        override fun handleMessage(msg: Message): Boolean {
            val what = msg.what
            val arg1 = msg.arg1
            val data = msg.data
            Log.v(TAG, "handleMessage: $what, $arg1")
            executor.execute {
                when (what) {
                    BtService.ACTION_STATUS -> {
                        isRunning = arg1 == 1
                        selected = data.getString("selected")
                    }
                }
            }
            return true
        }

        override fun onServiceDisconnected(name: ComponentName?) = executor.execute {
            Log.v(TAG, "onServiceDisconnected")
            messenger = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) =
            executor.execute {
                Log.v(TAG, "onServiceConnected")
                messenger = Messenger(service)
                sendMessage(BtService.ACTION_STATUS, 1)
            }

        override fun onBindingDied(name: ComponentName?) = executor.execute {
            Log.w(TAG, "onBindingDied")
            unbind()
        }

        override fun onNullBinding(name: ComponentName?) = executor.execute {
            Log.w(TAG, "onNullBinding")
            unbind()
        }
    }
}