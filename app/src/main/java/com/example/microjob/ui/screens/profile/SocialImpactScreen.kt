package com.example.microjob.ui.screens.profile

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.microjob.model.DonationRecord
import com.example.microjob.model.VoucherItem
import com.example.microjob.viewmodel.SocialImpactUiState

@Composable
fun SocialImpactScreen(
    uiState: SocialImpactUiState,
    onViewAllDonations: () -> Unit = {},
    onViewAllVouchers: () -> Unit = {},
    onRedeemVoucher: (VoucherItem) -> Unit = {}
) {
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Points display at top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .background(primary.copy(alpha = 0.1f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("\u2B50", fontSize = 12.sp)
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${uiState.userPoints} pts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = primary
                    )
                }
            }
        }

        // ===== Impact Summary Box =====
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Community Impact",
                    fontSize = 13.sp,
                    color = onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn(
                        value = "${uiState.peopleHelped}",
                        label = "People Helped",
                        color = primary
                    )
                    StatColumn(
                        value = uiState.totalDonated,
                        label = "Total Donated",
                        color = Color(0xFF10B981)
                    )
                }
            }
        }

        // ===== Donation History (Shows 2 items + View All) =====
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Donation History",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface
                    )
                    TextButton(
                        onClick = onViewAllDonations,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "View All",
                            fontSize = 12.sp,
                            color = primary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "View All",
                            modifier = Modifier.size(14.dp),
                            tint = primary
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                uiState.donationHistory.take(2).forEach { record ->
                    DonationHistoryItem(record = record)
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        // ===== Food Voucher Section =====
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Food Vouchers",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = onSurface
                    )
                    TextButton(
                        onClick = onViewAllVouchers,
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "View All",
                            fontSize = 12.sp,
                            color = primary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "View All",
                            modifier = Modifier.size(14.dp),
                            tint = primary
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                uiState.voucherList.forEach { voucher ->
                    VoucherItemCard(
                        voucher = voucher,
                        onRedeem = { onRedeemVoucher(voucher) }
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ---------------------- Reusable Components ----------------------

@Composable
fun StatColumn(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DonationHistoryItem(record: DonationRecord) {
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(surfaceVariant)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(record.organization, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = onSurface)
            Text(record.date, fontSize = 11.sp, color = onSurfaceVariant)
        }
        Text(
            text = "+ ${record.amount}",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF10B981)
        )
    }
}

@Composable
fun VoucherItemCard(voucher: VoucherItem, onRedeem: () -> Unit) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, outline.copy(alpha = 0.3f))
            .background(surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(voucher.brandColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = voucher.brand,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = voucher.brandColor
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(voucher.title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = onSurface)
            Text(voucher.validStores, fontSize = 11.sp, color = onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${voucher.pointsRequired} Points",
                fontSize = 12.sp,
                color = primary,
                fontWeight = FontWeight.Medium
            )
        }
        Button(
            onClick = onRedeem,
            modifier = Modifier.height(32.dp),
            shape = RoundedCornerShape(6.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primary),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text("Redeem", fontSize = 12.sp)
        }
    }
}
