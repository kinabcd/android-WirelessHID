package tw.lospot.kin.wirelesshid.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun PageButton(
    onClick: () -> Unit,
    painter: Painter,
    contentDescription: String? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
) {
    val backgroundColor =
        if (selected) MaterialTheme.colors.onSurface else MaterialTheme.colors.surface
    val contentColor =
        if (selected) MaterialTheme.colors.surface else MaterialTheme.colors.onSurface
    val colors = ButtonDefaults.buttonColors(
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        disabledBackgroundColor = backgroundColor,
        disabledContentColor = contentColor.copy(alpha = ContentAlpha.disabled)
    )

    OutlinedButton(
        modifier = Modifier.size(36.dp),
        contentPadding = PaddingValues(4.dp),
        enabled = enabled,
        colors = colors,
        onClick = onClick,
    ) {
        Icon(
            painter = painter,
            contentDescription = contentDescription,
        )
    }
}