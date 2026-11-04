package ru.plumsoftware.habitstracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.plumsoftware.habitstracker.ui.theme.AppTheme
import ru.plumsoftware.habitstracker.ui.components.clickableNoRipple

/** Единая шапка внутренних экранов приложения. */
@Composable
fun AppScreenTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(56.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(colors.surface)
                .clickableNoRipple(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text("‹", color = colors.textPrimary, fontSize = 42.sp, lineHeight = 42.sp,
                fontWeight = FontWeight.Light)
        }
        Spacer(Modifier.width(18.dp))
        Text(title, color = colors.textPrimary, fontSize = 28.sp,
            lineHeight = 34.sp, fontWeight = FontWeight.Bold)
    }
}
