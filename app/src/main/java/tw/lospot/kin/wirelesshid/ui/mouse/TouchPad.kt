package tw.lospot.kin.wirelesshid.ui.mouse

import android.view.MotionEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlin.math.max

@Composable
fun TouchPad(
    modifier: Modifier = Modifier,
    onKey: (keycode: Int, down: Boolean) -> Unit,
    onMove: (dx: Int, dy: Int) -> Unit,
    onScroll: (dx: Int, dy: Int) -> Unit,
) {
    Column(modifier = modifier) {
        var scrollMode = remember { false }
        var scrollOffset: Offset = remember { Offset.Zero }
        Box(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .border(BorderStroke(1.dp, Color.Gray))
            .pointerInput(Unit) {
                detectTapAndDragGestures(
                    onPress = {
                        val rightOffset = size.width - it.x
                        if (rightOffset < size.width / 10f && rightOffset < 50.dp.toPx()) {
                            scrollMode = true
                        }
                    },
                    onDrag = { count, change, dragAmount ->
                        when {
                            count == 2 || scrollMode -> {
                                scrollOffset += dragAmount
                                val scaledX = (scrollOffset.x / 10f).toInt()
                                val scaledY = (scrollOffset.y / 10f).toInt()
                                scrollOffset -= Offset(scaledX * 10f, scaledY * 10f)
                                if (scaledX != 0 || scaledY != 0) {
                                    onScroll(scaledX, scaledY)
                                }
                            }
                            count == 1 -> onMove(dragAmount.x.toInt(), dragAmount.y.toInt())
                        }
                    },
                    onRelease = { distance, maxCount, duration, position ->
                        scrollMode = false
                        scrollOffset = Offset.Zero
                        if (distance < 15 && duration < 150) {
                            when (maxCount) {
                                1 -> {
                                    onKey(MotionEvent.BUTTON_PRIMARY, true)
                                    onKey(MotionEvent.BUTTON_PRIMARY, false)
                                }
                                2 -> {
                                    onKey(MotionEvent.BUTTON_SECONDARY, true)
                                    onKey(MotionEvent.BUTTON_SECONDARY, false)
                                }
                                3 -> {
                                    onKey(MotionEvent.BUTTON_TERTIARY, true)
                                    onKey(MotionEvent.BUTTON_TERTIARY, false)
                                }
                            }
                        }
                    }
                )
            }
        )
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

suspend fun PointerInputScope.detectTapAndDragGestures(
    onPress: (position: Offset) -> Unit,
    onDrag: (currentCount: Int, change: PointerInputChange, dragAmount: Offset) -> Unit,
    onRelease: (distance: Float, maxCount: Int, duration: Long, position: Offset) -> Unit,
) = coroutineScope {
    forEachGesture {
        awaitPointerEventScope {
            val down = awaitFirstDown(requireUnconsumed = false)
            var up: PointerInputChange = down
            var moveDistance = 0f
            var maxCount = 1
            onPress(down.position)
            do {
                val event = awaitPointerEvent()
                val canceled = event.changes.any { it.isConsumed }
                if (!canceled) {
                    maxCount = max(maxCount, event.changes.size)
                    event.changes.firstOrNull { down.id == it.id }?.let {
                        val distance = it.positionChange().getDistance()
                        moveDistance += distance
                        up = it
                        if (distance != 0f) {
                            onDrag(event.changes.size, it, it.positionChange())
                        }
                    }
                }
            } while (!canceled && event.changes.any { it.pressed })
            onRelease(moveDistance, maxCount, up.uptimeMillis - down.uptimeMillis, up.position)
        }
    }
}