package com.yeter.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Yatay kaydırılabilir para birimi seçici
@Composable
fun CurrencyPicker(selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CURRENCIES.forEach { c ->
            val isSel = c.code == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSel) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .selectable(selected = isSel, role = Role.RadioButton, onClick = { onSelect(c.code) })
                    .defaultMinSize(minHeight = 48.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${c.symbol.trim()} ${c.code}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    onFinish: (homeCurrency: String, daily: Double, goalName: String, goalPrice: Double, goalCurrency: String, recurring: Boolean) -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var home by remember { mutableStateOf("TRY") }
    var dailyText by remember { mutableStateOf("30") }
    var goalName by remember { mutableStateOf("Claude Pro (aylık)") }
    var goalPriceText by remember { mutableStateOf("20") }
    var goalCur by remember { mutableStateOf("USD") }
    var goalRecurring by remember { mutableStateOf(true) }
    var permissionAsked by remember { mutableStateOf(false) }

    fun finish() {
        onFinish(
            home,
            dailyText.replace(',', '.').toDoubleOrNull() ?: 30.0,
            goalName,
            goalPriceText.replace(',', '.').toDoubleOrNull() ?: 0.0,
            goalCur,
            goalRecurring
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionAsked = true
    }
    LaunchedEffect(permissionAsked) {
        if (permissionAsked) finish()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_y_mark),
                contentDescription = "Yeter Logo",
                modifier = Modifier.size(if (step == 0) 120.dp else 72.dp).clip(RoundedCornerShape(24.dp))
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (step) {
                0 -> {
                    Text("Yeter!", fontSize = 40.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Boşa harcama dürtüsüne dur de, parayı asıl hedefine biriktir.",
                        fontSize = 18.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OnboardingPoint("⏱️", "Canlı sayaç", "Harcamadığın her saniye birikime işler.")
                    OnboardingPoint("✋", "Dürtü anında DUR", "10 dakika bekle, vazgeçersen tutar birikime eklenir.")
                    OnboardingPoint("🎯", "Hedefler", "Claude Pro gibi aylık hedefler, canlı geri sayım ve 8 para birimi.")
                }
                1 -> {
                    Text("Para birimin ve günlük tasarruf", fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Ana para birimi", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(8.dp))
                    CurrencyPicker(selected = home, onSelect = { home = it })
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = dailyText, onValueChange = { dailyText = it },
                        label = { Text("Günde ne kadar tasarruf edebilirsin? (${currencySymbol(home).trim()})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Bu tutar her gün saniye saniye birikir. Her sabah o günün tutarını değiştirebilirsin.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    Text("İlk hedefin", fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = goalName, onValueChange = { goalName = it },
                        label = { Text("Hedef adı") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = goalPriceText, onValueChange = { v -> goalPriceText = if ((v.replace(',', '.').toDoubleOrNull() ?: 0.0) > Savings.MAX_GOAL) Savings.MAX_GOAL.toInt().toString() else v },
                        label = { Text("Fiyat ($goalCur)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CurrencyPicker(selected = goalCur, onSelect = { goalCur = it })
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Switch(checked = goalRecurring, onCheckedChange = { goalRecurring = it })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Her ay tekrar eder", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Hazır gelen örnek Claude Pro (aylık \$20). Değiştirebilir ya da boş bırakıp sonra Vitrin'den ekleyebilirsin. Günlük hatırlatmalar için bildirim izni istenecek.",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (step > 0) {
                    OutlinedButton(onClick = { step -= 1 }, modifier = Modifier.weight(1f).height(50.dp)) { Text("Geri") }
                }
                Button(
                    onClick = {
                        if (step < 2) step += 1
                        else if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else finish()
                    },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text(if (step == 0) "Başlayalım" else if (step == 1) "Devam" else "Bitir", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OnboardingPoint(icon: String, title: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 26.sp, modifier = Modifier.padding(end = 12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
