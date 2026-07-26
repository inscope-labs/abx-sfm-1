package com.inscopelabs.sfm

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.sfm.ui.VaultViewModel
import com.inscopelabs.sfm.ui.components.ChangePinDialog
import com.inscopelabs.sfm.ui.components.CreateNoteDialog
import com.inscopelabs.sfm.ui.components.FileDetailDialog
import com.inscopelabs.sfm.ui.components.FilePreviewDialog
import com.inscopelabs.sfm.ui.components.RenameFileDialog
import com.inscopelabs.sfm.ui.screens.AnalyticsScreen
import com.inscopelabs.sfm.ui.screens.FavoritesScreen
import com.inscopelabs.sfm.ui.screens.SecuritySettingsScreen
import com.inscopelabs.sfm.ui.screens.UnlockScreen
import com.inscopelabs.sfm.ui.screens.VaultExplorerScreen
import com.inscopelabs.sfm.ui.theme.SecureFilesTheme
import com.inscopelabs.sfm.ui.theme.VaultCyanSecondary
import com.inscopelabs.sfm.ui.theme.VaultDarkBackground
import com.inscopelabs.sfm.ui.theme.VaultDarkCardBorder
import com.inscopelabs.sfm.ui.theme.VaultDarkSurface
import com.inscopelabs.sfm.ui.theme.VaultDarkSurfaceVariant
import com.inscopelabs.sfm.ui.theme.VaultEmeraldPrimary
import com.inscopelabs.sfm.ui.theme.VaultTextMuted
import com.inscopelabs.sfm.ui.theme.VaultTextPrimary

class MainActivity : ComponentActivity() {

    private val viewModel: VaultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SecureFilesTheme {
                MainVaultApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVaultApp(viewModel: VaultViewModel) {
    val context = LocalContext.current
    val isUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // File picker launcher for importing documents/photos/files into vault
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                var fileName = "imported_vault_file"
                var fileSize = 0L
                context.contentResolver.query(selectedUri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }
                val mimeType = context.contentResolver.getType(selectedUri)
                viewModel.importFileFromUri(fileName, selectedUri, mimeType, fileSize)
            } catch (e: Exception) {
                Toast.makeText(context, "Error importing file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    if (!isUnlocked) {
        UnlockScreen(viewModel = viewModel)
    } else {
        var selectedTab by remember { mutableIntStateOf(0) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = when (selectedTab) {
                                0 -> "Vault Explorer"
                                1 -> "Vault Analytics"
                                2 -> "Starred Files"
                                else -> "Security Settings"
                            },
                            fontWeight = FontWeight.Bold,
                            color = VaultTextPrimary,
                            fontSize = 18.sp
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.lockVault() },
                            modifier = Modifier.testTag("topbar_lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = VaultEmeraldPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = VaultDarkSurfaceVariant,
                        titleContentColor = VaultTextPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = VaultDarkSurfaceVariant,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Explorer") },
                        label = { Text("Explorer") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VaultTextPrimary,
                            selectedTextColor = VaultTextPrimary,
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = VaultTextMuted,
                            unselectedTextColor = VaultTextMuted
                        ),
                        modifier = Modifier.testTag("nav_explorer")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                        label = { Text("Analytics") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VaultTextPrimary,
                            selectedTextColor = VaultTextPrimary,
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = VaultTextMuted,
                            unselectedTextColor = VaultTextMuted
                        ),
                        modifier = Modifier.testTag("nav_analytics")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Star, contentDescription = "Starred") },
                        label = { Text("Starred") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VaultTextPrimary,
                            selectedTextColor = VaultTextPrimary,
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = VaultTextMuted,
                            unselectedTextColor = VaultTextMuted
                        ),
                        modifier = Modifier.testTag("nav_starred")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Security, contentDescription = "Security") },
                        label = { Text("Security") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VaultTextPrimary,
                            selectedTextColor = VaultTextPrimary,
                            indicatorColor = Color(0xFFE8DEF8),
                            unselectedIconColor = VaultTextMuted,
                            unselectedTextColor = VaultTextMuted
                        ),
                        modifier = Modifier.testTag("nav_security")
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = VaultDarkBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> VaultExplorerScreen(
                        viewModel = viewModel,
                        onImportFileClick = { filePickerLauncher.launch(arrayOf("*/*")) }
                    )
                    1 -> AnalyticsScreen(viewModel = viewModel)
                    2 -> FavoritesScreen(viewModel = viewModel)
                    3 -> SecuritySettingsScreen(viewModel = viewModel)
                }
            }
        }

        // Active Dialogs
        val detailsFile by viewModel.selectedFileForDetails.collectAsState()
        detailsFile?.let { entity ->
            FileDetailDialog(
                entity = entity,
                onDismiss = { viewModel.selectedFileForDetails.value = null }
            )
        }

        val previewFile by viewModel.selectedFileForPreview.collectAsState()
        previewFile?.let { entity ->
            FilePreviewDialog(
                entity = entity,
                onDismiss = { viewModel.selectedFileForPreview.value = null }
            )
        }

        val showNoteDialog by viewModel.showImportNoteDialog.collectAsState()
        if (showNoteDialog) {
            CreateNoteDialog(
                onDismiss = { viewModel.showImportNoteDialog.value = false },
                onCreate = { title, content -> viewModel.createSecureNote(title, content) }
            )
        }

        val showChangePin by viewModel.showChangePinDialog.collectAsState()
        if (showChangePin) {
            ChangePinDialog(
                onDismiss = { viewModel.showChangePinDialog.value = false },
                onSubmit = { oldPin, newPin -> viewModel.updatePin(oldPin, newPin) }
            )
        }

        val renameFileEntity by viewModel.fileToRename.collectAsState()
        renameFileEntity?.let { entity ->
            RenameFileDialog(
                currentName = entity.name,
                onDismiss = { viewModel.fileToRename.value = null },
                onRename = { newName -> viewModel.renameFile(entity.id, newName) }
            )
        }
    }
}
