package tw.lospot.kin.wirelesshid.ui.keyboard

import android.view.KeyEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min

@Preview(
    widthDp = 785,
    heightDp = 392,
)
@Preview(
    widthDp = 392,
    heightDp = 785,
)
@Composable
fun QwertKeyboard(onKey: (keycode: Int, down: Boolean) -> Unit = { _, _ -> }) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val bSize = with(LocalDensity.current) {
        max(20.dp, min(40.dp, size.width.toDp() / 14.5f))
    }
    Column(modifier = Modifier
        .fillMaxWidth()
        .onSizeChanged { size = it }) {
        Row {
            KeyButton("`", size = bSize) { onKey(KeyEvent.KEYCODE_GRAVE, it) }
            KeyButton("1", size = bSize) { onKey(KeyEvent.KEYCODE_1, it) }
            KeyButton("2", size = bSize) { onKey(KeyEvent.KEYCODE_2, it) }
            KeyButton("3", size = bSize) { onKey(KeyEvent.KEYCODE_3, it) }
            KeyButton("4", size = bSize) { onKey(KeyEvent.KEYCODE_4, it) }
            KeyButton("5", size = bSize) { onKey(KeyEvent.KEYCODE_5, it) }
            KeyButton("6", size = bSize) { onKey(KeyEvent.KEYCODE_6, it) }
            KeyButton("7", size = bSize) { onKey(KeyEvent.KEYCODE_7, it) }
            KeyButton("8", size = bSize) { onKey(KeyEvent.KEYCODE_8, it) }
            KeyButton("9", size = bSize) { onKey(KeyEvent.KEYCODE_9, it) }
            KeyButton("0", size = bSize) { onKey(KeyEvent.KEYCODE_0, it) }
            KeyButton("-", size = bSize) { onKey(KeyEvent.KEYCODE_MINUS, it) }
            KeyButton("=", size = bSize) { onKey(KeyEvent.KEYCODE_EQUALS, it) }
            KeyButton("←", size = bSize, colSpan = 1.5f) { onKey(KeyEvent.KEYCODE_DEL, it) }
        }
        Row {
            KeyButton("TAB", size = bSize, colSpan = 1.5f) { onKey(KeyEvent.KEYCODE_TAB, it) }
            KeyButton("Q", size = bSize) { onKey(KeyEvent.KEYCODE_Q, it) }
            KeyButton("W", size = bSize) { onKey(KeyEvent.KEYCODE_W, it) }
            KeyButton("E", size = bSize) { onKey(KeyEvent.KEYCODE_E, it) }
            KeyButton("R", size = bSize) { onKey(KeyEvent.KEYCODE_R, it) }
            KeyButton("T", size = bSize) { onKey(KeyEvent.KEYCODE_T, it) }
            KeyButton("Y", size = bSize) { onKey(KeyEvent.KEYCODE_Y, it) }
            KeyButton("U", size = bSize) { onKey(KeyEvent.KEYCODE_U, it) }
            KeyButton("I", size = bSize) { onKey(KeyEvent.KEYCODE_I, it) }
            KeyButton("O", size = bSize) { onKey(KeyEvent.KEYCODE_O, it) }
            KeyButton("P", size = bSize) { onKey(KeyEvent.KEYCODE_P, it) }
            KeyButton("[", size = bSize) { onKey(KeyEvent.KEYCODE_LEFT_BRACKET, it) }
            KeyButton("]", size = bSize) { onKey(KeyEvent.KEYCODE_RIGHT_BRACKET, it) }
            KeyButton("\\", size = bSize) { onKey(KeyEvent.KEYCODE_BACKSLASH, it) }
        }
        Row {
            KeyButton("CAPS", size = bSize, colSpan = 2f) { onKey(KeyEvent.KEYCODE_CAPS_LOCK, it) }
            KeyButton("A", size = bSize) { onKey(KeyEvent.KEYCODE_A, it) }
            KeyButton("S", size = bSize) { onKey(KeyEvent.KEYCODE_S, it) }
            KeyButton("D", size = bSize) { onKey(KeyEvent.KEYCODE_D, it) }
            KeyButton("F", size = bSize) { onKey(KeyEvent.KEYCODE_F, it) }
            KeyButton("G", size = bSize) { onKey(KeyEvent.KEYCODE_G, it) }
            KeyButton("H", size = bSize) { onKey(KeyEvent.KEYCODE_H, it) }
            KeyButton("J", size = bSize) { onKey(KeyEvent.KEYCODE_J, it) }
            KeyButton("K", size = bSize) { onKey(KeyEvent.KEYCODE_K, it) }
            KeyButton("L", size = bSize) { onKey(KeyEvent.KEYCODE_L, it) }
            KeyButton(";", size = bSize) { onKey(KeyEvent.KEYCODE_SEMICOLON, it) }
            KeyButton("'", size = bSize) { onKey(KeyEvent.KEYCODE_APOSTROPHE, it) }
            KeyButton("↵", size = bSize, colSpan = 1.5f) { onKey(KeyEvent.KEYCODE_ENTER, it) }
        }
        Row {
            KeyButton("△", size = bSize, colSpan = 2.5f) { onKey(KeyEvent.KEYCODE_SHIFT_LEFT, it) }
            KeyButton("Z", size = bSize) { onKey(KeyEvent.KEYCODE_Z, it) }
            KeyButton("X", size = bSize) { onKey(KeyEvent.KEYCODE_X, it) }
            KeyButton("C", size = bSize) { onKey(KeyEvent.KEYCODE_C, it) }
            KeyButton("V", size = bSize) { onKey(KeyEvent.KEYCODE_V, it) }
            KeyButton("B", size = bSize) { onKey(KeyEvent.KEYCODE_B, it) }
            KeyButton("N", size = bSize) { onKey(KeyEvent.KEYCODE_N, it) }
            KeyButton("M", size = bSize) { onKey(KeyEvent.KEYCODE_M, it) }
            KeyButton(",", size = bSize) { onKey(KeyEvent.KEYCODE_COMMA, it) }
            KeyButton(".", size = bSize) { onKey(KeyEvent.KEYCODE_PERIOD, it) }
            KeyButton("/", size = bSize) { onKey(KeyEvent.KEYCODE_SLASH, it) }
            KeyButton("ᐃ", size = bSize) { onKey(KeyEvent.KEYCODE_DPAD_UP, it) }
            KeyButton("△", size = bSize) { onKey(KeyEvent.KEYCODE_SHIFT_RIGHT, it) }
        }
        Row {
            KeyButton("Ct", size = bSize) { onKey(KeyEvent.KEYCODE_CTRL_LEFT, it) }
            KeyButton("Wi", size = bSize) { onKey(KeyEvent.KEYCODE_META_LEFT, it) }
            KeyButton("Al", size = bSize) { onKey(KeyEvent.KEYCODE_ALT_LEFT, it) }
            KeyButton(" ", size = bSize, colSpan = 5.5f) { onKey(KeyEvent.KEYCODE_SPACE, it) }
            KeyButton("Al", size = bSize) { onKey(KeyEvent.KEYCODE_ALT_RIGHT, it) }
            KeyButton("Wi", size = bSize) { onKey(KeyEvent.KEYCODE_META_RIGHT, it) }
            KeyButton("Ct", size = bSize) { onKey(KeyEvent.KEYCODE_CTRL_RIGHT, it) }
            KeyButton("ᐊ", size = bSize) { onKey(KeyEvent.KEYCODE_DPAD_LEFT, it) }
            KeyButton("ᐁ", size = bSize) { onKey(KeyEvent.KEYCODE_DPAD_DOWN, it) }
            KeyButton("ᐅ", size = bSize) { onKey(KeyEvent.KEYCODE_DPAD_RIGHT, it) }
        }
    }
}