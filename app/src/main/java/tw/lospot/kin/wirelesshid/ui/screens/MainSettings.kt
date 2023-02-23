package tw.lospot.kin.wirelesshid.ui.screens

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import tw.lospot.kin.wirelesshid.BtSettings
import tw.lospot.kin.wirelesshid.BuildConfig
import tw.lospot.kin.wirelesshid.R
import tw.lospot.kin.wirelesshid.ui.*
import tw.lospot.kin.wirelesshid.util.Permission

@Composable
fun SettingsScreen(
    navController: NavController,
    model: UiModel = viewModel(),
) {
    PageContent(
        title = stringResource(id = R.string.title_settings),
        backAction = { navController.popBackStack() },
    ) {
        PageColumn {
            val context = LocalContext.current
            val bluetoothManager = remember(context) {
                context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            }
            val deviceName = remember(model.selectedDevice) { mutableStateOf("") }
            LaunchedEffect(key1 = model.selectedDevice) {
                deviceName.value = ""
                if (model.selectedDevice != null &&
                    Permission.checkBluetoothConnectPermission(context)
                ) {
                    deviceName.value =
                        bluetoothManager.adapter.getRemoteDevice(model.selectedDevice)?.name ?: ""
                }
            }
            val deviceContent = when {
                model.selectedDevice == null -> "None"
                deviceName.value.isEmpty() -> "${model.selectedDevice}"
                else -> "${deviceName.value} (${model.selectedDevice})"
            }
            TwoLineInfoCard(title = "Device", content = deviceContent) {
                navController.navigate(DEVICES)
            }
            RotationCard(model)
            Divider(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            )
            val version = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
            TwoLineInfoCard(stringResource(R.string.application_version), content = version)
            TwoLineInfoCard(stringResource(R.string.author), "Kin Lo (kin@lospot.tw)") {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://kin.lospot.tw"))
                )
            }
        }
    }
}

@Composable
private fun RotationCard(model: UiModel) {
    InfoCard {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.orientation),
                fontWeight = FontWeight.Bold,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RotationIcon(
                    model,
                    painterResource(id = R.drawable.ic_screen_rotation_alt),
                    ActivityInfo.SCREEN_ORIENTATION_SENSOR,
                )
                RotationIcon(
                    model,
                    painterResource(id = R.drawable.ic_screen_lock_landscape),
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
                )
                RotationIcon(
                    model,
                    painterResource(id = R.drawable.ic_screen_lock_portrait),
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
                )
            }
        }
    }
}

@Composable
private fun RotationIcon(
    model: UiModel,
    painter: Painter,
    value: Int,
) {
    val selected = model.orientation == value
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    PageButton(
        painter = painter,
        selected = selected,
        onClick = {
            scope.launch {
                BtSettings(context).setRequestedOrientation(
                    if (selected) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else value
                )
            }
        }
    )
}