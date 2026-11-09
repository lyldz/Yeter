package com.yeter.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// "Gece laciverti + nane" teması. Zemin rengi res/values/colors.xml (yeter_bg) ile aynı olmalı.
// Yüzeyler: derinlik gölgeyle değil, giderek açılan lacivert tonlarıyla verilir.
val YeterBackground = Color(0xFF0B1220)   // s0 zemin
val YeterSurface = Color(0xFF111A2E)      // s1
val YeterCard = Color(0xFF16213A)         // s2 kart
val YeterCardHigh = Color(0xFF1C2842)     // s3 öne çıkan yüzey
val YeterOutline = Color(0xFF7C8BA6)      // pasif metin / ikon (5.4:1)
val YeterOutlineSoft = Color(0xFF2A3858)  // ince çizgi
val YeterText = Color(0xFFE8EEF8)
val YeterTextSecondary = Color(0xFFA9B6CC)

val YeterMint = Color(0xFF34D6B4)         // Ana vurgu (birikim, olumlu)
val YeterBlue = Color(0xFF6FB1FF)         // Bilgi / ikincil vurgu
val YeterAmber = Color(0xFFFFB547)        // Uyarı
val YeterLime = Color(0xFFA6E36A)         // Hedefe ulaşıldı / pozitif
val YeterCoral = Color(0xFFFF6B6B)        // Dürtü uyarısı (DUR), tehlike

val YeterColorScheme = darkColorScheme(
    primary = YeterMint,
    onPrimary = YeterBackground,
    primaryContainer = Color(0xFF0F4A3E),
    onPrimaryContainer = Color(0xFFB4F5E3),
    secondary = YeterBlue,
    onSecondary = Color(0xFF002A55),
    secondaryContainer = Color(0xFF1C3358),
    onSecondaryContainer = Color(0xFFD3E4FF),
    tertiary = YeterLime,
    onTertiary = Color(0xFF1C3300),
    tertiaryContainer = Color(0xFF2B4A12),
    onTertiaryContainer = Color(0xFFD2F5A8),
    background = YeterBackground,
    onBackground = YeterText,
    surface = YeterSurface,
    onSurface = YeterText,
    surfaceVariant = YeterCard,
    onSurfaceVariant = YeterTextSecondary,
    outline = YeterOutline,
    outlineVariant = YeterOutlineSoft,
    error = YeterCoral,
    onError = YeterBackground,
    errorContainer = Color(0xFF5A1A1A),
    onErrorContainer = Color(0xFFFFDAD6)
)

// Tipografi: en küçük boyut 12 sp. Roller: display 56, headline 28/22, title 18/16, body 16/14, label 14/12.
val YeterTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Black),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
    labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
)

// Şekil: beş köşe yarıçapı
val YeterShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Boşluk ölçeği (4 tabanlı)
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val screenEdge: Dp = 20.dp
}

@Composable
fun YeterTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YeterColorScheme,
        typography = YeterTypography,
        shapes = YeterShapes,
        content = content
    )
}

// Tekrar eden kart: 20 dp köşe, yüzey tonuyla derinlik (gölge yok)
@Composable
fun YeterCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentPadding: Dp = Spacing.lg,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(contentPadding), content = content)
    }
}

// BÜYÜK HARFLİ küçük bölüm etiketi
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
    )
}
