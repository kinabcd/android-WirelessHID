package tw.lospot.kin.wirelesshid.ui.keyboard

import android.view.KeyEvent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*

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
    val params = remember(bSize) { KeyButtonParameter(size = bSize) }
    val funParams = remember(params) { params.copy(fontSize = bSize.value.sp / 3f) }
    var fnMode by remember { mutableStateOf(false) }
    Column(modifier = Modifier
        .fillMaxWidth()
        .onSizeChanged { size = it }) {
        Row {
            KeyButton("ESC", funParams, colSpan = 1.5f) { onKey(KeyEvent.KEYCODE_ESCAPE, it) }
            if (fnMode) {
                KeyButton("Mute", funParams) { onKey(KeyEvent.KEYCODE_VOLUME_MUTE, it) }
                KeyButton("VolDn", funParams) { onKey(KeyEvent.KEYCODE_VOLUME_DOWN, it) }
                KeyButton("VolUp", funParams) { onKey(KeyEvent.KEYCODE_VOLUME_UP, it) }
                KeyButton("Media\nPrev", funParams) { onKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS, it) }
                KeyButton("Media\nPlay", funParams) { onKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, it) }
                KeyButton("Media\nNext", funParams) { onKey(KeyEvent.KEYCODE_MEDIA_NEXT, it) }
                KeyButton("Media\nStop", funParams) { onKey(KeyEvent.KEYCODE_MEDIA_STOP, it) }
                KeyButton(" ", funParams) { }
                KeyButton("ScrLk", funParams) { onKey(KeyEvent.KEYCODE_SCROLL_LOCK, it) }
                KeyButton("Pause", funParams) { onKey(KeyEvent.KEYCODE_BREAK, it) }
                KeyButton("PrtSc", funParams) { onKey(KeyEvent.KEYCODE_SYSRQ, it) }
                KeyButton("Ins", funParams) { onKey(KeyEvent.KEYCODE_INSERT, it) }
            } else {
                KeyButton("F1", funParams) { onKey(KeyEvent.KEYCODE_F1, it) }
                KeyButton("F2", funParams) { onKey(KeyEvent.KEYCODE_F2, it) }
                KeyButton("F3", funParams) { onKey(KeyEvent.KEYCODE_F3, it) }
                KeyButton("F4", funParams) { onKey(KeyEvent.KEYCODE_F4, it) }
                KeyButton("F5", funParams) { onKey(KeyEvent.KEYCODE_F5, it) }
                KeyButton("F6", funParams) { onKey(KeyEvent.KEYCODE_F6, it) }
                KeyButton("F7", funParams) { onKey(KeyEvent.KEYCODE_F7, it) }
                KeyButton("F8", funParams) { onKey(KeyEvent.KEYCODE_F8, it) }
                KeyButton("F9", funParams) { onKey(KeyEvent.KEYCODE_F9, it) }
                KeyButton("F10", funParams) { onKey(KeyEvent.KEYCODE_F10, it) }
                KeyButton("F11", funParams) { onKey(KeyEvent.KEYCODE_F11, it) }
                KeyButton("F12", funParams) { onKey(KeyEvent.KEYCODE_F12, it) }
            }
            KeyButton("Del", funParams) { onKey(KeyEvent.KEYCODE_FORWARD_DEL, it) }
        }
        Spacer(modifier = Modifier.height(bSize / 3f))
        Row {
            KeyButton("`", params) { onKey(KeyEvent.KEYCODE_GRAVE, it) }
            KeyButton("1", params) { onKey(KeyEvent.KEYCODE_1, it) }
            KeyButton("2", params) { onKey(KeyEvent.KEYCODE_2, it) }
            KeyButton("3", params) { onKey(KeyEvent.KEYCODE_3, it) }
            KeyButton("4", params) { onKey(KeyEvent.KEYCODE_4, it) }
            KeyButton("5", params) { onKey(KeyEvent.KEYCODE_5, it) }
            KeyButton("6", params) { onKey(KeyEvent.KEYCODE_6, it) }
            KeyButton("7", params) { onKey(KeyEvent.KEYCODE_7, it) }
            KeyButton("8", params) { onKey(KeyEvent.KEYCODE_8, it) }
            KeyButton("9", params) { onKey(KeyEvent.KEYCODE_9, it) }
            KeyButton("0", params) { onKey(KeyEvent.KEYCODE_0, it) }
            KeyButton("-", params) { onKey(KeyEvent.KEYCODE_MINUS, it) }
            KeyButton("=", params) { onKey(KeyEvent.KEYCODE_EQUALS, it) }
            KeyButton("←", params, colSpan = 1.5f) { onKey(KeyEvent.KEYCODE_DEL, it) }
        }
        Row {
            KeyButton("TAB", funParams, colSpan = 1.5f) { onKey(KeyEvent.KEYCODE_TAB, it) }
            KeyButton("Q", params) { onKey(KeyEvent.KEYCODE_Q, it) }
            KeyButton("W", params) { onKey(KeyEvent.KEYCODE_W, it) }
            KeyButton("E", params) { onKey(KeyEvent.KEYCODE_E, it) }
            KeyButton("R", params) { onKey(KeyEvent.KEYCODE_R, it) }
            KeyButton("T", params) { onKey(KeyEvent.KEYCODE_T, it) }
            KeyButton("Y", params) { onKey(KeyEvent.KEYCODE_Y, it) }
            KeyButton("U", params) { onKey(KeyEvent.KEYCODE_U, it) }
            KeyButton("I", params) { onKey(KeyEvent.KEYCODE_I, it) }
            KeyButton("O", params) { onKey(KeyEvent.KEYCODE_O, it) }
            KeyButton("P", params) { onKey(KeyEvent.KEYCODE_P, it) }
            KeyButton("[", params) { onKey(KeyEvent.KEYCODE_LEFT_BRACKET, it) }
            KeyButton("]", params) { onKey(KeyEvent.KEYCODE_RIGHT_BRACKET, it) }
            KeyButton("\\", params) { onKey(KeyEvent.KEYCODE_BACKSLASH, it) }
        }
        Row {
            KeyButton("CAPS", funParams, colSpan = 2f) { onKey(KeyEvent.KEYCODE_CAPS_LOCK, it) }
            KeyButton("A", params) { onKey(KeyEvent.KEYCODE_A, it) }
            KeyButton("S", params) { onKey(KeyEvent.KEYCODE_S, it) }
            KeyButton("D", params) { onKey(KeyEvent.KEYCODE_D, it) }
            KeyButton("F", params) { onKey(KeyEvent.KEYCODE_F, it) }
            KeyButton("G", params) { onKey(KeyEvent.KEYCODE_G, it) }
            KeyButton("H", params) { onKey(KeyEvent.KEYCODE_H, it) }
            KeyButton("J", params) { onKey(KeyEvent.KEYCODE_J, it) }
            KeyButton("K", params) { onKey(KeyEvent.KEYCODE_K, it) }
            KeyButton("L", params) { onKey(KeyEvent.KEYCODE_L, it) }
            KeyButton(";", params) { onKey(KeyEvent.KEYCODE_SEMICOLON, it) }
            KeyButton("'", params) { onKey(KeyEvent.KEYCODE_APOSTROPHE, it) }
            KeyButton("↵", params, colSpan = 1.5f) { onKey(KeyEvent.KEYCODE_ENTER, it) }
        }
        Row {
            KeyButton("△", params, colSpan = 1.5f) { onKey(KeyEvent.KEYCODE_SHIFT_LEFT, it) }
            KeyButton("Fn", funParams) { if (it) fnMode = !fnMode }
            KeyButton("Z", params) { onKey(KeyEvent.KEYCODE_Z, it) }
            KeyButton("X", params) { onKey(KeyEvent.KEYCODE_X, it) }
            KeyButton("C", params) { onKey(KeyEvent.KEYCODE_C, it) }
            KeyButton("V", params) { onKey(KeyEvent.KEYCODE_V, it) }
            KeyButton("B", params) { onKey(KeyEvent.KEYCODE_B, it) }
            KeyButton("N", params) { onKey(KeyEvent.KEYCODE_N, it) }
            KeyButton("M", params) { onKey(KeyEvent.KEYCODE_M, it) }
            KeyButton(",", params) { onKey(KeyEvent.KEYCODE_COMMA, it) }
            KeyButton(".", params) { onKey(KeyEvent.KEYCODE_PERIOD, it) }
            KeyButton("/", params) { onKey(KeyEvent.KEYCODE_SLASH, it) }
            if (fnMode) {
                KeyButton("PgUp", funParams) { onKey(KeyEvent.KEYCODE_PAGE_UP, it) }
            } else {
                KeyButton("ᐃ", params) { onKey(KeyEvent.KEYCODE_DPAD_UP, it) }
            }

            KeyButton("△", params) { onKey(KeyEvent.KEYCODE_SHIFT_RIGHT, it) }
        }
        Row {
            KeyButton("Ctrl", funParams) { onKey(KeyEvent.KEYCODE_CTRL_LEFT, it) }
            KeyButton("Win", funParams) { onKey(KeyEvent.KEYCODE_META_LEFT, it) }
            KeyButton("Alt", funParams) { onKey(KeyEvent.KEYCODE_ALT_LEFT, it) }
            KeyButton(" ", params, colSpan = 5.5f) { onKey(KeyEvent.KEYCODE_SPACE, it) }
            KeyButton("Alt", funParams) { onKey(KeyEvent.KEYCODE_ALT_RIGHT, it) }
            KeyButton("Win", funParams) { onKey(KeyEvent.KEYCODE_META_RIGHT, it) }
            KeyButton("Ctrl", funParams) { onKey(KeyEvent.KEYCODE_CTRL_RIGHT, it) }
            if (fnMode) {
                KeyButton("Home", funParams) { onKey(KeyEvent.KEYCODE_MOVE_HOME, it) }
                KeyButton("PgDn", funParams) { onKey(KeyEvent.KEYCODE_PAGE_DOWN, it) }
                KeyButton("End", funParams) { onKey(KeyEvent.KEYCODE_MOVE_END, it) }
            } else {
                KeyButton("ᐊ", params) { onKey(KeyEvent.KEYCODE_DPAD_LEFT, it) }
                KeyButton("ᐁ", params) { onKey(KeyEvent.KEYCODE_DPAD_DOWN, it) }
                KeyButton("ᐅ", params) { onKey(KeyEvent.KEYCODE_DPAD_RIGHT, it) }
            }
        }
    }
}