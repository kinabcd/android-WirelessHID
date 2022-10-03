package tw.lospot.kin.wirelesshid

import android.Manifest.permission.BLUETOOTH_CONNECT
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat.Type.navigationBars
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.navigation.compose.rememberNavController
import tw.lospot.kin.wirelesshid.bluetooth.report.KeyboardReport
import tw.lospot.kin.wirelesshid.ui.SetupNavGraph
import tw.lospot.kin.wirelesshid.ui.UiModel
import tw.lospot.kin.wirelesshid.ui.theme.ToolkitTheme
import tw.lospot.kin.wirelesshid.util.Permission

class BtActivity : ComponentActivity() {
    private val model: UiModel by viewModels()
    private var showPermissionHint by mutableStateOf(false)
    private val requestPermissionLauncher =
        registerForActivityResult(RequestMultiplePermissions()) {
            showPermissionHint = !checkPermission()
        }

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        model.init(this)
        setContent {
            val navController = rememberNavController()
            ToolkitTheme {
                val colors = MaterialTheme.colors
                val windowInsetsController =
                    remember { WindowInsetsControllerCompat(window, window.peekDecorView()) }
                LaunchedEffect(colors) {
                    window.statusBarColor = colors.primarySurface.toArgb()
                    window.navigationBarColor = colors.background.toArgb()
                    windowInsetsController.apply {
                        isAppearanceLightStatusBars = colors.isLight
                        isAppearanceLightNavigationBars = colors.isLight
                    }
                }
                LaunchedEffect(true) {
                    windowInsetsController.apply {
                        systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        hide(navigationBars())
                    }
                }
                LaunchedEffect(model.orientation) {
                    requestedOrientation = model.orientation
                }
                Surface(
                    color = MaterialTheme.colors.background,
                    modifier = Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    if (showPermissionHint) {
                        PermissionHint()
                    } else if (model.isConnected) {
                        SetupNavGraph(navController, model)
                    }
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
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (val overrideKey = keyOverride(keyCode)) {
            in KeyboardReport.KeyEventMap.keys -> model.connection?.sendKey(overrideKey, true)
            else -> return super.onKeyDown(overrideKey, event)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (val overrideKey = keyOverride(keyCode)) {
            in KeyboardReport.KeyEventMap.keys -> model.connection?.sendKey(overrideKey, false)
            else -> return super.onKeyUp(overrideKey, event)
        }
        return true
    }

    private fun keyOverride(keyCode: Int) = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> KeyEvent.KEYCODE_PAGE_UP
        KeyEvent.KEYCODE_VOLUME_DOWN -> KeyEvent.KEYCODE_PAGE_DOWN
        else -> keyCode
    }

    private fun maybeRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !checkPermission())
            requestPermissionLauncher.launch(arrayOf(BLUETOOTH_CONNECT))
    }


    private fun checkPermission() = Permission.checkBluetoothConnectPermission(this)

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