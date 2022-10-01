package tw.lospot.kin.wirelesshid.ui.screens

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import tw.lospot.kin.wirelesshid.R
import tw.lospot.kin.wirelesshid.bluetooth.State
import tw.lospot.kin.wirelesshid.ui.InfoCard
import tw.lospot.kin.wirelesshid.ui.PageContent
import tw.lospot.kin.wirelesshid.ui.PageLazyColumn
import tw.lospot.kin.wirelesshid.ui.UiModel
import tw.lospot.kin.wirelesshid.util.Permission

@Composable
fun DeviceScreen(
    navController: NavController,
    model: UiModel = viewModel(),
) {
    PageContent(
        title = stringResource(id = R.string.title_devices),
        backAction = { navController.popBackStack() },
    ) {
        DeviceList(model = model) {
            model.connection?.selectDevice(it)
        }
    }
}

@Composable
fun DeviceList(
    model: UiModel = viewModel(),
    onDeviceClicked: (address: String?) -> Unit = {}
) {
    val context = LocalContext.current
    val devices = remember { mutableStateListOf<Pair<String, String>>() }
    LaunchedEffect(true) {
        updateDevices(context, devices)
    }

    PageLazyColumn {
        item {
            DeviceItem(
                address = null,
                name = "None",
                checked = model.selectedDevice == null,
                onDeviceClicked = onDeviceClicked
            )
        }
        items(devices, key = { it.first }) {
            val state = when (it.first) {
                model.currentDevice -> when (model.state) {
                    State.DISCONNECTING -> "Disconnecting"
                    State.CONNECTING -> "Connecting"
                    State.CONNECTED -> "Connected"
                    else -> ""
                }
                else -> ""
            }
            DeviceItem(
                address = it.first, name = it.second,
                checked = it.first == model.selectedDevice,
                state = state,
                onDeviceClicked = onDeviceClicked,
            )
        }
    }
}

@Composable
private fun DeviceItem(
    address: String?,
    name: String?,
    checked: Boolean = false,
    state: String = "",
    onDeviceClicked: (address: String?) -> Unit = {}
) {
    InfoCard(
        onClick = { onDeviceClicked(address) }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .let {
                    when {
                        checked -> it.background(Color(0x2200FF00))
                        else -> it
                    }
                }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (!name.isNullOrBlank()) Text(name, fontWeight = FontWeight.Bold)
                if (!address.isNullOrBlank()) Text(address, fontFamily = null)
            }
            if (state.isNotBlank()) Text(text = state, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

private fun updateDevices(context: Context, list: SnapshotStateList<Pair<String, String>>) {
    val newList = if (Permission.checkBluetoothConnectPermission(context)) {
        (context.getSystemService(ComponentActivity.BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter.bondedDevices.map {
                it.address to it.name
            }
    } else emptyList<Pair<String, String>>()
    list.removeAll { it !in newList }
    list.addAll(newList.filter { it !in list })
}