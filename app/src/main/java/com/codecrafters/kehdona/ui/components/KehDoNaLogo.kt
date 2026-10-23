package com.codecrafters.kehdona.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codecrafters.kehdona.ui.theme.Fraunces
import com.codecrafters.kehdona.ui.theme.Ink
import com.codecrafters.kehdona.ui.theme.Lavender400
import com.codecrafters.kehdona.ui.theme.Night950
import com.codecrafters.kehdona.ui.theme.Rose300

/** The KehDoNa heart mark, drawn on a 32×32 grid. */
@Composable
fun HeartMark(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Canvas(modifier.size(size)) {
        scale(this.size.minDimension / 32f, pivot = Offset.Zero) {
            val heart = Path().apply {
                moveTo(16f, 27.5f)
                cubicTo(15.6f, 27.5f, 15.2f, 27.4f, 14.9f, 27.1f)
                cubicTo(9.4f, 22.4f, 4f, 17.9f, 4f, 12.2f)
                cubicTo(4f, 8.6f, 6.8f, 5.5f, 10.4f, 5.5f)
                cubicTo(12.6f, 5.5f, 14.6f, 6.6f, 16f, 8.4f)
                cubicTo(17.4f, 6.6f, 19.4f, 5.5f, 21.6f, 5.5f)
                cubicTo(25.2f, 5.5f, 28f, 8.6f, 28f, 12.2f)
                cubicTo(28f, 17.9f, 22.6f, 22.4f, 17.1f, 27.1f)
                cubicTo(16.8f, 27.4f, 16.4f, 27.5f, 16f, 27.5f)
                close()
            }
            drawPath(
                heart,
                Brush.linearGradient(listOf(Rose300, Lavender400), Offset(4f, 5f), Offset(28f, 28f)),
            )
            val smile = Path().apply {
                moveTo(11.5f, 13.2f)
                quadraticTo(16f, 18.6f, 20.5f, 13.2f)
            }
            drawPath(smile, Night950, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
        }
    }
}

/** Heart mark plus the "KehDoNa" wordmark with an italic rose "Do". */
@Composable
fun KehDoNaLogo(
    modifier: Modifier = Modifier,
    markSize: Dp = 28.dp,
    fontSize: TextUnit = 24.sp,
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "KehDoNa" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HeartMark(size = markSize)
        Text(
            text = buildAnnotatedString {
                append("Keh")
                withStyle(SpanStyle(color = Rose300, fontStyle = FontStyle.Italic)) { append("Do") }
                append("Na")
            },
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                fontSize = fontSize,
                color = Ink,
            ),
        )
    }
}
