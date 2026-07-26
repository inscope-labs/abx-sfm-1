package com.inscopelabs.sfm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.sfm.data.VaultFileEntity
import com.inscopelabs.sfm.ui.screens.formatDate
import com.inscopelabs.sfm.ui.theme.VaultCyanSecondary
import com.inscopelabs.sfm.ui.theme.VaultDarkCardBorder
import com.inscopelabs.sfm.ui.theme.VaultDarkSurface
import com.inscopelabs.sfm.ui.theme.VaultDarkSurfaceVariant
import com.inscopelabs.sfm.ui.theme.VaultEmeraldPrimary
import com.inscopelabs.sfm.ui.theme.VaultTextMuted
import com.inscopelabs.sfm.ui.theme.VaultTextPrimary
import com.inscopelabs.sfm.ui.theme.VaultTextSecondary

@Composable
fun FileDetailDialog(
    entity: VaultFileEntity,
    onDismiss: () -> Unit
) {
    val fileItem = entity.toFileItem()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultDarkSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = VaultCyanSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "File Metadata Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailRow("Name", fileItem.name)
                DetailRow("Formatted Size", fileItem.formattedSize)
                DetailRow("Raw Byte Size", "${fileItem.size} bytes")
                DetailRow("File Type Category", fileItem.fileType.displayName)
                DetailRow("Extension", if (fileItem.extension.isNotBlank()) fileItem.extension else "None")
                DetailRow("MIME Type", fileItem.mimeType ?: "Unknown")
                DetailRow("Path", fileItem.path)
                DetailRow("URI", fileItem.uri.toString())
                DetailRow("Last Modified", formatDate(fileItem.lastModified))
                DetailRow("Is Readable", fileItem.isReadable.toString())
                DetailRow("Is Writable", fileItem.isWritable.toString())
                DetailRow("Is Directory", fileItem.isDirectory.toString())
                if (!entity.notes.isNullOrEmpty()) {
                    DetailRow("Encrypted Notes", entity.notes ?: "")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = VaultCyanSecondary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.isBlank()

@Composable
fun DetailRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VaultDarkSurfaceVariant, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = VaultTextMuted, fontWeight = FontWeight.SemiBold)
        Text(
            text = value,
            fontSize = 13.sp,
            color = VaultTextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun FilePreviewDialog(
    entity: VaultFileEntity,
    onDismiss: () -> Unit
) {
    val fileItem = entity.toFileItem()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultDarkSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = null,
                    tint = VaultEmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = entity.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (!entity.textContent.isNullOrBlank()) {
                    Text(
                        text = "Encrypted Vault Content:",
                        fontSize = 12.sp,
                        color = VaultTextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Surface(
                        color = VaultDarkSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, VaultDarkCardBorder, RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = entity.textContent,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = VaultEmeraldPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VaultDarkSurfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "File Encrypted in Vault",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = VaultCyanSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Type: ${fileItem.fileType.displayName} (${fileItem.formattedSize})",
                                fontSize = 12.sp,
                                color = VaultTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "This file is safely stored with AES-256 GCM in your private sandbox.",
                                fontSize = 11.sp,
                                color = VaultTextMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = VaultEmeraldPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun CreateNoteDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultDarkSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Create Encrypted Note", color = VaultTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title", color = VaultTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary,
                        focusedBorderColor = VaultEmeraldPrimary,
                        unfocusedBorderColor = VaultDarkCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_title_input")
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Encrypted Content", color = VaultTextMuted) },
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary,
                        focusedBorderColor = VaultEmeraldPrimary,
                        unfocusedBorderColor = VaultDarkCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title, content)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VaultEmeraldPrimary),
                modifier = Modifier.testTag("save_note_button")
            ) {
                Text("Save to Vault", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextMuted)
            }
        }
    )
}

@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    onSubmit: (oldPin: String, newPin: String) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultDarkSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Change Vault PIN", color = VaultTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 4) oldPin = it },
                    label = { Text("Current PIN (e.g. 1234)", color = VaultTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary,
                        focusedBorderColor = VaultCyanSecondary,
                        unfocusedBorderColor = VaultDarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("old_pin_input")
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text("New 4-digit PIN", color = VaultTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary,
                        focusedBorderColor = VaultCyanSecondary,
                        unfocusedBorderColor = VaultDarkCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("new_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(oldPin, newPin)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = VaultCyanSecondary),
                modifier = Modifier.testTag("submit_change_pin_button")
            ) {
                Text("Update PIN", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextMuted)
            }
        }
    )
}

@Composable
fun RenameFileDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultDarkSurface,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text("Rename File", color = VaultTextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("File Name", color = VaultTextMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = VaultTextPrimary,
                    unfocusedTextColor = VaultTextPrimary,
                    focusedBorderColor = VaultEmeraldPrimary,
                    unfocusedBorderColor = VaultDarkCardBorder
                ),
                modifier = Modifier.fillMaxWidth().testTag("rename_input")
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onRename(newName)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VaultEmeraldPrimary),
                modifier = Modifier.testTag("confirm_rename_button")
            ) {
                Text("Rename", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextMuted)
            }
        }
    )
}
