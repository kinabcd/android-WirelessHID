package tw.lospot.kin.wirelesshid

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.*
import android.util.Log
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
import androidx.core.app.NotificationManagerCompat
import tw.lospot.kin.wirelesshid.bluetooth.HidCallback
import kotlin.properties.Delegates

class BtService : Service(), Handler.Callback {
    private val context = this
    private val handler = Handler(Looper.getMainLooper(), this)
    private val messenger = Messenger(handler)
    private val notificationManager by lazy { NotificationManagerCompat.from(this) }
    private val btManager by lazy { getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val btAdapter by lazy { btManager.adapter }
    private var hidCallback: HidCallback? by Delegates.observable(null) { _, old, new ->
        if (old == new) return@observable
        old?.unregisterApp()
        new?.registerApp()
    }
    private val connectedProfiles = HashSet<BluetoothProfile>()
    private var running = false


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return messenger.binder
    }

    override fun handleMessage(msg: Message): Boolean {
        when (msg.what) {
            ACTION_START -> start(msg.arg1, msg.replyTo)
            ACTION_STOP -> stop(msg.replyTo)
            ACTION_STATUS -> status(msg.replyTo)
            ACTION_KEY_UP -> sendKey(msg.arg1, false)
            ACTION_KEY_DOWN -> sendKey(msg.arg1, true)
        }
        return true
    }

    private fun start(flags: Int, replyTo: Messenger) {
        startForegroundService(Intent().apply {
            setClass(applicationContext, BtService::class.java)
        })
        running = true
        btAdapter.getProfileProxy(this, profileListener, BluetoothProfile.HID_DEVICE)
        setNotification(
            getString(R.string.app_name), "Running"
        )
        status(replyTo)
    }

    private fun stop(replyTo: Messenger) {
        running = false
        hidCallback = null
        connectedProfiles.forEach {
            btAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, it)
        }
        status(replyTo)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun status(replyTo: Messenger) {
        replyTo.send(Message.obtain().apply {
            what = ACTION_STATUS
            arg1 = if (running) 1 else 0
        })
    }

    private fun sendKey(keyEventCode: Int, down: Boolean) {
        hidCallback?.sendKey(keyEventCode, down)
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

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, bp: BluetoothProfile) {
            Log.v(TAG, "onServiceConnected($profile, $bp)")
            if (checkSelfPermission(context, BLUETOOTH_CONNECT) != PERMISSION_GRANTED) {
                return
            }
            if (bp is BluetoothHidDevice) {
                connectedProfiles.add(bp)
                hidCallback = HidCallback(context, bp)
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            Log.v(TAG, "onServiceDisconnected($profile)")
            connectedProfiles.clear()
            hidCallback = null
        }
    }

    companion object {
        private const val TAG = "BtService"
        const val ACTION_START = 0
        const val ACTION_STOP = 1
        const val ACTION_STATUS = 2
        const val ACTION_KEY_UP = 3
        const val ACTION_KEY_DOWN = 4
    }

}