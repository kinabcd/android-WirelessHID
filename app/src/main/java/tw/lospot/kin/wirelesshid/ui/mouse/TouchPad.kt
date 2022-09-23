package tw.lospot.kin.wirelesshid.ui.mouse

import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun TouchPad(
    modifier: Modifier = Modifier,
    onKey: (keycode: Int, down: Boolean) -> Unit,
    onMove: (dx: Int, dy: Int) -> Unit
) {
    Column(modifier = modifier) {
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color.Gray))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        onMove(dragAmount.x.toInt(), dragAmount.y.toInt())
                    },
                    onDragEnd = { onMove(0, 0) },
                    onDragCancel = { onMove(0, 0) },
                )
            })
        Row(Modifier.height(48.dp)) {
            MouseButton(
                MotionEvent.BUTTON_PRIMARY,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onKey = onKey
            )
            MouseButton(
                MotionEvent.BUTTON_TERTIARY,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onKey = onKey
            )
            MouseButton(
                MotionEvent.BUTTON_SECONDARY,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                onKey = onKey
            )
        }
    }
}

@Composable
private fun MouseButton(
    keycode: Int,
    modifier: Modifier = Modifier,
    onKey: (keycode: Int, down: Boolean) -> Unit,
) {
    var rightDown by remember { mutableStateOf(false) }
    Box(modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    rightDown = true
                    onKey(keycode, true)
                    tryAwaitRelease()
                    rightDown = false
                    onKey(keycode, false)
                }
            )
        }
        .background(if (rightDown) Color.Gray else Color.Transparent)
        .border(BorderStroke(1.dp, Color.Gray)))

}