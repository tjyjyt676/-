package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.LocalCrmColors

@Composable
fun CatalogVkScreen(
    state: CrmDatabaseState,
    onUpsertProgram: (String?, String, ProgramFormat, Int, String, Int, Boolean, String) -> Unit,
    onToggleArchiveProgram: (String) -> Unit,
    onAdjustStock: (String, Int) -> Unit,
    onCreateVkOrder: (String, String, Int) -> Unit
) {
    val context = LocalContext.current
    val crmColors = LocalCrmColors.current
    val isAdmin = state.settings.currentRole == UserRole.ADMIN
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Программы, 1 = Товары ВК
    var editingProgram by remember { mutableStateOf<Program?>(null) }
    var showCreateProgram by remember { mutableStateOf(false) }
    var orderProduct by remember { mutableStateOf<VkProduct?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("catalog_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitleBar(
                title = "Каталог программ и ВК",
                subtitle = "Тарифы центра «Кринж & Co» и товары сообщества ВК",
                trailing = if (isAdmin && activeTab == 0) {
                    {
                        Button(
                            onClick = { showCreateProgram = true },
                            modifier = Modifier.testTag("add_program_btn")
                        ) {
                            Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Курс")
                        }
                    }
                } else null
            )
        }

        item {
            TabRow(selectedTabIndex = activeTab) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("Программы (${state.programs.size})") }
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Товары ВК (${state.vkProducts.size})") }
                )
            }
        }

        if (activeTab == 0) {
            items(state.programs, key = { it.id }) { prog ->
                CrmCard(modifier = Modifier.fillMaxWidth().testTag("program_card_${prog.id}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prog.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${prog.format.label} · ${prog.durationDesc}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatRub(prog.priceRub),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(prog.description, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill("${prog.modulesCount} модулей", crmColors.infoContainer, crmColors.info)
                        if (prog.installmentAllowed) {
                            StatusPill("Рассрочка 0%", crmColors.warningContainer, crmColors.warning)
                        }
                        if (prog.archived) {
                            StatusPill("В архиве", crmColors.dangerContainer, crmColors.danger)
                        } else {
                            StatusPill("Активен", crmColors.successContainer, crmColors.success)
                        }
                    }
                    if (isAdmin) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editingProgram = prog }) {
                                Icon(Icons.Filled.Edit, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Редактировать")
                            }
                            TextButton(onClick = { onToggleArchiveProgram(prog.id) }) {
                                Text(if (prog.archived) "Вернуть из архива" else "Архивировать")
                            }
                        }
                    }
                }
            }
        } else {
            // VK Store Sync Banner
            item {
                CrmCard(containerColor = crmColors.infoContainer.copy(alpha = 0.4f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Синхронизация с сообществом ВК", style = MaterialTheme.typography.titleSmall)
                            Text(
                                DemoDataSeeder.VK_COMMUNITY_URL,
                                style = MaterialTheme.typography.bodySmall,
                                color = crmColors.info
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DemoDataSeeder.VK_COMMUNITY_URL)))
                                }
                            }
                        ) {
                            Icon(Icons.Filled.OpenInNew, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Открыть ВК")
                        }
                    }
                }
            }

            items(state.vkProducts, key = { it.id }) { prod ->
                CrmCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prod.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${prod.category} · Продано: ${prod.soldCount} шт.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            formatRub(prod.priceRub),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(
                                text = "Остаток: ${prod.stock} шт.",
                                containerColor = if (prod.stock < 10) crmColors.warningContainer else crmColors.successContainer,
                                contentColor = if (prod.stock < 10) crmColors.warning else crmColors.success
                            )
                            IconButton(onClick = { onAdjustStock(prod.id, -1) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Filled.RemoveCircleOutline, "Уменьшить остаток")
                            }
                            IconButton(onClick = { onAdjustStock(prod.id, 1) }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Filled.AddCircleOutline, "Увеличить остаток")
                            }
                        }
                        Button(
                            onClick = { orderProduct = prod },
                            enabled = prod.stock > 0
                        ) {
                            Text("Оформить заказ")
                        }
                    }
                }
            }

            item {
                CrmCard {
                    Text("Последние заказы товаров ВК (${state.vkOrders.size})", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    state.vkOrders.forEach { ord ->
                        val cl = state.clients.find { it.id == ord.clientId }
                        val pr = state.vkProducts.find { it.id == ord.productId }
                        Text(
                            "• ${ord.date} · ${cl?.fullName ?: "Клиент"} — ${pr?.name ?: "Товар"} ×${ord.quantity} (${formatRub(ord.totalRub)}) [${ord.status}]",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        item { LegalFooterBanner() }
    }

    if (showCreateProgram || editingProgram != null) {
        ProgramEditorDialog(
            initial = editingProgram,
            onDismiss = { showCreateProgram = false; editingProgram = null },
            onSave = { id, title, format, price, dur, mods, inst, desc ->
                onUpsertProgram(id, title, format, price, dur, mods, inst, desc)
                showCreateProgram = false
                editingProgram = null
            }
        )
    }

    orderProduct?.let { prod ->
        var clientId by remember { mutableStateOf(state.clients.firstOrNull()?.id ?: "") }
        AlertDialog(
            onDismissRequest = { orderProduct = null },
            title = { Text("Заказ ВК: ${prod.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Цена: ${formatRub(prod.priceRub)} · Остаток: ${prod.stock} шт.")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.clients.forEach { cl ->
                            FilterChip(selected = cl.id == clientId, onClick = { clientId = cl.id }, label = { Text(cl.fullName.substringBeforeLast(" ")) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onCreateVkOrder(clientId, prod.id, 1); orderProduct = null }) {
                    Text("Оформить 1 шт.")
                }
            },
            dismissButton = { TextButton(onClick = { orderProduct = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun ProgramEditorDialog(
    initial: Program?,
    onDismiss: () -> Unit,
    onSave: (String?, String, ProgramFormat, Int, String, Int, Boolean, String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var format by remember { mutableStateOf(initial?.format ?: ProgramFormat.ONLINE) }
    var priceStr by remember { mutableStateOf((initial?.priceRub ?: 18000).toString()) }
    var duration by remember { mutableStateOf(initial?.durationDesc ?: "6 модулей · онлайн") }
    var modulesStr by remember { mutableStateOf((initial?.modulesCount ?: 6).toString()) }
    var installment by remember { mutableStateOf(initial?.installmentAllowed ?: true) }
    var desc by remember { mutableStateOf(initial?.description ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новый курс / программа" else "Редактировать программу") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Название") }, singleLine = true)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ProgramFormat.entries.forEach { f ->
                        FilterChip(selected = format == f, onClick = { format = f }, label = { Text(f.label.take(18)) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = priceStr, onValueChange = { priceStr = it.filter(Char::isDigit) }, label = { Text("Цена ₽") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = modulesStr, onValueChange = { modulesStr = it.filter(Char::isDigit) }, label = { Text("Модулей") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Длительность / формат") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Описание") }, maxLines = 3)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Доступна рассрочка 0%")
                    Switch(checked = installment, onCheckedChange = { installment = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            initial?.id,
                            title,
                            format,
                            priceStr.toIntOrNull() ?: 14000,
                            duration,
                            modulesStr.toIntOrNull() ?: 6,
                            installment,
                            desc
                        )
                    }
                }
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
