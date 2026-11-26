package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
fun TeamScreen(
    state: CrmDatabaseState,
    onAddSpecialist: (String, String, Boolean, String, String, List<String>) -> Unit
) {
    val crmColors = LocalCrmColors.current
    val isAdmin = state.settings.currentRole == UserRole.ADMIN
    var showAddDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("team_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitleBar(
                title = "Команда специалистов",
                subtitle = "Ведущие сессий и эксперты с медицинским образованием",
                trailing = if (isAdmin) {
                    {
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("add_specialist_btn")
                        ) {
                            Icon(Icons.Filled.PersonAdd, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Сотрудник")
                        }
                    }
                } else null
            )
        }

        items(state.specialists, key = { it.id }) { sp ->
            val assignedProgs = state.programs.filter { it.id in sp.assignedProgramIds }
            val activeBookings = state.bookings.count { it.specialistId == sp.id && it.status == BookingStatus.CONFIRMED }

            CrmCard(modifier = Modifier.fillMaxWidth().testTag("specialist_card_${sp.id}")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sp.fullName, style = MaterialTheme.typography.titleMedium)
                        Text(sp.specialization, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusPill(
                        text = "${"%.2f".format(sp.rating)} ★",
                        containerColor = crmColors.warningContainer,
                        contentColor = crmColors.warning
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (sp.hasMedicalEducation) {
                        StatusPill("Медобразование ✓", crmColors.successContainer, crmColors.success, icon = Icons.Filled.MedicalServices)
                    } else {
                        StatusPill("Коуч гигиены сна", crmColors.infoContainer, crmColors.info)
                    }
                    StatusPill("${sp.completedSessions} сессий", crmColors.violetContainer, crmColors.violet)
                    StatusPill("$activeBookings в графике", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Text("График: ${sp.workSchedule}", style = MaterialTheme.typography.bodySmall)
                Text("Телефон: ${sp.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Программы: ${assignedProgs.joinToString { it.title }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item { LegalFooterBanner() }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var spec by remember { mutableStateOf("") }
        var hasMed by remember { mutableStateOf(true) }
        var schedule by remember { mutableStateOf("Пн–Пт 10:00–19:00") }
        var phone by remember { mutableStateOf("+7 (903) ") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Добавить специалиста") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ФИО") }, singleLine = true)
                    OutlinedTextField(value = spec, onValueChange = { spec = it }, label = { Text("Специализация") }, singleLine = true)
                    OutlinedTextField(value = schedule, onValueChange = { schedule = it }, label = { Text("График работы") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Телефон") }, singleLine = true)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Наличие медобразования")
                        Switch(checked = hasMed, onCheckedChange = { hasMed = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddSpecialist(
                                name,
                                spec.ifBlank { "Специалист по восстановлению сна" },
                                hasMed,
                                schedule,
                                phone,
                                if (hasMed) listOf("prog_vip", "prog_group", "prog_online") else listOf("prog_group", "prog_online")
                            )
                            showAddDialog = false
                        }
                    }
                ) { Text("Добавить") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Отмена") } }
        )
    }
}

@Composable
fun SupportChatScreen(
    state: CrmDatabaseState,
    onSendReply: (ticketId: String, text: String) -> Unit,
    onUpdateStatus: (ticketId: String, status: TicketStatus) -> Unit,
    onOpenClient: (String) -> Unit
) {
    val crmColors = LocalCrmColors.current
    var statusFilter by remember { mutableStateOf<TicketStatus?>(null) }
    var selectedTicketId by remember { mutableStateOf(state.tickets.firstOrNull()?.id) }
    var draftText by remember { mutableStateOf("") }

    val quickTemplates = listOf(
        "Здравствуйте! Отправляем расписание трансфера из центра Волгограда — сбор в 09:00.",
        "Бесплатный перенос сессии возможен не позднее чем за 24 часа. Подберём удобный слот!",
        "Рассрочка 0% от центра оформляется без переплат и штрафов. Подтверждаем перенос взноса.",
        "Доступ к модулям курса в приложении обновлён — пожалуйста, перезапустите приложение."
    )

    val filteredTickets = remember(state.tickets, statusFilter) {
        if (statusFilter == null) state.tickets else state.tickets.filter { it.status == statusFilter }
    }
    val activeTicket = state.tickets.find { it.id == selectedTicketId } ?: filteredTickets.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("chat_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitleBar(
                title = "Единое окно техподдержки",
                subtitle = "Входящие диалоги из клиентского приложения · Шаблоны ответов"
            )
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("Все (${state.tickets.size})") }
                )
                TicketStatus.entries.forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = if (statusFilter == st) null else st },
                        label = { Text("${st.label} (${state.tickets.count { it.status == st }})") }
                    )
                }
            }
        }

        // Ticket selector cards
        items(filteredTickets, key = { it.id }) { tk ->
            val cl = state.clients.find { it.id == tk.clientId }
            val selected = tk.id == activeTicket?.id
            val (bg, fg) = when (tk.status) {
                TicketStatus.NEW -> crmColors.dangerContainer to crmColors.danger
                TicketStatus.IN_WORK -> crmColors.warningContainer to crmColors.warning
                TicketStatus.CLOSED -> crmColors.successContainer to crmColors.success
            }
            CrmCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { selectedTicketId = tk.id },
                borderColor = if (selected) MaterialTheme.colorScheme.primary else crmColors.subtleBorder
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cl?.fullName ?: "Клиент", style = MaterialTheme.typography.titleSmall)
                        Text(tk.subject, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    StatusPill(tk.status.label, bg, fg)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    tk.messages.lastOrNull()?.text ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // Active Thread Panel
        if (activeTicket != null) {
            val client = state.clients.find { it.id == activeTicket.clientId }
            item {
                CrmCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Диалог: ${client?.fullName ?: "Клиент"}", style = MaterialTheme.typography.titleMedium)
                            Text(activeTicket.subject, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { if (client != null) onOpenClient(client.id) }) {
                            Text("Карточка 360°")
                        }
                    }

                    // Status switcher
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TicketStatus.entries.forEach { st ->
                            FilterChip(
                                selected = activeTicket.status == st,
                                onClick = { onUpdateStatus(activeTicket.id, st) },
                                label = { Text(st.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeTicket.messages.forEach { msg ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (msg.fromClient) Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (msg.fromClient) {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    },
                                    modifier = Modifier.widthIn(max = 300.dp)
                                ) {
                                    Column(Modifier.padding(10.dp)) {
                                        Text(msg.text, style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "${if (msg.fromClient) "Клиент" else "Поддержка"} · ${msg.timestamp}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Быстрые шаблоны:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickTemplates.forEachIndexed { idx, tpl ->
                            AssistChip(
                                onClick = { draftText = tpl },
                                label = { Text("Шаблон ${idx + 1}: ${tpl.take(22)}…", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = draftText,
                            onValueChange = { draftText = it },
                            placeholder = { Text("Ответить клиенту...") },
                            modifier = Modifier.weight(1f),
                            maxLines = 3
                        )
                        FilledIconButton(
                            onClick = {
                                if (draftText.isNotBlank()) {
                                    onSendReply(activeTicket.id, draftText)
                                    draftText = ""
                                }
                            },
                            modifier = Modifier.testTag("chat_send_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, "Отправить")
                        }
                    }
                }
            }
        }

        item { LegalFooterBanner() }
    }
}
