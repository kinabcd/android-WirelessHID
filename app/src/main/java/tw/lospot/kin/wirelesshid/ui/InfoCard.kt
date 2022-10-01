package tw.lospot.kin.wirelesshid.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tw.lospot.kin.wirelesshid.ui.theme.Shapes


@Composable
fun TwoLineInfoCard(
    title: String,
    content: String,
    contentFontFamily: FontFamily? = null,
    backgroundColor: Color = MaterialTheme.colors.surface,
    onClick: (() -> Unit)? = null
) {
    InfoCard(
        backgroundColor = backgroundColor,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(content, fontFamily = contentFontFamily)
        }
    }
}

@Composable
fun InfoCard(
    backgroundColor: Color = MaterialTheme.colors.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = Shapes.small,
        color = backgroundColor,
        border = BorderStroke(1.dp, contentColorFor(backgroundColor).copy(alpha = 0.2f)),
        content = content
    )
}