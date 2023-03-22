package tw.lospot.kin.wirelesshid.ui.screens

import android.bluetooth.BluetoothManager
import android.content.Context.BLUETOOTH_SERVICE
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import tw.lospot.kin.wirelesshid.R
import tw.lospot.kin.wirelesshid.ui.PageButton
import tw.lospot.kin.wirelesshid.ui.SETTINGS
import tw.lospot.kin.wirelesshid.ui.UiModel
import tw.lospot.kin.wirelesshid.ui.keyboard.QwertKeyboard
import tw.lospot.kin.wirelesshid.ui.mouse.TouchPad
import tw.lospot.kin.wirelesshid.util.Permission

@Composable
fun MainPanelScreen(
    navController: NavController,
    model: UiModel = viewModel(),
) {
    DisposableEffect(model) {
        model.isMainPanel = true
        onDispose {
            model.isMainPanel = false
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .systemGestureExclusion()
        ) {
            val orientation = LocalConfiguration.current.orientation
            if (orientation != Configuration.ORIENTATION_LANDSCAPE) {
                Column(Modifier.fillMaxSize()) {
                    TouchPad(
                        Modifier
                            .fillMaxWidth()
                            .weight(0.65f),
                        onKey = { keycode, down -> model.connection?.sendMouseKey(keycode, down) },
                        onMove = { dx, dy -> model.connection?.moveMouse(dx, dy) },
                        onScroll = { dx, dy -> model.connection?.scroll(dx, dy) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    QwertKeyboard(
                        Modifier
                            .fillMaxWidth()
                            .weight(0.35f)
                    ) { keycode, down -> model.connection?.sendKey(keycode, down) }
                }
            } else {
                Row(Modifier.fillMaxSize()) {
                    QwertKeyboard(
                        Modifier
                            .fillMaxHeight()
                            .weight(0.65f)
                    ) { keycode, down -> model.connection?.sendKey(keycode, down) }
                    Spacer(modifier = Modifier.width(8.dp))
                    TouchPad(
                        Modifier
                            .fillMaxHeight()
                            .weight(0.35f),
                        onKey = { keycode, down -> model.connection?.sendMouseKey(keycode, down) },
                        onMove = { dx, dy -> model.connection?.moveMouse(dx, dy) },
                        onScroll = { dx, dy -> model.connection?.scroll(dx, dy) },
                    )
                }
            }
        }
        val context = LocalContext.current
        var selectedDeviceName by remember { mutableStateOf("None") }
        LaunchedEffect(model.selectedDevice) {
            launch(Dispatchers.Default) {
                val bluetoothManager =
                    (context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager)
                selectedDeviceName = model.selectedDevice?.let {
                    if (Permission.checkBluetoothConnectPermission(context)) {
                        bluetoothManager.adapter.getRemoteDevice(it)?.name
                    } else it
                } ?: "None"
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(52.dp)) {
            Spacer(modifier = Modifier.width(32.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = selectedDeviceName, modifier = Modifier.fillMaxWidth())
                Text(text = model.state.name, modifier = Modifier.fillMaxWidth(), fontSize = 7.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            PageButton(
                painter = painterResource(id = R.drawable.ic_power),
                selected = model.isRunning,
                onClick = { model.connection?.switchPower() },
            )
            Spacer(modifier = Modifier.width(8.dp))
            PageButton(
                painter = painterResource(id = R.drawable.ic_settings),
                onClick = { navController.navigate(SETTINGS) },
            )
            Spacer(modifier = Modifier.width(32.dp))
        }
    }
}

