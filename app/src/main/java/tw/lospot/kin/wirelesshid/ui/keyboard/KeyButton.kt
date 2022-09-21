package tw.lospot.kin.wirelesshid.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun KeyButton(
    text: String = "●",
    size: Dp = 40.dp,
    colSpan: Float = 1f,
    onClick: (Boolean) -> Unit = {}
) {
    Box(Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { onClick(true) },
                onTap = { onClick(false) },
            )
        }
        .width(size * colSpan)
        .height(size)
        .padding(1.dp)
        .border(BorderStroke(1.dp, Color.Gray))) {
        Text(text = text, modifier = Modifier.align(Alignment.Center))
    }
}