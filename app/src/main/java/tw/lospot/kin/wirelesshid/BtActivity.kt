package tw.lospot.kin.wirelesshid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        setContent {
            ToolkitTheme {
                Surface(
                    color = MaterialTheme.colors.background,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
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

    private fun updateConnectionState() {
        Log.v(TAG, "updateConnectionState: ${connection.isConnected}, ${connection.isRunning}")
        isConnected = connection.isConnected
        isRunning = connection.isRunning
    }

    @Composable
    private fun Panel(
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row {
                if (!isRunning) {
                    Button(
                        enabled = isConnected && !isRunning,
                        onClick = ::clickStart
                    ) {
                        Text(stringResource(R.string.activity_main_start))
                    }
                } else {
                    Button(
                        enabled = isConnected && isRunning,
                        onClick = ::clickStop
                    ) {
                        Text(stringResource(R.string.activity_main_stop))
                    }
                }
            }
            QwertKeyboard { keycode, down ->
                if (down) {
                    backgroundHandler.post {
                        connection.sendMessage(BtService.ACTION_KEY_DOWN, keycode)
                    }
                } else {
                    backgroundHandler.post {
                        connection.sendMessage(BtService.ACTION_KEY_UP, keycode)
                    }

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
            messenger = null
            isBound = false
        }

        fun sendMessage(action: Int, arg: Int = 0) {
            if (!handler.looper.isCurrentThread) throw RuntimeException("Wrong thread")
            Log.v(TAG, "sendMessage: $action, $arg")
            try {
                messenger?.send(Message.obtain().apply {
                    what = action
                    arg1 = arg
                    replyTo = reply
                })
            } catch (e: RemoteException) {
                Log.w(TAG, "sendMessage: $e")
            }
        }

        override fun handleMessage(msg: Message): Boolean {
            val what = msg.what
            val arg1 = msg.arg1
            Log.v(TAG, "handleMessage: $what, $arg1")
            executor.execute {
                when (what) {
                    BtService.ACTION_STATUS -> isRunning = arg1 == 1
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
                sendMessage(BtService.ACTION_STATUS)
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