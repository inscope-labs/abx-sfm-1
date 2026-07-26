package com.inscopelabs.sfm.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.sfm.data.VaultFileEntity
import com.inscopelabs.sfm.ui.VaultViewModel
import com.inscopelabs.sfm.ui.theme.VaultAccentYellow
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
import com.inscopelabs.sfm.core.model.FileItem
import com.inscopelabs.sfm.core.model.FileType
import com.inscopelabs.sfm.core.model.SortOption
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VaultExplorerScreen(
    viewModel: VaultViewModel,
    onImportFileClick: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsState()
    val sortOption by viewModel.currentSortOption.collectAsState()
    val files by viewModel.filteredFiles.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Search & Sort Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VaultDarkSurfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Vault Status Summary Card (High Density Design Theme)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD0BCFF)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Vault Status",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF21005D)
                            )
                            val totalSizeMB = (files.sumOf { it.size } / (1024f * 1024f))
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f MB / 50 GB", totalSizeMB),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF21005D)
                            )
                            Text(
                                text = "256-bit AES Hardware Sandbox Active",
                                fontSize = 11.sp,
                                color = Color(0xFF21005D).copy(alpha = 0.8f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF21005D).copy(alpha = 0.12f))
                                .border(3.dp, Color(0xFF21005D), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${files.size}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF21005D)
                            )
                        }
                    }
                }

                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search vault files...", color = VaultTextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = VaultTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = VaultTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VaultDarkSurface,
                        unfocusedContainerColor = VaultDarkSurface,
                        focusedBorderColor = VaultEmeraldPrimary,
                        unfocusedBorderColor = VaultDarkCardBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_field")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sort Dropdown Selector Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(VaultDarkSurface)
                            .clickable { showSortMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("sort_menu_trigger")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = VaultEmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = sortOption.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = VaultTextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier
                            .background(VaultDarkSurface)
                            .border(1.dp, VaultDarkCardBorder)
                    ) {
                        SortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option.displayName,
                                        color = if (option == sortOption) VaultEmeraldPrimary else VaultTextPrimary,
                                        fontWeight = if (option == sortOption) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    viewModel.currentSortOption.value = option
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    Text(
                        text = "${files.size} items",
                        fontSize = 12.sp,
                        color = VaultTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Category Chips
                val categories = listOf(
                    "ALL" to "All",
                    "DOCUMENTS" to "Documents",
                    "IMAGES" to "Images",
                    "AUDIO" to "Audio",
                    "VIDEO" to "Video",
                    "ARCHIVES" to "Archives",
                    "CODE" to "Code",
                    "EXECUTABLES" to "Executables"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(categories) { (code, label) ->
                        val isSelected = selectedCategory == code
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedCategoryFilter.value = code },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color(0xFF21005D) else VaultTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFE8DEF8),
                                containerColor = VaultDarkSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = VaultDarkCardBorder,
                                selectedBorderColor = VaultEmeraldPrimary
                            )
                        )
                    }
                }
            }

            // File List / Empty State
            if (files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Empty",
                            tint = VaultTextMuted,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Secure Files Found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultTextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try adjusting your search query" else "Import files or create notes in your encrypted vault",
                            fontSize = 13.sp,
                            color = VaultTextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(files, key = { it.id }) { entity ->
                        VaultFileCard(
                            entity = entity,
                            onToggleFavorite = { viewModel.toggleFavorite(entity.id) },
                            onShowDetails = { viewModel.selectedFileForDetails.value = entity },
                            onPreview = { viewModel.selectedFileForPreview.value = entity },
                            onRename = { viewModel.fileToRename.value = entity },
                            onDelete = { viewModel.deleteFile(entity.id) }
                        )
                    }
                }
            }
        }

        // Action FABs
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(
                onClick = { viewModel.showImportNoteDialog.value = true },
                containerColor = VaultDarkSurfaceVariant,
                contentColor = VaultCyanSecondary,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("add_note_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.NoteAdd,
                    contentDescription = "New Note",
                    modifier = Modifier.size(22.dp)
                )
            }

            FloatingActionButton(
                onClick = onImportFileClick,
                containerColor = VaultEmeraldPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("import_file_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Import File",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun VaultFileCard(
    entity: VaultFileEntity,
    onToggleFavorite: () -> Unit,
    onShowDetails: () -> Unit,
    onPreview: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val fileItem = entity.toFileItem()
    val fileType = fileItem.fileType
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = VaultDarkSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, VaultDarkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview() }
            .testTag("file_card_${entity.id}")
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // File Type Icon Box (High Density Theme Palette)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getFileTypeBackgroundColor(fileType)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getFileTypeIcon(fileType),
                    contentDescription = fileType.displayName,
                    tint = getFileTypeIconTint(fileType),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // File Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entity.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = VaultTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VaultDarkSurfaceVariant)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (fileItem.extension.isNotBlank()) ".${fileItem.extension.lowercase()}" else "FILE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = VaultCyanSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = fileItem.formattedSize,
                        fontSize = 12.sp,
                        color = VaultTextSecondary
                    )
                    Text(
                        text = "•",
                        fontSize = 12.sp,
                        color = VaultTextMuted
                    )
                    Text(
                        text = formatDate(entity.lastModified),
                        fontSize = 12.sp,
                        color = VaultTextMuted
                    )
                }
            }

            // Quick Actions
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (entity.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (entity.isFavorite) VaultEmeraldPrimary else VaultTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onShowDetails) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Details",
                    tint = VaultCyanSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = VaultTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier
                        .background(VaultDarkSurface)
                        .border(1.dp, VaultDarkCardBorder)
                ) {
                    DropdownMenuItem(
                        text = { Text("Preview / View Content", color = VaultTextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = VaultCyanSecondary)
                        },
                        onClick = {
                            showMenu = false
                            onPreview()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Rename", color = VaultTextPrimary) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = VaultEmeraldPrimary)
                        },
                        onClick = {
                            showMenu = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = VaultError) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = VaultError)
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

fun getFileTypeIcon(fileType: FileType): ImageVector {
    return when (fileType) {
        FileType.DIRECTORY -> Icons.Default.Folder
        FileType.PDF -> Icons.Default.PictureAsPdf
        FileType.DOC, FileType.DOCX, FileType.XLS, FileType.XLSX, FileType.PPT, FileType.PPTX, FileType.TXT, FileType.RTF -> Icons.Default.Description
        FileType.IMAGE, FileType.JPEG, FileType.PNG, FileType.GIF, FileType.WEBP, FileType.SVG -> Icons.Default.Image
        FileType.AUDIO, FileType.MP3, FileType.WAV, FileType.FLAC, FileType.OGG -> Icons.Default.AudioFile
        FileType.VIDEO, FileType.MP4, FileType.MKV, FileType.AVI, FileType.WEBM -> Icons.Default.VideoFile
        FileType.ARCHIVE, FileType.ZIP, FileType.TAR, FileType.GZ, FileType.RAR -> Icons.Default.FolderZip
        FileType.CODE, FileType.JSON, FileType.XML, FileType.HTML, FileType.CSS, FileType.JAVASCRIPT, FileType.KOTLIN, FileType.JAVA, FileType.PYTHON -> Icons.Default.Code
        FileType.APK -> Icons.Default.Android
        FileType.SHELL -> Icons.Default.Terminal
        FileType.UNKNOWN -> Icons.Default.InsertDriveFile
    }
}

fun getFileTypeBackgroundColor(fileType: FileType): Color {
    return when (fileType) {
        FileType.PDF -> Color(0xFFF9DEDC)
        FileType.IMAGE, FileType.JPEG, FileType.PNG, FileType.WEBP -> Color(0xFFE8DEF8)
        FileType.CODE, FileType.JSON, FileType.KOTLIN, FileType.SHELL, FileType.DOC, FileType.DOCX, FileType.TXT -> Color(0xFFD3E3FD)
        FileType.ARCHIVE, FileType.ZIP -> Color(0xFFE7E0EC)
        FileType.AUDIO, FileType.VIDEO -> Color(0xFFEADDFF)
        else -> Color(0xFFF3EDF7)
    }
}

fun getFileTypeIconTint(fileType: FileType): Color {
    return when (fileType) {
        FileType.PDF -> Color(0xFF410002)
        FileType.IMAGE, FileType.JPEG, FileType.PNG, FileType.WEBP -> Color(0xFF21005D)
        FileType.CODE, FileType.JSON, FileType.KOTLIN, FileType.SHELL, FileType.DOC, FileType.DOCX, FileType.TXT -> Color(0xFF041E49)
        FileType.ARCHIVE, FileType.ZIP -> Color(0xFF1D1B20)
        FileType.AUDIO, FileType.VIDEO -> Color(0xFF21005D)
        else -> Color(0xFF1D1B20)
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
