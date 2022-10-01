package tw.lospot.kin.wirelesshid

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import tw.lospot.kin.wirelesshid.bluetooth.State
import java.util.concurrent.Executor
import kotlin.properties.Delegates

class BtConnection(
    private val context: Context,
    private val handler: Handler,
    private val callback: () -> Unit
) : ServiceConnection, Handler.Callback {
    companion object {
        private const val TAG = "BtConnection"
        private val DEBUG = Log.isLoggable(TAG, Log.VERBOSE)
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
        private set
    var selectedDevice: String? = null
        private set
    var currentDevice: String? = null
        private set
    var state: State = State.INITIALIZED
        private set

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

    fun sendKey(keycode: Int, down: Boolean) =
        sendMessage(BtService.ACTION_KEY, keycode, if (down) 1 else 0)

    fun sendMouseKey(keycode: Int, down: Boolean) =
        sendMessage(BtService.ACTION_MOUSE_KEY, keycode, if (down) 1 else 0)

    fun moveMouse(dx: Int, dy: Int) = sendMessage(BtService.ACTION_MOUSE_MOVE, dx, dy)
    fun switchPower() = sendMessage(BtService.ACTION_POWER)
    fun selectDevice(address: String?) =
        sendMessage(BtService.ACTION_SELECT_DEVICE, inData = Bundle().apply {
            putString("address", address)
        })

    private fun sendMessage(action: Int, inArg1: Int = 0, inArg2: Int = 0, inData: Bundle? = null) {
        if (!handler.looper.isCurrentThread) {
            handler.post { sendMessage(action, inArg1, inArg2, inData) }
            return
        }
        if (DEBUG) Log.v(TAG, "sendMessage: $action, $inArg1, $inArg2")
        try {
            messenger?.send(Message.obtain().apply {
                what = action
                arg1 = inArg1
                arg2 = inArg2
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
        if (DEBUG) Log.v(TAG, "handleMessage: $what, $arg1")
        executor.execute {
            when (what) {
                BtService.ACTION_STATUS -> {
                    isRunning = data.getBoolean("isRunning")
                    selectedDevice = data.getString("selected")
                    currentDevice = data.getString("current")
                    state = State.valueOf(data.getString("state") ?: State.INITIALIZED.name)
                    if (isConnected) callback()
                }
            }
        }
        return true
    }

    override fun onServiceDisconnected(name: ComponentName?) = executor.execute {
        Log.v(TAG, "onServiceDisconnected")
        messenger = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) = executor.execute {
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