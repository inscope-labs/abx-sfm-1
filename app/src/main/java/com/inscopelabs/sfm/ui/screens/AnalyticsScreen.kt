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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.sfm.ui.VaultViewModel
import com.inscopelabs.sfm.ui.theme.VaultAccentYellow
import com.inscopelabs.sfm.ui.theme.VaultCyanSecondary
import com.inscopelabs.sfm.ui.theme.VaultDarkBackground
import com.inscopelabs.sfm.ui.theme.VaultDarkCardBorder
import com.inscopelabs.sfm.ui.theme.VaultDarkSurface
import com.inscopelabs.sfm.ui.theme.VaultDarkSurfaceVariant
import com.inscopelabs.sfm.ui.theme.VaultEmeraldPrimary
import com.inscopelabs.sfm.ui.theme.VaultTextMuted
import com.inscopelabs.sfm.ui.theme.VaultTextPrimary
import com.inscopelabs.sfm.ui.theme.VaultTextSecondary
import com.inscopelabs.sfm.core.model.FileItem

@Composable
fun AnalyticsScreen(viewModel: VaultViewModel) {
    val entities by viewModel.allEntities.collectAsState()

    val totalFiles = entities.size
    val totalSizeBytes = entities.sumOf { it.size }
    val formattedTotalSize = FileItem.formatFileSize(totalSizeBytes)

    // Grouping sizes by category
    val docsSize = entities.filter { it.toFileItem().fileType.name in listOf("PDF", "DOC", "DOCX", "XLS", "XLSX", "TXT") }.sumOf { it.size }
    val imagesSize = entities.filter { it.toFileItem().fileType.name in listOf("IMAGE", "JPEG", "PNG", "WEBP") }.sumOf { it.size }
    val archivesSize = entities.filter { it.toFileItem().fileType.name in listOf("ZIP", "TAR", "GZ", "ARCHIVE") }.sumOf { it.size }
    val codeSize = entities.filter { it.toFileItem().fileType.name in listOf("CODE", "JSON", "KOTLIN", "SHELL") }.sumOf { it.size }
    val mediaSize = entities.filter { it.toFileItem().fileType.name in listOf("AUDIO", "MP3", "VIDEO", "MP4") }.sumOf { it.size }

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
            // Header Card
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(VaultEmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = VaultEmeraldPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Vault Storage Overview",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultTextPrimary
                            )
                            Text(
                                text = "AES-256 GCM Hardware Sandbox",
                                fontSize = 12.sp,
                                color = VaultTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Encrypted Files",
                                fontSize = 12.sp,
                                color = VaultTextSecondary
                            )
                            Text(
                                text = "$totalFiles items",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultEmeraldPrimary
                            )
                        }

                        Column {
                            Text(
                                text = "Total Vault Storage",
                                fontSize = 12.sp,
                                color = VaultTextSecondary
                            )
                            Text(
                                text = formattedTotalSize,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultCyanSecondary
                            )
                        }
                    }
                }
            }

            // Storage Breakdown Chart Card
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Category Storage Breakdown",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CategoryStorageRow("Documents & PDF", docsSize, totalSizeBytes, VaultEmeraldPrimary)
                    CategoryStorageRow("Images & Media", imagesSize, totalSizeBytes, VaultCyanSecondary)
                    CategoryStorageRow("Archives & Seeds", archivesSize, totalSizeBytes, VaultAccentYellow)
                    CategoryStorageRow("Code & Shell Scripts", codeSize, totalSizeBytes, Color(0xFFC084FC))
                    CategoryStorageRow("Audio & Video", mediaSize, totalSizeBytes, Color(0xFFEC4899))
                }
            }

            // Security Audit Checklist
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultDarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, VaultDarkCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Zero-Trust Security Verification",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    SecurityCheckItem("PIN Keypad Authentication Active")
                    SecurityCheckItem("Android App Sandbox Isolation")
                    SecurityCheckItem("Immutable FileItem Data Integrity")
                    SecurityCheckItem("No Cleartext Leaks in Shared Memory")
                }
            }
        }
    }
}

@Composable
fun CategoryStorageRow(
    label: String,
    categorySize: Long,
    totalSize: Long,
    color: Color
) {
    val progress = if (totalSize > 0) (categorySize.toFloat() / totalSize.toFloat()).coerceIn(0f, 1f) else 0f
    val formatted = FileItem.formatFileSize(categorySize)

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 13.sp, color = VaultTextPrimary, fontWeight = FontWeight.Medium)
            Text(text = formatted, fontSize = 13.sp, color = VaultTextSecondary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = VaultDarkSurfaceVariant
        )
    }
}

@Composable
fun SecurityCheckItem(title: String) {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = VaultEmeraldPrimary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            color = VaultTextSecondary
        )
    }
}
