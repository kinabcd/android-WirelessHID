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
import kotlinx.coroutines.*
import tw.lospot.kin.wirelesshid.bluetooth.HidDeviceAdapter
import tw.lospot.kin.wirelesshid.bluetooth.HidController
import tw.lospot.kin.wirelesshid.bluetooth.HidOverGattAdapter
import tw.lospot.kin.wirelesshid.bluetooth.State
import kotlin.properties.Delegates

class BtService : Service(), Handler.Callback {
    private val context = this
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper(), this)
    private val messenger = Messenger(handler)
    private val listeners = HashSet<Messenger>()
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private val btManager by lazy { getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val btAdapter by lazy { btManager.adapter }
    private val btSettings by lazy { BtSettings(context) }
    private val hidController: HidController by lazy {
        HidDeviceAdapter(context, btAdapter) {
            updateServiceState()
            notifyStatus()
        }
    }

    private var isForeground by Delegates.observable(false) { _, old, new ->
        if (old != new) {
            if (new) {
                startForegroundService(Intent().apply {
                    setClass(applicationContext, BtService::class.java)
                })
                setNotification(
                    getString(R.string.app_name), "Running"
                )
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            btSettings.selectedAddress.collect {
                hidController.selectDevice(it)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return messenger.binder
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            ACTION_POWER -> hidController.isRunning = !hidController.isRunning
            ACTION_STATUS -> status(msg.replyTo, msg.arg1 == 1)
            ACTION_KEY -> hidController.sendKey(msg.arg1, msg.arg2 == 1)
            ACTION_SELECT_DEVICE -> {
                val address = msg.data.getString("address", "") ?: ""
                scope.launch { btSettings.setSelectedAddress(address) }
            }
            ACTION_MOUSE_MOVE -> hidController.sendMouseMove(msg.arg1, msg.arg2)
            ACTION_MOUSE_KEY -> hidController.sendMouseKey(msg.arg1, msg.arg2 == 1)
            ACTION_MOUSE_SCROLL -> hidController.sendMouseScroll(msg.arg1, msg.arg2)
        }
        return true
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
            putBoolean("isRunning", hidController.isRunning)
            putString("state", hidController.currentState.name)
            putString("current", hidController.currentDevice?.address)
            putString("selected", hidController.targetDevice?.address)
        }
        replyTo.send(Message.obtain().apply {
            what = ACTION_STATUS
            data = outData
        })
    }

    private fun updateServiceState() {
        isForeground = hidController.currentState != State.INITIALIZED
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
        const val ACTION_POWER = 1
        const val ACTION_STATUS = 2
        const val ACTION_SELECT_DEVICE = 3
        const val ACTION_KEY = 4
        const val ACTION_MOUSE_MOVE = 5
        const val ACTION_MOUSE_KEY = 6
        const val ACTION_MOUSE_SCROLL = 7
    }

}