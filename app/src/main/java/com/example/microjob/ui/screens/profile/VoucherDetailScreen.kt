package com.example.microjob.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.microjob.model.VoucherItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherDetailScreen(
    voucher: VoucherItem,
    userPoints: Int,
    onBack: () -> Unit,
    onRedeem: (VoucherItem) -> Unit
) {
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val canAfford = userPoints >= voucher.pointsRequired

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(voucher.brand, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(background)
                .verticalScroll(rememberScrollState())
        ) {
            // Brand banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(voucher.brandColor),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = voucher.brand,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = voucher.value,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            // Voucher info
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title and description
                Text(
                    text = voucher.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                Text(
                    text = voucher.description,
                    fontSize = 14.sp,
                    color = onSurfaceVariant,
                    lineHeight = 20.sp
                )

                // Points required
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = primary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Points Required", fontSize = 14.sp, color = onSurfaceVariant)
                        Text(
                            text = "${voucher.pointsRequired} pts",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = primary
                        )
                    }
                }

                // Rules section
                Text(
                    text = "Rules",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface
                )
                voucher.rules.forEach { rule ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = rule,
                            fontSize = 13.sp,
                            color = onSurfaceVariant
                        )
                    }
                }

                // Valid at
                Text(
                    text = "Valid at",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = voucher.validStores,
                    fontSize = 13.sp,
                    color = onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                // Redeem button
                Button(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = voucher.brandColor,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    ),
                    enabled = canAfford
                ) {
                    Text(
                        text = if (canAfford) "Redeem" else "Not enough points (${userPoints}/${voucher.pointsRequired})",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                if (!canAfford) {
                    Text(
                        text = "You need ${voucher.pointsRequired - userPoints} more points to redeem this voucher.",
                        fontSize = 12.sp,
                        color = onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Redemption") },
            text = {
                Column {
                    Text("Are you sure you want to redeem this voucher?")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${voucher.title}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${voucher.pointsRequired} points will be deducted",
                        fontSize = 13.sp,
                        color = onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        showSuccessDialog = true
                        onRedeem(voucher)
                    }
                ) {
                    Text("Confirm", color = Color(0xFF10B981))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Redeemed!") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("\u2705", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "You have successfully redeemed ${voucher.title}",
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}
