package com.codecrafters.kehdona.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.codecrafters.kehdona.ui.theme.ErrorRose
import com.codecrafters.kehdona.ui.theme.Fraunces
import com.codecrafters.kehdona.ui.theme.Ink
import com.codecrafters.kehdona.ui.theme.InkMuted
import com.codecrafters.kehdona.ui.theme.InkSubtle
import com.codecrafters.kehdona.ui.theme.Lavender200
import com.codecrafters.kehdona.ui.theme.Lavender300
import com.codecrafters.kehdona.ui.theme.LineStrong
import com.codecrafters.kehdona.ui.theme.Night950
import com.codecrafters.kehdona.ui.theme.Plum800
import com.codecrafters.kehdona.ui.theme.Rose300
import com.codecrafters.kehdona.ui.theme.Rose400

/** Brand text field: soft glass container, rose focus ring, error text underneath. */
@Composable
fun KehDoNaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
    contentType: ContentType? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .then(if (contentType != null) Modifier.semantics { this.contentType = contentType } else Modifier),
        label = { Text(label) },
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        isError = error != null,
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = trailing,
        supportingText = (error ?: supportingText)?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.06f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.035f),
            disabledContainerColor = Color.White.copy(alpha = 0.02f),
            errorContainerColor = ErrorRose.copy(alpha = 0.05f),
            focusedBorderColor = Rose300,
            unfocusedBorderColor = LineStrong,
            focusedLabelColor = Rose300,
            unfocusedLabelColor = InkMuted,
            focusedLeadingIconColor = Rose300,
            unfocusedLeadingIconColor = InkSubtle,
            cursorColor = Rose300,
            unfocusedSupportingTextColor = InkSubtle,
            focusedSupportingTextColor = InkMuted,
        ),
    )
}

/** Password field with a show/hide toggle. */
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    supportingText: String? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    isNewPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    KehDoNaTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        error = error,
        supportingText = supportingText,
        leadingIcon = leadingIcon,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction() }),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        contentType = if (isNewPassword) ContentType.NewPassword else ContentType.Password,
        trailing = {
            TextButton(onClick = { visible = !visible }) {
                Text(if (visible) "Hide" else "Show", style = MaterialTheme.typography.labelMedium, color = Lavender300)
            }
        },
    )
}

enum class MessageTone { Error, Info, Success }

/** Animated banner for form-level errors and confirmations. Announced by screen readers. */
@Composable
fun InlineMessage(
    message: String?,
    modifier: Modifier = Modifier,
    tone: MessageTone = MessageTone.Error,
) {
    // Keep the last message while the banner animates out.
    val shown = remember { mutableStateOf(message) }
    if (message != null) shown.value = message
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter = fadeIn(tween(220)) + expandVertically(tween(260)),
        exit = fadeOut(tween(160)) + shrinkVertically(tween(200)),
    ) {
        val (tint, icon) = when (tone) {
            MessageTone.Error -> ErrorRose to Icons.Filled.Warning
            MessageTone.Info -> Lavender300 to Icons.Filled.Info
            MessageTone.Success -> Rose300 to Icons.Filled.CheckCircle
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    liveRegion = if (tone == MessageTone.Error) LiveRegionMode.Assertive else LiveRegionMode.Polite
                },
            shape = MaterialTheme.shapes.small,
            color = tint.copy(alpha = 0.09f),
            border = BorderStroke(1.dp, tint.copy(alpha = 0.3f)),
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(shown.value.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Lavender200)
            }
        }
    }
}

/** Two-option pill switch, e.g. Phone / Email, with a sliding rose highlight. */
@Composable
fun <T> SegmentedToggle(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.04f),
        border = BorderStroke(1.dp, LineStrong),
    ) {
        BoxWithConstraints(Modifier.padding(4.dp)) {
            val segment = maxWidth / options.size
            val offset by animateDpAsState(segment * selectedIndex, spring(dampingRatio = 0.8f, stiffness = 500f), label = "toggle")
            Surface(
                modifier = Modifier
                    .offset { IntOffset(offset.roundToPx(), 0) }
                    .width(segment)
                    .fillMaxHeight(),
                shape = CircleShape,
                color = Plum800,
                border = BorderStroke(1.dp, Rose300.copy(alpha = 0.35f)),
            ) {}
            Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
                options.forEach { (value, label) ->
                    val isSelected = value == selected
                    val color by animateColorAsState(if (isSelected) Ink else InkMuted, label = "toggleText")
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .selectable(selected = isSelected, enabled = enabled, role = Role.Tab) { onSelect(value) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(label, style = MaterialTheme.typography.labelLarge, color = color)
                    }
                }
            }
        }
    }
}

/**
 * Six separate digit boxes backed by one hidden field, so paste and SMS autofill
 * work. Filled digits pop in; the active box glows rose.
 */
@Composable
fun OtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(length)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .semantics {
                contentType = ContentType.SmsOtpCode
                contentDescription = "Verification code, ${value.length} of $length digits entered"
                if (isError) error("Incorrect code")
            },
        decorationBox = { inner ->
            Box {
                // The real field sits underneath so it keeps focus, IME and paste handling.
                Box(Modifier.size(1.dp)) { inner() }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(length) { index ->
                        OtpCell(
                            digit = value.getOrNull(index),
                            active = focused && enabled && index == value.length.coerceAtMost(length - 1),
                            isError = isError,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun OtpCell(digit: Char?, active: Boolean, isError: Boolean, modifier: Modifier = Modifier) {
    val border by animateColorAsState(
        when {
            isError -> ErrorRose
            active -> Rose300
            digit != null -> Lavender300.copy(alpha = 0.5f)
            else -> LineStrong
        },
        label = "otpBorder",
    )
    val scale by animateFloatAsState(
        if (digit != null) 1f else 0.6f,
        spring(dampingRatio = 0.5f, stiffness = 600f),
        label = "otpDigit",
    )
    Surface(
        modifier = modifier
            .aspectRatio(0.82f)
            .defaultMinSize(minWidth = 40.dp),
        shape = MaterialTheme.shapes.small,
        color = if (active) Rose300.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
        border = BorderStroke(if (active) 2.dp else 1.dp, border),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit?.toString().orEmpty(),
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = Fraunces),
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    alpha = if (digit != null) 1f else 0f
                },
            )
        }
    }
}

/** "Continue with Google" following Google's light button style: white pill, multicolour G. */
@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Continue with Google",
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        shape = CircleShape,
        border = BorderStroke(1.dp, Color(0xFF747775)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color(0xFF1F1F1F),
            disabledContainerColor = Color.White.copy(alpha = 0.6f),
            disabledContentColor = Color(0xFF1F1F1F).copy(alpha = 0.6f),
        ),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = Night950, strokeWidth = 2.dp)
        } else {
            GoogleG(Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun GoogleG(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = size.minDimension * 0.2f
        val inset = stroke / 2
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(inset, inset)
        fun arc(color: Color, start: Float, sweep: Float) =
            drawArc(color, start, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        arc(Color(0xFFEA4335), 200f, 115f) // red, top
        arc(Color(0xFFFBBC05), 140f, 60f) // yellow, left
        arc(Color(0xFF34A853), 45f, 95f) // green, bottom
        arc(Color(0xFF4285F4), 0f, 45f) // blue, right
        drawLine(
            Color(0xFF4285F4),
            start = Offset(size.width / 2, size.height / 2),
            end = Offset(size.width - inset / 2, size.height / 2),
            strokeWidth = stroke,
        )
    }
}

/** "or" between two hairlines. */
@Composable
fun OrDivider(modifier: Modifier = Modifier, text: String = "or") {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(LineStrong))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = InkSubtle,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        Box(Modifier.weight(1f).height(1.dp).background(LineStrong))
    }
}

/** Rose text link used inside forms. */
@Composable
fun LinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled, modifier = modifier.defaultMinSize(minHeight = 48.dp)) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) Rose400 else InkSubtle)
    }
}
