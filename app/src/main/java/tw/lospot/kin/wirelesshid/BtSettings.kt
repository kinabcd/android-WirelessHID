package tw.lospot.kin.wirelesshid

import android.content.Context
import android.content.pm.ActivityInfo
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

class BtSettings(context: Context) {
    companion object {
        private val KEY_REQUESTED_ORIENTATION = intPreferencesKey("requestedOrientation")
        private val KEY_SELECTED_ADDRESS = stringPreferencesKey("selectedAddress")
        private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keepScreenOn")
        private val Context.dataStore by preferencesDataStore("BtSettings")
    }

    private val context = context.applicationContext
    val selectedAddress = context.dataStore.data.map { it[KEY_SELECTED_ADDRESS] ?: "" }
    val requestedOrientation = context.dataStore.data.map {
        it[KEY_REQUESTED_ORIENTATION] ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }
    val requestedKeepScreenOn = context.dataStore.data.map { it[KEY_KEEP_SCREEN_ON] ?: true }

    suspend fun setSelectedAddress(address: String) =
        context.dataStore.edit { it[KEY_SELECTED_ADDRESS] = address }

    suspend fun setRequestedOrientation(orientation: Int) =
        context.dataStore.edit { it[KEY_REQUESTED_ORIENTATION] = orientation }

    suspend fun setRequestedKeepScreenOn(on: Boolean) =
        context.dataStore.edit { it[KEY_KEEP_SCREEN_ON] = on }
}