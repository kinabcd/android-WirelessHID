package tw.lospot.kin.wirelesshid.ui.keyboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun RowScope.KeyButton(
    text: String = "●",
    fontSize: TextUnit = TextUnit.Unspecified,
    colSpan: Float = 1f,
    onClick: (Boolean) -> Unit = {}
) {
    var down by remember { mutableStateOf(false) }

    Box(Modifier
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    down = true
                    onClick(true)
                    tryAwaitRelease()
                    down = false
                    onClick(false)
                }
            )
        }
        .weight(colSpan)
        .fillMaxHeight()
        .padding(1.dp)
        .background(if (down) Color.Gray else Color.Transparent)
        .border(BorderStroke(1.dp, Color.Gray))) {
        Text(
            text = text,
            modifier = Modifier.align(Alignment.Center),
            fontSize = fontSize,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun RowScope.KeyButton(
    text: String = "●",
    params: KeyButtonParameter,
    colSpan: Float = 1f,
    onClick: (Boolean) -> Unit = {}
) = KeyButton(
    text = text,
    fontSize = params.fontSize,
    colSpan = colSpan,
    onClick = onClick
)

data class KeyButtonParameter(
    val fontSize: TextUnit = TextUnit.Unspecified,
)