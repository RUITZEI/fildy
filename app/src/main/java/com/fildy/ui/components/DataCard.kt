package com.fildy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fildy.ui.theme.Blanco
import com.fildy.ui.theme.CardShadow
import com.fildy.ui.theme.Musgo
import com.fildy.ui.theme.Tinta

private val CardShape = RoundedCornerShape(28.dp)

/** White data card for the home screen ("Tarjeta de dato"): radius 28 dp, soft green shadow. */
@Composable
fun DataCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .dropShadow(CardShape, Shadow(radius = 30.dp, color = CardShadow, offset = DpOffset(0.dp, 10.dp)))
            .background(Blanco, CardShape)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp),
        content = content,
    )
}

/**
 * Card header: a 38 dp round icon, title and subtitle on the left; a big number with its unit
 * on the right.
 */
@Composable
fun DataCardHeader(
    @DrawableRes icon: Int,
    iconTint: Color,
    iconBackground: Color,
    title: String,
    subtitle: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Tinta,
    unitColor: Color = Musgo,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .background(iconBackground, CircleShape),
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Tinta)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Musgo)
            }
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp, color = valueColor)) {
                    append(value)
                }
                withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = unitColor)) {
                    append(" $unit")
                }
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
