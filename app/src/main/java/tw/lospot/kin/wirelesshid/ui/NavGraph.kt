package tw.lospot.kin.wirelesshid.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import tw.lospot.kin.wirelesshid.ui.screens.DeviceScreen
import tw.lospot.kin.wirelesshid.ui.screens.MainPanelScreen
import tw.lospot.kin.wirelesshid.ui.screens.SettingsScreen

const val MAIN_PANEL = "MainPanel"
const val SETTINGS = "Settings"
const val DEVICES = "Devices"

@Composable
fun SetupNavGraph(navController: NavHostController, model: UiModel = viewModel()) {
    NavHost(navController = navController, startDestination = MAIN_PANEL) {
        composable(MAIN_PANEL) { MainPanelScreen(navController, model) }
        composable(DEVICES) { DeviceScreen(navController, model) }
        composable(SETTINGS) { SettingsScreen(navController, model) }
    }
}