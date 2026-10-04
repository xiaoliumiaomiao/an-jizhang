package com.xiaoliumiaomiao.anjizhang.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.xiaoliumiaomiao.anjizhang.R
private val LedgerColors = lightColorScheme(primary = Color(0xFF176B5B), onPrimary = Color.White,
    background = Color(0xFFF7F9F8), surface = Color.White, onSurface = Color(0xFF182A25),
    onBackground = Color(0xFF182A25), error = Color(0xFFA74335), outlineVariant = Color(0xFFD9E4DF))
@Composable fun LedgerTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = LedgerColors.copy(primary = colorResource(R.color.ledger_primary), background = colorResource(R.color.ledger_background)), content = content) }
