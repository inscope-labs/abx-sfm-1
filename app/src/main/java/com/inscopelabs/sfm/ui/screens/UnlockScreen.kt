package com.inscopelabs.sfm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.sfm.ui.VaultViewModel
import com.inscopelabs.sfm.ui.theme.VaultCyanSecondary
import com.inscopelabs.sfm.ui.theme.VaultDarkBackground
import com.inscopelabs.sfm.ui.theme.VaultDarkCardBorder
import com.inscopelabs.sfm.ui.theme.VaultDarkSurface
import com.inscopelabs.sfm.ui.theme.VaultDarkSurfaceVariant
import com.inscopelabs.sfm.ui.theme.VaultEmeraldPrimary
import com.inscopelabs.sfm.ui.theme.VaultError
import com.inscopelabs.sfm.ui.theme.VaultTextMuted
import com.inscopelabs.sfm.ui.theme.VaultTextPrimary
import com.inscopelabs.sfm.ui.theme.VaultTextSecondary

@Composable
fun UnlockScreen(viewModel: VaultViewModel) {
    val enteredPin by viewModel.enteredPin.collectAsState()
    val errorMessage by viewModel.pinErrorMessage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        VaultDarkBackground,
                        VaultDarkSurfaceVariant,
                        VaultDarkSurface
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Vault Shield Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                VaultEmeraldPrimary.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(2.dp, VaultEmeraldPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Vault Locked",
                    tint = VaultEmeraldPrimary,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "SECURE VAULT",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = VaultTextPrimary,
                letterSpacing = 2.sp
            )

            Text(
                text = "AES-256 Encrypted File Manager",
                fontSize = 13.sp,
                color = VaultTextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // PIN Dots Display
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                for (i in 0 until 4) {
                    val isFilled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) VaultEmeraldPrimary else VaultDarkSurface
                            )
                            .border(
                                2.dp,
                                if (isFilled) VaultEmeraldPrimary else VaultDarkCardBorder,
                                CircleShape
                            )
                    )
                }
            }

            // Error Message
            AnimatedVisibility(visible = errorMessage != null) {
                errorMessage?.let {
                    Text(
                        text = it,
                        color = VaultError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                            .testTag("pin_error_text")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Demo Hint Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = VaultDarkSurface.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultDarkCardBorder),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = VaultCyanSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Demo Vault Key",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultCyanSecondary
                        )
                        Text(
                            text = "Default PIN is 1234 or tap Biometrics below",
                            fontSize = 11.sp,
                            color = VaultTextMuted
                        )
                    }
                }
            }

            // Keypad Grid
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                keys.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        row.forEach { key ->
                            KeypadButton(
                                key = key,
                                onClick = {
                                    when (key) {
                                        "DEL" -> viewModel.removeLastPinDigit()
                                        "BIO" -> viewModel.quickBiometricUnlock()
                                        else -> viewModel.appendPinDigit(key)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Biometric Shortcut Button
            OutlinedButton(
                onClick = { viewModel.quickBiometricUnlock() },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = VaultCyanSecondary
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultCyanSecondary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .testTag("biometric_unlock_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Fingerprint",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Unlock with Biometrics")
            }
        }
    }
}

@Composable
fun KeypadButton(
    key: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = VaultDarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultDarkCardBorder),
        modifier = Modifier
            .size(68.dp)
            .testTag("keypad_btn_$key")
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (key) {
                "DEL" -> Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Delete",
                    tint = VaultTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                "BIO" -> Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric",
                    tint = VaultCyanSecondary,
                    modifier = Modifier.size(26.dp)
                )
                else -> Text(
                    text = key,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = VaultTextPrimary
                )
            }
        }
    }
}
