package tw.lospot.kin.wirelesshid.util

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.S
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED

object Permission {
    fun checkBluetoothConnectPermission(context: Context) = if (SDK_INT >= S) {
        PermissionChecker.checkSelfPermission(context, BLUETOOTH_CONNECT) == PERMISSION_GRANTED
    } else true
}