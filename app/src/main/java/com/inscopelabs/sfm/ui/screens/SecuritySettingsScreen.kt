package com.inscopelabs.sfm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.sfm.ui.VaultViewModel
import com.inscopelabs.sfm.ui.theme.VaultCyanSecondary
import com.inscopelabs.sfm.ui.theme.VaultDarkBackground
import com.inscopelabs.sfm.ui.theme.VaultDarkCardBorder
import com.inscopelabs.sfm.ui.theme.VaultDarkSurface
import com.inscopelabs.sfm.ui.theme.VaultEmeraldPrimary
import com.inscopelabs.sfm.ui.theme.VaultError
import com.inscopelabs.sfm.ui.theme.VaultTextMuted
import com.inscopelabs.sfm.ui.theme.VaultTextPrimary
import com.inscopelabs.sfm.ui.theme.VaultTextSecondary

@Composable
fun SecuritySettingsScreen(viewModel: VaultViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBackground)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Header
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(VaultEmeraldPrimary.copy(alpha = 0.15f))
                            .border(1.dp, VaultEmeraldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = VaultEmeraldPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Vault Protection Active",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultTextPrimary
                        )
                        Text(
                            text = "Internal sandbox storage & PIN protection",
                            fontSize = 12.sp,
                            color = VaultTextMuted
                        )
                    }
                }
            }

            // PIN & Passcode Settings
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Password,
                            contentDescription = null,
                            tint = VaultCyanSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PIN & Keypad Configuration",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Current Key: 4-digit PIN lock configured",
                        fontSize = 13.sp,
                        color = VaultTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { viewModel.showChangePinDialog.value = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultCyanSecondary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VaultCyanSecondary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("change_pin_button")
                    ) {
                        Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Change Vault PIN")
                    }
                }
            }

            // Security Logs
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = VaultEmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Vault Audit Log",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Last Successful Unlock:", fontSize = 13.sp, color = VaultTextSecondary)
                        Text(text = viewModel.getFormattedLastUnlockTime(), fontSize = 13.sp, color = VaultEmeraldPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Failed Attempts:", fontSize = 13.sp, color = VaultTextSecondary)
                        Text(text = "${viewModel.getFailedAttempts()}", fontSize = 13.sp, color = if (viewModel.getFailedAttempts() > 0) VaultError else VaultTextMuted, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Emergency Lock
            Button(
                onClick = { viewModel.lockVault() },
                colors = ButtonDefaults.buttonColors(containerColor = VaultError, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("emergency_lock_button")
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = "Lock")
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Lock Vault Immediately",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}
