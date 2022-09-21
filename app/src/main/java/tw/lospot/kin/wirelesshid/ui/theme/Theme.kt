package tw.lospot.kin.wirelesshid.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tw.lospot.kin.wirelesshid.ui.PageContent

private val DarkColorPalette = darkColors(
    primary = Color(0xFF213361),
    primaryVariant = Color(0xFF000088.toInt()),
    onPrimary = Color(0xFFB4B4B4),
    secondary = Color(0xFF2B73CC),
    secondaryVariant = Color(0xFF2B73CC),
    onSecondary = Color(0xFFE6E6E6),
    background = Color.Black,
    surface = Color(0xFF222222.toInt()),
    onSurface = Color(0xFFAAAAAA.toInt()),
)

private val LightColorPalette = lightColors(
    primary = Color(0xFFEAF3FF),
    primaryVariant = Color(0xFF4783FC),
    onPrimary = Color(0xFF0F1014),
    secondary = Color(0xFF358FFC),
    secondaryVariant = Color(0xFF358FFC),
    background = Color.White,
    surface = Color(0xFFFEFEFE.toInt()),
    onSurface = Color(0xFF666666.toInt()),
)

@Composable
fun ToolkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Preview
@Composable
private fun PreviewThemeDark() {
    PreviewTheme(true)
}

@Preview
@Composable
private fun PreviewThemeLight() {
    PreviewTheme(false)
}

@Composable
private fun PreviewTheme(darkTheme: Boolean = false) {
    ToolkitTheme(darkTheme = darkTheme) {
        PageContent(title = "Preview") {
            Column {
                ColorText("Primary", MaterialTheme.colors.primary)
                ColorText("PrimaryVariant", MaterialTheme.colors.primaryVariant)
                ColorText("Secondary", MaterialTheme.colors.secondary)
                ColorText("SecondaryVariant", MaterialTheme.colors.secondaryVariant)
                ColorText("Surface", MaterialTheme.colors.surface)
                ColorText("Background", MaterialTheme.colors.background)
                ColorText("Error", MaterialTheme.colors.error)
                Divider(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
                Row {
                    Switch(checked = true, onCheckedChange = {})
                    Switch(checked = false, onCheckedChange = {})
                }
                Row {
                    OutlinedTextField(value = "Text", onValueChange = {})
                    OutlinedTextField(enabled = false, value = "Text", onValueChange = {})
                }
                Row {
                    Button(onClick = { }) { Text("Button") }
                    Button(enabled = false, onClick = { }) { Text("Button") }
                }
            }
        }
    }
}

@Composable
private fun ColorText(text: String, color: Color) {
    Box(
        Modifier
            .background(color)
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colors.contentColorFor(color)
        )
    }

}