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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.LocalCrmColors

@Composable
fun CalendarScreen(
    state: CrmDatabaseState,
    hoursUntil: (Booking) -> Double,
    onCreateBooking: (
        clientId: String,
        specialistId: String,
        programId: String,
        tourbaseId: String?,
        dateLabel: String,
        dayOfWeek: String,
        timeSlot: String,
        hoursFromNow: Int,
        notes: String
    ) -> String?,
    onCancelBooking: (bookingId: String, refundToDeposit: Boolean, forceLateCancel: Boolean) -> Boolean,
    onOpenClient: (String) -> Unit
) {
    val crmColors = LocalCrmColors.current
    val isSpecialist = state.settings.currentRole == UserRole.SPECIALIST
    var viewMode by remember { mutableStateOf("Неделя") } // "День" / "Неделя"
    var selectedDay by remember { mutableStateOf("20.11.2026") }
    var specialistFilter by remember { mutableStateOf<String?>(null) }
    var programFilter by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var cancelTarget by remember { mutableStateOf<Booking?>(null) }

    val availableDays = remember(state.bookings) {
        (listOf("20.11.2026", "21.11.2026", "22.11.2026", "23.11.2026", "24.11.2026") +
            state.bookings.map { it.dateLabel }).distinct()
    }

    val visibleBookings = remember(state.bookings, isSpecialist, state.settings.activeSpecialistId, viewMode, selectedDay, specialistFilter, programFilter) {
        state.bookings.filter { bk ->
            val roleOk = !isSpecialist || bk.specialistId == state.settings.activeSpecialistId
            val dayOk = viewMode == "Неделя" || bk.dateLabel == selectedDay
            val spOk = specialistFilter == null || bk.specialistId == specialistFilter
            val prOk = programFilter == null || bk.programId == programFilter
            roleOk && dayOk && spOk && prOk
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitleBar(
                title = if (isSpecialist) "Мой календарь сессий" else "Единый календарь записей",
                subtitle = "Правило 24 часов на бронь и бесплатную отмену · Защита от накладок",
                trailing = {
                    Button(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("new_booking_button")
                    ) {
                        Icon(Icons.Filled.EventAvailable, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Запись")
                    }
                }
            )
        }

        // 24h business rule callout
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = crmColors.warningContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.Schedule, null, tint = crmColors.warning, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Бизнес-правило: запись и бесплатная отмена возможны не позднее чем за 24 часа до начала. Система автоматически блокирует накладки у специалистов.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // Day / Week switcher & Filters
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("День", "Неделя").forEach { mode ->
                        FilterChip(
                            selected = viewMode == mode,
                            onClick = { viewMode = mode },
                            label = { Text(mode) }
                        )
                    }
                }
                if (viewMode == "День") {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableDays.forEach { day ->
                            FilterChip(
                                selected = selectedDay == day,
                                onClick = { selectedDay = day },
                                label = { Text(day.take(5)) }
                            )
                        }
                    }
                }
                if (!isSpecialist) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = specialistFilter == null,
                            onClick = { specialistFilter = null },
                            label = { Text("Все специалисты") }
                        )
                        state.specialists.forEach { sp ->
                            FilterChip(
                                selected = specialistFilter == sp.id,
                                onClick = { specialistFilter = if (specialistFilter == sp.id) null else sp.id },
                                label = { Text(sp.fullName.substringBeforeLast(" ")) }
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = programFilter == null,
                        onClick = { programFilter = null },
                        label = { Text("Все программы") }
                    )
                    state.programs.filter { !it.archived }.forEach { pr ->
                        FilterChip(
                            selected = programFilter == pr.id,
                            onClick = { programFilter = if (programFilter == pr.id) null else pr.id },
                            label = { Text(pr.title.take(22)) }
                        )
                    }
                }
            }
        }

        if (visibleBookings.isEmpty()) {
            item {
                CrmCard {
                    Text(
                        "На выбранный период записей нет",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(visibleBookings, key = { it.id }) { bk ->
                val client = state.clients.find { it.id == bk.clientId }
                val sp = state.specialists.find { it.id == bk.specialistId }
                val prog = state.programs.find { it.id == bk.programId }
                val tb = state.tourbases.find { it.id == bk.tourbaseId }
                val hrs = hoursUntil(bk)
                val within24h = hrs < 24.0 && bk.status == BookingStatus.CONFIRMED

                CrmCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("booking_card_${bk.id}"),
                    borderColor = if (within24h) crmColors.warning else crmColors.subtleBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusPill(
                                text = "${bk.dayOfWeek}, ${bk.dateLabel} · ${bk.timeSlot}",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                icon = Icons.Filled.Schedule
                            )
                            if (within24h) {
                                StatusPill(
                                    text = "< 24ч (${hrs.toInt().coerceAtLeast(1)}ч)",
                                    containerColor = crmColors.dangerContainer,
                                    contentColor = crmColors.danger,
                                    icon = Icons.Filled.Warning
                                )
                            }
                        }
                        val (stBg, stFg) = when (bk.status) {
                            BookingStatus.CONFIRMED -> crmColors.successContainer to crmColors.success
                            BookingStatus.COMPLETED -> crmColors.infoContainer to crmColors.info
                            BookingStatus.CANCELLED -> crmColors.dangerContainer to crmColors.danger
                        }
                        StatusPill(bk.status.label, stBg, stFg)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = client?.fullName ?: "Клиент",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${prog?.title ?: "Программа"} · Ведущий: ${sp?.fullName ?: "—"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (tb != null) {
                        Text(
                            text = "Турбаза: ${tb.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = crmColors.success
                        )
                    }
                    if (bk.notes.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(bk.notes, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { if (client != null) onOpenClient(client.id) }) {
                            Text("Карточка 360°")
                        }
                        if (bk.status == BookingStatus.CONFIRMED) {
                            OutlinedButton(
                                onClick = { cancelTarget = bk },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = crmColors.danger)
                            ) {
                                Icon(Icons.Filled.EventBusy, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Отменить / Перенос")
                            }
                        }
                    }
                }
            }
        }

        item { LegalFooterBanner() }
    }

    if (showCreateDialog) {
        NewBookingDialog(
            state = state,
            onDismiss = { showCreateDialog = false },
            onConfirm = { clId, spId, prId, tbId, dateLbl, dow, slot, hrs, note ->
                val error = onCreateBooking(clId, spId, prId, tbId, dateLbl, dow, slot, hrs, note)
                if (error == null) showCreateDialog = false
            }
        )
    }

    cancelTarget?.let { bk ->
        val hrs = hoursUntil(bk)
        val lessThan24h = hrs < 24.0
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = {
                Text(if (lessThan24h) "Предупреждение: до сессии менее 24ч!" else "Отмена записи")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (lessThan24h) {
                        Surface(color = crmColors.dangerContainer, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                text = "Осталось ~${hrs.toInt().coerceAtLeast(1)} ч. По правилам центра бесплатная отмена возможна не позднее чем за 24 часа. При подтверждении можно перенести оплату на депозит или оформить возврат.",
                                style = MaterialTheme.typography.bodySmall,
                                color = crmColors.danger,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "До сессии более 24 часов — доступна бесплатная отмена. Слот специалиста и место на турбазе будут освобождены мгновенно.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            onCancelBooking(bk.id, true, true)
                            cancelTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Отменить → на депозит")
                    }
                    OutlinedButton(
                        onClick = {
                            onCancelBooking(bk.id, false, true)
                            cancelTarget = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Отменить → возврат средств")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) { Text("Оставить запись") }
            }
        )
    }
}

@Composable
private fun NewBookingDialog(
    state: CrmDatabaseState,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?, String, String, String, Int, String) -> Unit
) {
    var clientId by remember { mutableStateOf(state.clients.firstOrNull()?.id ?: "") }
    var specialistId by remember { mutableStateOf(state.specialists.firstOrNull()?.id ?: "") }
    var programId by remember { mutableStateOf(state.programs.firstOrNull()?.id ?: "") }
    val dayOptions = listOf(
        Triple("20.11.2026", "Пт", 12),  // < 24h to demonstrate rule check!
        Triple("21.11.2026", "Сб", 42),
        Triple("22.11.2026", "Вс", 66),
        Triple("23.11.2026", "Пн", 90),
        Triple("24.11.2026", "Вт", 114)
    )
    var selectedDayIdx by remember { mutableIntStateOf(1) }
    val slots = listOf("10:00", "11:00", "14:00", "16:00", "18:00")
    var selectedSlot by remember { mutableStateOf("14:00") }
    var notes by remember { mutableStateOf("") }

    val chosenDay = dayOptions[selectedDayIdx]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая запись в календарь") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Клиент", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.clients.forEach { cl ->
                            FilterChip(selected = cl.id == clientId, onClick = { clientId = cl.id }, label = { Text(cl.fullName.substringBeforeLast(" ")) })
                        }
                    }
                }
                item {
                    Text("Специалист", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.specialists.forEach { sp ->
                            FilterChip(selected = sp.id == specialistId, onClick = { specialistId = sp.id }, label = { Text(sp.fullName.substringBeforeLast(" ")) })
                        }
                    }
                }
                item {
                    Text("Дата (20.11 — тест правила <24ч!)", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayOptions.forEachIndexed { idx, triple ->
                            FilterChip(
                                selected = selectedDayIdx == idx,
                                onClick = { selectedDayIdx = idx },
                                label = { Text("${triple.second} ${triple.first.take(5)}") }
                            )
                        }
                    }
                }
                item {
                    Text("Время слота", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        slots.forEach { sl ->
                            FilterChip(selected = selectedSlot == sl, onClick = { selectedSlot = sl }, label = { Text(sl) })
                        }
                    }
                }
                item {
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Заметка к сессии") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(clientId, specialistId, programId, null, chosenDay.first, chosenDay.second, selectedSlot, chosenDay.third, notes)
                }
            ) { Text("Забронировать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
