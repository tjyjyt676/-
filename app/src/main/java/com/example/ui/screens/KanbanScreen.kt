package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.LocalCrmColors

@Composable
fun KanbanScreen(
    state: CrmDatabaseState,
    onMoveDeal: (String, DealStage) -> Boolean,
    onCreateDeal: (clientId: String, programId: String, specialistId: String, tourbaseId: String?, isInstallment: Boolean, notes: String) -> Unit,
    onAttachTourbase: (dealId: String, tourbaseId: String) -> Unit,
    onOpenClient: (String) -> Unit
) {
    val crmColors = LocalCrmColors.current
    var searchQuery by remember { mutableStateOf("") }
    var segmentFilter by remember { mutableStateOf<AgeSegment?>(null) }
    var sourceFilter by remember { mutableStateOf<LeadSource?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var stagePickerDeal by remember { mutableStateOf<Deal?>(null) }
    var tourbasePickerDeal by remember { mutableStateOf<Deal?>(null) }

    val filteredDeals = remember(state.deals, state.clients, searchQuery, segmentFilter, sourceFilter) {
        state.deals.filter { deal ->
            val cl = state.clients.find { it.id == deal.clientId }
            val matchesQuery = searchQuery.isBlank() ||
                (cl?.fullName?.contains(searchQuery, ignoreCase = true) == true) ||
                deal.notes.contains(searchQuery, ignoreCase = true)
            val matchesSeg = segmentFilter == null || cl?.segment == segmentFilter
            val matchesSrc = sourceFilter == null || cl?.source == sourceFilter
            matchesQuery && matchesSeg && matchesSrc
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 12.dp)
            .testTag("kanban_screen")
    ) {
        // Header & Add Deal button
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SectionTitleBar(
                title = "Воронка продаж",
                subtitle = "8 этапов · Автосохранение · Защита 152-ФЗ перед оплатой",
                trailing = {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("kanban_add_deal_button")
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Сделка")
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск по ФИО клиента или заметке...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kanban_search_input")
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = segmentFilter == null && sourceFilter == null,
                    onClick = { segmentFilter = null; sourceFilter = null },
                    label = { Text("Все (${state.deals.size})") }
                )
                AgeSegment.entries.forEach { seg ->
                    FilterChip(
                        selected = segmentFilter == seg,
                        onClick = { segmentFilter = if (segmentFilter == seg) null else seg },
                        label = { Text(seg.shortLabel) }
                    )
                }
                LeadSource.entries.forEach { src ->
                    FilterChip(
                        selected = sourceFilter == src,
                        onClick = { sourceFilter = if (sourceFilter == src) null else src },
                        label = { Text(src.label) }
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Horizontally scrollable 8-column Kanban Board
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val allStages = DealStage.entries
            allStages.forEachIndexed { stageIdx, stage ->
                val stageDeals = filteredDeals.filter { it.stage == stage }
                val stageSum = stageDeals.sumOf { it.amountRub }
                val accent = when (stage) {
                    DealStage.NEW_LEAD -> crmColors.info
                    DealStage.TEST_PASSED -> crmColors.violet
                    DealStage.CONSULTATION -> MaterialTheme.colorScheme.primary
                    DealStage.AWAITING_PAYMENT -> crmColors.warning
                    DealStage.PAID_BOOKED, DealStage.IN_PROGRESS, DealStage.COMPLETED -> crmColors.success
                    DealStage.CANCELLED -> crmColors.danger
                }

                Surface(
                    modifier = Modifier
                        .width(292.dp)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, crmColors.subtleBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Column Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stage.shortLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = accent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill(
                                text = "${stageDeals.size}",
                                containerColor = accent.copy(alpha = 0.16f),
                                contentColor = accent
                            )
                        }
                        Text(
                            text = formatRub(stageSum),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))

                        if (stageDeals.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Нет сделок на этапе",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(stageDeals, key = { it.id }) { deal ->
                                    val client = state.clients.find { it.id == deal.clientId }
                                    val program = state.programs.find { it.id == deal.programId }
                                    val tourbase = state.tourbases.find { it.id == deal.tourbaseId }

                                    CrmCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("deal_card_${deal.id}"),
                                        onClick = { if (client != null) onOpenClient(client.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = client?.fullName ?: "Клиент",
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (client != null) {
                                                StatusPill(
                                                    text = if (client.pdnConsentSigned) "152-ФЗ ✓" else "Нет 152-ФЗ",
                                                    containerColor = if (client.pdnConsentSigned) crmColors.successContainer else crmColors.dangerContainer,
                                                    contentColor = if (client.pdnConsentSigned) crmColors.success else crmColors.danger
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = program?.title ?: "Программа",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = formatRub(deal.amountRub),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (deal.isInstallment) {
                                                StatusPill(
                                                    text = "Рассрочка 0%",
                                                    containerColor = crmColors.warningContainer,
                                                    contentColor = crmColors.warning
                                                )
                                            }
                                        }
                                        if (tourbase != null) {
                                            Spacer(Modifier.height(4.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.Forest,
                                                    null,
                                                    tint = crmColors.success,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = tourbase.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = crmColors.success,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (deal.notes.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = deal.notes,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        HorizontalDivider(color = crmColors.subtleBorder.copy(alpha = 0.5f))
                                        Spacer(Modifier.height(6.dp))
                                        // Quick Drag/Move Controls + Tourbase attach
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                IconButton(
                                                    onClick = {
                                                        if (stageIdx > 0) onMoveDeal(deal.id, allStages[stageIdx - 1])
                                                    },
                                                    enabled = stageIdx > 0,
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Предыдущий этап", Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = { stagePickerDeal = deal },
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.Filled.SwapHoriz, "Переместить на этап", Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = {
                                                        if (stageIdx < allStages.lastIndex) onMoveDeal(deal.id, allStages[stageIdx + 1])
                                                    },
                                                    enabled = stageIdx < allStages.lastIndex,
                                                    modifier = Modifier.size(34.dp)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Следующий этап", Modifier.size(16.dp))
                                                }
                                            }
                                            TextButton(
                                                onClick = { tourbasePickerDeal = deal },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Filled.Place, null, Modifier.size(14.dp))
                                                Spacer(Modifier.width(3.dp))
                                                Text(
                                                    text = if (tourbase == null) "Турбаза" else "Сменить",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Stage Picker Dialog
    stagePickerDeal?.let { deal ->
        AlertDialog(
            onDismissRequest = { stagePickerDeal = null },
            title = { Text("Переместить сделку на этап") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    DealStage.entries.forEach { st ->
                        val isCurrent = st == deal.stage
                        OutlinedButton(
                            onClick = {
                                onMoveDeal(deal.id, st)
                                stagePickerDeal = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (isCurrent) ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(st.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { stagePickerDeal = null }) { Text("Закрыть") }
            }
        )
    }

    // Attach Tourbase Dialog
    tourbasePickerDeal?.let { deal ->
        AlertDialog(
            onDismissRequest = { tourbasePickerDeal = null },
            title = { Text("Прикрепить турбазу Волгоградской обл.") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(state.tourbases) { tb ->
                        val full = tb.freeSeats <= 0
                        OutlinedButton(
                            onClick = {
                                onAttachTourbase(deal.id, tb.id)
                                tourbasePickerDeal = null
                            },
                            enabled = !full,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("${tb.name} (${tb.freeSeats}/${tb.capacity} мест)", fontWeight = FontWeight.SemiBold)
                                Text(tb.district, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { tourbasePickerDeal = null }) { Text("Отмена") }
            }
        )
    }

    // Create New Deal Dialog
    if (showCreateDialog) {
        CreateDealDialog(
            state = state,
            onDismiss = { showCreateDialog = false },
            onConfirm = { clId, progId, spId, tbId, inst, note ->
                onCreateDeal(clId, progId, spId, tbId, inst, note)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CreateDealDialog(
    state: CrmDatabaseState,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?, Boolean, String) -> Unit
) {
    val activePrograms = state.programs.filter { !it.archived }
    var selectedClientId by remember { mutableStateOf(state.clients.firstOrNull()?.id ?: "") }
    var selectedProgramId by remember { mutableStateOf(activePrograms.firstOrNull()?.id ?: "") }
    var selectedSpecialistId by remember { mutableStateOf(state.specialists.firstOrNull()?.id ?: "") }
    var selectedTourbaseId by remember { mutableStateOf<String?>(null) }
    var isInstallment by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    val selectedProgram = activePrograms.find { it.id == selectedProgramId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая сделка в воронке") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Клиент", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        state.clients.forEach { cl ->
                            FilterChip(
                                selected = cl.id == selectedClientId,
                                onClick = { selectedClientId = cl.id },
                                label = { Text(cl.fullName.substringBeforeLast(" ")) }
                            )
                        }
                    }
                }
                item {
                    Text("Программа", style = MaterialTheme.typography.labelMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        activePrograms.forEach { p ->
                            FilterChip(
                                selected = p.id == selectedProgramId,
                                onClick = { selectedProgramId = p.id },
                                label = { Text("${p.title} · ${formatRub(p.priceRub)}") }
                            )
                        }
                    }
                }
                if (selectedProgram?.format != ProgramFormat.ONLINE) {
                    item {
                        Text("Турбаза (необязательно)", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedTourbaseId == null,
                                onClick = { selectedTourbaseId = null },
                                label = { Text("Без турбазы") }
                            )
                            state.tourbases.filter { it.freeSeats > 0 }.forEach { tb ->
                                FilterChip(
                                    selected = selectedTourbaseId == tb.id,
                                    onClick = { selectedTourbaseId = tb.id },
                                    label = { Text(tb.name) }
                                )
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Оформить рассрочку 0%", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = isInstallment, onCheckedChange = { isInstallment = it })
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Комментарий по заявке") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedClientId.isNotBlank() && selectedProgramId.isNotBlank()) {
                        onConfirm(selectedClientId, selectedProgramId, selectedSpecialistId, selectedTourbaseId, isInstallment, notes)
                    }
                }
            ) { Text("Создать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
