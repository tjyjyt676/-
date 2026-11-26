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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.LocalCrmColors

@Composable
fun ClientsScreen(
    state: CrmDatabaseState,
    onOpenClient: (String) -> Unit,
    onAddClient: (String, String, Int, LeadSource, Boolean, Int, String) -> Unit
) {
    val crmColors = LocalCrmColors.current
    val isSpecialist = state.settings.currentRole == UserRole.SPECIALIST
    var query by remember { mutableStateOf("") }
    var segmentFilter by remember { mutableStateOf<AgeSegment?>(null) }
    var onlyMissingPdn by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    val baseClients = remember(state.clients, isSpecialist, state.settings.activeSpecialistId) {
        if (isSpecialist) {
            state.clients.filter { it.assignedSpecialistId == state.settings.activeSpecialistId }
        } else {
            state.clients
        }
    }

    val filtered = remember(baseClients, query, segmentFilter, onlyMissingPdn) {
        baseClients.filter { cl ->
            val qOk = query.isBlank() ||
                cl.fullName.contains(query, ignoreCase = true) ||
                cl.phone.contains(query, ignoreCase = true)
            val sOk = segmentFilter == null || cl.segment == segmentFilter
            val pOk = !onlyMissingPdn || !cl.pdnConsentSigned
            qOk && sOk && pOk
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("clients_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitleBar(
                title = if (isSpecialist) "Мои клиенты 360°" else "База клиентов 360°",
                subtitle = if (isSpecialist) {
                    "Показаны только закреплённые за вами клиенты с экспресс-тестом и дневником сна"
                } else {
                    "3 целевых сегмента · Контроль согласия 152-ФЗ · Данные из приложения"
                },
                trailing = if (!isSpecialist) {
                    {
                        Button(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("add_client_button")
                        ) {
                            Icon(Icons.Filled.PersonAdd, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Клиент")
                        }
                    }
                } else null
            )
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Поиск по ФИО или телефону...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = segmentFilter == null && !onlyMissingPdn,
                    onClick = { segmentFilter = null; onlyMissingPdn = false },
                    label = { Text("Все (${baseClients.size})") }
                )
                AgeSegment.entries.forEach { seg ->
                    FilterChip(
                        selected = segmentFilter == seg,
                        onClick = { segmentFilter = if (segmentFilter == seg) null else seg },
                        label = { Text(seg.shortLabel) }
                    )
                }
                FilterChip(
                    selected = onlyMissingPdn,
                    onClick = { onlyMissingPdn = !onlyMissingPdn },
                    label = { Text("Без 152-ФЗ") }
                )
            }
        }

        items(filtered, key = { it.id }) { cl ->
            val recProg = state.programs.find { it.id == cl.recommendedProgramId }
            val specialist = state.specialists.find { it.id == cl.assignedSpecialistId }
            CrmCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("client_card_${cl.id}"),
                onClick = { onOpenClient(cl.id) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cl.fullName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${cl.phone} · ${cl.age} лет · ${cl.source.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        text = if (cl.pdnConsentSigned) "152-ФЗ ✓" else "Нет 152-ФЗ",
                        containerColor = if (cl.pdnConsentSigned) crmColors.successContainer else crmColors.dangerContainer,
                        contentColor = if (cl.pdnConsentSigned) crmColors.success else crmColors.danger
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusPill(
                        text = cl.segment.shortLabel,
                        containerColor = crmColors.infoContainer,
                        contentColor = crmColors.info
                    )
                    StatusPill(
                        text = "Тест: ${cl.expressTestScore}/100",
                        containerColor = crmColors.warningContainer,
                        contentColor = crmColors.warning
                    )
                    StatusPill(
                        text = "Сон: ${cl.sleepDiary.adherencePercent}%",
                        containerColor = crmColors.violetContainer,
                        contentColor = crmColors.violet
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Рекомендован тариф: ${recProg?.title ?: "—"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Ведущий: ${specialist?.fullName ?: "Не назначен"} · Заметок: ${cl.specialistNotes.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { LegalFooterBanner() }
    }

    if (showAddDialog) {
        AddClientDialog(
            specialists = state.specialists,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, age, src, pdn, score, spId ->
                onAddClient(name, phone, age, src, pdn, score, spId)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun Client360Dialog(
    clientId: String,
    state: CrmDatabaseState,
    onDismiss: () -> Unit,
    onTogglePdn: (String, Boolean) -> Unit,
    onAddNote: (String, String) -> Unit
) {
    val client = state.clients.find { it.id == clientId } ?: return
    val crmColors = LocalCrmColors.current
    val recProg = state.programs.find { it.id == client.recommendedProgramId }
    val clientDeals = state.deals.filter { it.clientId == clientId }
    val clientBookings = state.bookings.filter { it.clientId == clientId }
    val clientPayments = state.payments.filter { it.clientId == clientId }
    val clientVkOrders = state.vkOrders.filter { it.clientId == clientId }
    val clientTickets = state.tickets.filter { it.clientId == clientId }
    var newNoteText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Dialog Top Bar
                Surface(color = crmColors.deepHeader) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Карточка клиента 360°", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(client.fullName, style = MaterialTheme.typography.titleLarge)
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, "Закрыть")
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile & 152-FZ Block
                    item {
                        CrmCard {
                            Text("Профиль и согласие 152-ФЗ", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text("Телефон: ${client.phone}", style = MaterialTheme.typography.bodyMedium)
                            Text("Возраст: ${client.age} лет · Сегмент: ${client.segment.label}", style = MaterialTheme.typography.bodyMedium)
                            Text("Источник обращения: ${client.source.label} · В базе с ${client.createdAt}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (client.pdnConsentSigned) crmColors.successContainer else crmColors.dangerContainer
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (client.pdnConsentSigned) "Согласие на ПДн (152-ФЗ) подписано" else "Нет согласия на ПДн (152-ФЗ)!",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (client.pdnConsentSigned) crmColors.success else crmColors.danger
                                        )
                                        Text(
                                            text = if (client.pdnConsentSigned) "Дата подписания: ${client.pdnConsentDate}" else "Перевод сделки в оплату заблокирован до подписания",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Switch(
                                        checked = client.pdnConsentSigned,
                                        onCheckedChange = { onTogglePdn(client.id, it) },
                                        modifier = Modifier.testTag("pdn_consent_switch")
                                    )
                                }
                            }
                        }
                    }

                    // Express Test & Sleep Diary from Mobile App
                    item {
                        CrmCard {
                            Text("Данные из клиентского приложения", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Экспресс-тест истощения НС", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${client.expressTestScore} / 100 баллов",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { client.expressTestScore / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (client.expressTestScore >= 75) crmColors.danger else MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Авто-рекомендация тарифа: ${recProg?.title ?: "Онлайн-курс"} (${formatRub(recProg?.priceRub ?: 14000)})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = crmColors.subtleBorder.copy(alpha = 0.5f))
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Дневник гигиены сна (средний сон: ${client.sleepDiary.avgSleepHours} ч · выполнение ${client.sleepDiary.adherencePercent}%)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(6.dp))
                            val d = client.sleepDiary
                            DiaryBar("Отказ от экрана за 1 час до сна", d.noScreenBeforeBedDays, d.totalTrackedDays)
                            DiaryBar("Проветривание спальни", d.roomVentilationDays, d.totalTrackedDays)
                            DiaryBar("Единый режим подъёма", d.wakeUpScheduleDays, d.totalTrackedDays)
                        }
                    }

                    // Specialist Internal Notes after sessions
                    item {
                        CrmCard {
                            Text("Внутренние заметки специалистов после сессий", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newNoteText,
                                onValueChange = { newNoteText = it },
                                placeholder = { Text("Добавить заметку по динамике сна после сессии...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (newNoteText.isNotBlank()) {
                                        onAddNote(client.id, newNoteText)
                                        newNoteText = ""
                                    }
                                },
                                modifier = Modifier.testTag("save_specialist_note_btn")
                            ) {
                                Icon(Icons.Filled.NoteAdd, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Сохранить заметку")
                            }
                            Spacer(Modifier.height(8.dp))
                            if (client.specialistNotes.isEmpty()) {
                                Text("Пока нет заметок", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                client.specialistNotes.forEach { note ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column(Modifier.padding(10.dp)) {
                                            Text(
                                                "${note.specialistName} · ${note.date}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(note.text, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // History: Deals, Bookings, Payments, VK Orders, Chats
                    item {
                        CrmCard {
                            Text("История взаимодействий", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Сделки (${clientDeals.size}):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            clientDeals.forEach { dl ->
                                Text("• ${dl.stage.shortLabel} · ${formatRub(dl.amountRub)} (${dl.updatedAt})", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Записи в календаре (${clientBookings.size}):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            clientBookings.forEach { bk ->
                                Text("• ${bk.dateLabel} ${bk.timeSlot} — ${bk.status.label}", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("Оплаты (${clientPayments.size}):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            clientPayments.forEach { p ->
                                Text("• ${p.programTitle}: ${formatRub(p.paidAmountRub)} / ${formatRub(p.totalAmountRub)} (${p.status.label})", style = MaterialTheme.typography.bodySmall)
                            }
                            if (clientVkOrders.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text("Покупки товаров ВК (${clientVkOrders.size}):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                clientVkOrders.forEach { ord ->
                                    val pr = state.vkProducts.find { it.id == ord.productId }
                                    Text("• ${pr?.name ?: "Товар ВК"} × ${ord.quantity} — ${formatRub(ord.totalRub)} (${ord.status})", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            if (clientTickets.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                Text("Обращения в чат (${clientTickets.size}):", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                clientTickets.forEach { tk ->
                                    Text("• ${tk.subject} (${tk.status.label})", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryBar(label: String, done: Int, total: Int) {
    val frac = if (total > 0) (done.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text("$done / $total дн.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun AddClientDialog(
    specialists: List<Specialist>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Int, LeadSource, Boolean, Int, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+7 (902) ") }
    var ageStr by remember { mutableStateOf("45") }
    var scoreStr by remember { mutableStateOf("70") }
    var source by remember { mutableStateOf(LeadSource.MOBILE_APP) }
    var pdnSigned by remember { mutableStateOf(true) }
    var specialistId by remember { mutableStateOf(specialists.firstOrNull()?.id ?: "sp_1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый клиент") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("ФИО клиента") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Телефон") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = ageStr, onValueChange = { ageStr = it.filter(Char::isDigit) }, label = { Text("Возраст") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = scoreStr, onValueChange = { scoreStr = it.filter(Char::isDigit) }, label = { Text("Тест (0-100)") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LeadSource.entries.forEach { src ->
                        FilterChip(selected = source == src, onClick = { source = src }, label = { Text(src.label) })
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Согласие 152-ФЗ подписано")
                    Switch(checked = pdnSigned, onCheckedChange = { pdnSigned = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(
                            name,
                            phone,
                            ageStr.toIntOrNull() ?: 42,
                            source,
                            pdnSigned,
                            scoreStr.toIntOrNull() ?: 65,
                            specialistId
                        )
                    }
                }
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
