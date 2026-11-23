package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AcademicBlue
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OutlineStrongLight
import com.example.ui.theme.StatusErrorBg
import com.example.ui.theme.StatusErrorFg
import com.example.ui.theme.SurfaceVariantLight

@Composable
fun PinPadView(
    pinLength: Int,
    onDigitClick: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.widthIn(max = 260.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 4 PIN dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(4) { index ->
                val filled = index < pinLength
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (filled) AcademicBlue else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (filled) AcademicBlue else OutlineStrongLight,
                            shape = CircleShape,
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        )

        rows.forEach { rowDigits ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowDigits.forEach { digit ->
                    FilledTonalButton(
                        onClick = { onDigitClick(digit) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SurfaceVariantLight,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .size(width = 76.dp, height = 52.dp)
                            .testTag("pin_key_$digit"),
                    ) {
                        Text(
                            text = digit,
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextButton(
                onClick = onClear,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = StatusErrorBg,
                    contentColor = StatusErrorFg,
                ),
                modifier = Modifier
                    .size(width = 76.dp, height = 52.dp)
                    .testTag("pin_clear_button"),
            ) {
                Text(text = "Xóa", style = MaterialTheme.typography.labelLarge)
            }

            FilledTonalButton(
                onClick = { onDigitClick("0") },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = SurfaceVariantLight,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(width = 76.dp, height = 52.dp)
                    .testTag("pin_key_0"),
            ) {
                Text(
                    text = "0",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            FilledTonalButton(
                onClick = onBackspace,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = SurfaceVariantLight,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(width = 76.dp, height = 52.dp)
                    .testTag("pin_backspace_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = "Xóa một số",
                )
            }
        }
    }
}
