package tw.lospot.kin.wirelesshid

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.app.NotificationManagerCompat
import tw.lospot.kin.wirelesshid.bluetooth.HidCallback
import kotlin.properties.Delegates

class BtService : Service(), Handler.Callback {
    private val context = this
    private val handler = Handler(Looper.getMainLooper(), this)
    private val messenger = Messenger(handler)
    private val listeners = HashSet<Messenger>()
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private val btManager by lazy { getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val btAdapter by lazy { btManager.adapter }
    private var selectedAddress: String? = null
    private var hidCallback: HidCallback? by Delegates.observable(null) { _, old, new ->
        if (old == new) return@observable
        old?.selectDevice(null)
        new?.selectDevice(selectedAddress)
    }
    private var running = false
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return messenger.binder
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
            ACTION_STATUS -> status(msg.replyTo, msg.arg1 == 1)
            ACTION_KEY_UP -> sendKey(msg.arg1, false)
            ACTION_KEY_DOWN -> sendKey(msg.arg1, true)
            ACTION_SELECT_DEVICE -> selectDevice(msg.data.getString("address", null))
            ACTION_MOVE_MOUSE -> moveMouse(msg.arg1, msg.arg2)
            ACTION_MOUSE_KEY -> sendMouseKey(msg.arg1, msg.arg2 == 1)
        }
        return true
    }

    private fun start() {
        startForegroundService(Intent().apply {
            setClass(applicationContext, BtService::class.java)
        })
        running = true
        hidCallback = HidCallback(context, btAdapter)
        setNotification(
            getString(R.string.app_name), "Running"
        )
        notifyStatus()
    }

    private fun stop() {
        running = false
        hidCallback = null
        notifyStatus()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun status(replyTo: Messenger, on: Boolean) {
        if (on) {
            listeners.add(replyTo)
            sendStatus(replyTo)
        } else {
            listeners.remove(replyTo)
        }
    }

    private fun notifyStatus() {
        listeners.forEach {
            sendStatus(it)
        }
    }

    private fun sendStatus(replyTo: Messenger) {
        val outData = Bundle().apply {
            putString("selected", selectedAddress)
        }
        replyTo.send(Message.obtain().apply {
            what = ACTION_STATUS
            arg1 = if (running) 1 else 0
            data = outData
        })
    }

    private fun sendKey(keyEventCode: Int, down: Boolean) {
        hidCallback?.sendKey(keyEventCode, down)
    }

    private fun sendMouseKey(keyEventCode: Int, down: Boolean) {
        hidCallback?.sendMouseKey(keyEventCode, down)
    }
    private fun moveMouse(dx: Int, dy: Int) {
        hidCallback?.moveMouse(dx, dy)
    }

    private fun selectDevice(address: String?) {
        selectedAddress = address
        hidCallback?.selectDevice(address)
        notifyStatus()
    }

    private fun createChannel() {
        val notificationChannel =
            NotificationChannel("Main", "Main", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = ""
                lightColor = 0
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
        notificationManager.createNotificationChannel(notificationChannel)
    }

    private fun setNotification(title: String, content: String) {
        val intent = Intent(applicationContext, BtActivity::class.java)
        intent.action = "android.intent.action.MAIN"
        intent.addCategory("android.intent.category.LAUNCHER")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity =
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        createChannel()
        val builder = NotificationCompat.Builder(this, "Main")
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_keyboard)
            .setContentIntent(activity)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSound(null)
            .setVibrate(null)
            .setForegroundServiceBehavior(FOREGROUND_SERVICE_IMMEDIATE)
        startForeground(1000, builder.build())
    }

    companion object {
        private const val TAG = "BtService"
        const val ACTION_START = 0
        const val ACTION_STOP = 1
        const val ACTION_STATUS = 2
        const val ACTION_KEY_UP = 3
        const val ACTION_KEY_DOWN = 4
        const val ACTION_SELECT_DEVICE = 5
        const val ACTION_MOVE_MOUSE = 6
        const val ACTION_MOUSE_KEY = 7
    }

}