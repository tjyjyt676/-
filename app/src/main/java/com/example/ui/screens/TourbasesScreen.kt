package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.LocalCrmColors

@Composable
fun TourbasesScreen(
    state: CrmDatabaseState,
    onAttachTourbaseToDeal: (dealId: String, tourbaseId: String) -> Unit
) {
    val crmColors = LocalCrmColors.current
    var filterMode by remember { mutableStateOf("Все") }
    var attachTargetBase by remember { mutableStateOf<Tourbase?>(null) }

    val filteredBases = remember(state.tourbases, filterMode) {
        when (filterMode) {
            "Свободно" -> state.tourbases.filter { it.status == TourbaseStatus.AVAILABLE }
            "Частично" -> state.tourbases.filter { it.status == TourbaseStatus.PARTIAL }
            "Заполнено" -> state.tourbases.filter { it.status == TourbaseStatus.FULL }
            "VIP" -> state.tourbases.filter { it.supportsVip }
            else -> state.tourbases
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tourbases_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitleBar(
                title = "12 партнерских турбаз",
                subtitle = "Волгоградская область · 3-дневные групповые и VIP-интенсивы по восстановлению сна"
            )
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Все", "Свободно", "Частично", "Заполнено", "VIP").forEach { mode ->
                    FilterChip(
                        selected = filterMode == mode,
                        onClick = { filterMode = mode },
                        label = { Text(mode) }
                    )
                }
            }
        }

        items(filteredBases, key = { it.id }) { tb ->
            val (bg, fg) = when (tb.status) {
                TourbaseStatus.AVAILABLE -> crmColors.successContainer to crmColors.success
                TourbaseStatus.PARTIAL -> crmColors.warningContainer to crmColors.warning
                TourbaseStatus.FULL -> crmColors.dangerContainer to crmColors.danger
            }
            val linkedDeals = state.deals.filter { it.tourbaseId == tb.id && it.stage != DealStage.CANCELLED }

            CrmCard(modifier = Modifier.fillMaxWidth().testTag("tourbase_card_${tb.id}")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tb.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = tb.district,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(tb.status.label, bg, fg)
                }

                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (tb.supportsGroup) {
                        StatusPill("Групповой 28 000 ₽", crmColors.infoContainer, crmColors.info)
                    }
                    if (tb.supportsVip) {
                        StatusPill("VIP 48 000 ₽", crmColors.violetContainer, crmColors.violet)
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Занято мест: ${tb.bookedSeats} из ${tb.capacity}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "Свободно: ${tb.freeSeats}",
                        style = MaterialTheme.typography.labelMedium,
                        color = fg,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (tb.bookedSeats.toFloat() / tb.capacity.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = fg,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Контакт площадки: ${tb.contactName} · ${tb.contactPhone}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tb.availableDates.forEach { d ->
                        AssistChip(
                            onClick = { attachTargetBase = tb },
                            label = { Text(d, style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = { Icon(Icons.Filled.DateRange, null, Modifier.size(14.dp)) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Активных сделок: ${linkedDeals.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = { attachTargetBase = tb },
                        enabled = tb.freeSeats > 0,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Link, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Прикрепить к сделке")
                    }
                }
            }
        }

        item { LegalFooterBanner() }
    }

    attachTargetBase?.let { tb ->
        val activeDeals = state.deals.filter { it.stage != DealStage.CANCELLED && it.stage != DealStage.COMPLETED }
        AlertDialog(
            onDismissRequest = { attachTargetBase = null },
            title = { Text("Прикрепить ${tb.name}") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(activeDeals) { dl ->
                        val cl = state.clients.find { it.id == dl.clientId }
                        OutlinedButton(
                            onClick = {
                                onAttachTourbaseToDeal(dl.id, tb.id)
                                attachTargetBase = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(cl?.fullName ?: "Клиент", fontWeight = FontWeight.SemiBold)
                                Text("${dl.stage.shortLabel} · ${formatRub(dl.amountRub)}", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { attachTargetBase = null }) { Text("Закрыть") } }
        )
    }
}
