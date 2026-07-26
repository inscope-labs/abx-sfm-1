package com.inscopelabs.sfm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.inscopelabs.sfm.ui.VaultViewModel
import com.inscopelabs.sfm.ui.theme.VaultAccentYellow
import com.inscopelabs.sfm.ui.theme.VaultDarkBackground
import com.inscopelabs.sfm.ui.theme.VaultTextMuted
import com.inscopelabs.sfm.ui.theme.VaultTextPrimary

@Composable
fun FavoritesScreen(viewModel: VaultViewModel) {
    val entities by viewModel.allEntities.collectAsState()
    val favorites = entities.filter { it.isFavorite }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultDarkBackground)
    ) {
        if (favorites.isEmpty()) {
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
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = VaultTextMuted,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Starred Vault Items",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = VaultTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap the star icon on any file in the Explorer tab to quickly bookmark it here.",
                        fontSize = 13.sp,
                        color = VaultTextMuted
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Starred Vault Files (${favorites.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = VaultTextPrimary,
                    modifier = Modifier.padding(16.dp)
                )

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(favorites, key = { it.id }) { entity ->
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
    }
}
