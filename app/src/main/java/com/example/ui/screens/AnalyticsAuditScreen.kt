package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.LocalCrmColors
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    state: CrmDatabaseState,
    buildClientsCsv: () -> String,
    buildFinanceCsv: () -> String
) {
    val context = LocalContext.current
    val crmColors = LocalCrmColors.current
    var period by remember { mutableStateOf(DashboardPeriod.MONTH) }
    var csvPreviewTitle by remember { mutableStateOf<String?>(null) }
    var csvPreviewContent by remember { mutableStateOf("") }

    val mult = period.multiplier

    fun shareCsv(title: String, csv: String) {
        csvPreviewTitle = title
        csvPreviewContent = csv
        runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "$title — Кринж & Co CRM")
                putExtra(Intent.EXTRA_TEXT, csv)
            }
            context.startActivity(Intent.createChooser(sendIntent, "Экспорт CSV: $title"))
        }
    }

    val sourceCounts = LeadSource.entries.map { src ->
        src.label to ((state.clients.count { it.source == src } * mult).roundToInt().coerceAtLeast(1))
    }

    val popularHours = listOf(
        "10:00" to (14 * mult).roundToInt().coerceAtLeast(1),
        "11:00" to (22 * mult).roundToInt().coerceAtLeast(1),
        "14:00" to (19 * mult).roundToInt().coerceAtLeast(1),
        "16:00" to (28 * mult).roundToInt().coerceAtLeast(1),
        "18:00" to (34 * mult).roundToInt().coerceAtLeast(1),
        "19:30" to (25 * mult).roundToInt().coerceAtLeast(1)
    )

    val topPrograms = state.programs.map { p ->
        p.title to state.deals.count { it.programId == p.id }
    }.sortedByDescending { it.second }

    val topTourbases = state.tourbases.sortedByDescending { it.bookedSeats }.take(5)
    val topVkProducts = state.vkProducts.sortedByDescending { it.soldCount }.take(4)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionTitleBar(
                title = "Аналитика и статистика",
                subtitle = "Источники лидов, рейтинг специалистов, пиковые часы и экспорт CSV"
            )
        }

        // Period selector (Week / Month / Year)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(DashboardPeriod.WEEK, DashboardPeriod.MONTH, DashboardPeriod.YEAR).forEach { p ->
                    FilterChip(
                        selected = period == p,
                        onClick = { period = p },
                        label = { Text(p.label) }
                    )
                }
            }
        }

        // CSV Export Card
        item {
            CrmCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) {
                Text("Экспорт отчётов в CSV (UTF-8 для Excel)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { shareCsv("База клиентов CSV", buildClientsCsv()) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_clients_csv_btn")
                    ) {
                        Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Клиенты CSV")
                    }
                    OutlinedButton(
                        onClick = { shareCsv("Финансовый отчёт CSV", buildFinanceCsv()) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_finance_csv_btn")
                    ) {
                        Icon(Icons.Filled.TableChart, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Финансы CSV")
                    }
                }
            }
        }

        // Lead Sources Chart
        item {
            CrmCard {
                Text("Источники лидов (${period.label})", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                SimpleBarChart(
                    data = sourceCounts,
                    barColor = crmColors.info
                )
            }
        }

        // Popular Booking Hours Chart
        item {
            CrmCard {
                Text("Самые популярные часы записи (${period.label})", style = MaterialTheme.typography.titleMedium)
                Text("Пик обращений: Чт–Сб в 16:00–19:30", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                SimpleBarChart(
                    data = popularHours,
                    barColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Specialists Ranking
        item {
            CrmCard {
                Text("Рейтинг специалистов по числу сессий", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val maxSessions = (state.specialists.maxOfOrNull { it.completedSessions } ?: 1).coerceAtLeast(1)
                state.specialists.sortedByDescending { it.completedSessions }.forEach { sp ->
                    Column(modifier = Modifier.padding(vertical = 5.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(sp.fullName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text("${sp.completedSessions} сессий · ${"%.2f".format(sp.rating)}★", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { sp.completedSessions.toFloat() / maxSessions.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = crmColors.success
                        )
                    }
                }
            }
        }

        // Top Programs, Tourbases & VK Products
        item {
            CrmCard {
                Text("Востребованные программы, турбазы и товары ВК", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("Топ программ по сделкам:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                topPrograms.forEach { (title, count) ->
                    Text("• $title — $count сдел.", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Text("Топ-5 турбаз по заполняемости:", style = MaterialTheme.typography.labelMedium, color = crmColors.success)
                topTourbases.forEach { tb ->
                    Text("• ${tb.name} (${tb.district}) — ${tb.bookedSeats}/${tb.capacity} мест", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                Text("Лидеры продаж товаров ВК:", style = MaterialTheme.typography.labelMedium, color = crmColors.info)
                topVkProducts.forEach { pr ->
                    Text("• ${pr.name} — ${pr.soldCount} продаж (${formatRub(pr.priceRub)})", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { LegalFooterBanner() }
    }

    csvPreviewTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { csvPreviewTitle = null },
            title = { Text("Предпросмотр $title") },
            text = {
                Text(
                    text = csvPreviewContent.take(700) + if (csvPreviewContent.length > 700) "\n..." else "",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            confirmButton = { TextButton(onClick = { csvPreviewTitle = null }) { Text("Готово") } }
        )
    }
}

@Composable
private fun SimpleBarChart(
    data: List<Pair<String, Int>>,
    barColor: Color
) {
    val maxVal = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val count = data.size.coerceAtLeast(1)
            val gap = 12.dp.toPx()
            val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(12f)
            data.forEachIndexed { idx, (_, value) ->
                val left = idx * (barWidth + gap)
                val barHeight = (value.toFloat() / maxVal.toFloat()) * (size.height - 8.dp.toPx())
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(left, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { (label, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("$value", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = barColor)
                    Text(label.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun AuditSettingsScreen(
    state: CrmDatabaseState,
    syncInfo: SupabaseSyncInfo,
    onSyncNow: () -> Unit,
    onSeedDemoToSupabase: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleFont: () -> Unit,
    onResetDemoData: () -> Unit
) {
    val crmColors = LocalCrmColors.current
    var showResetConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("audit_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitleBar(
                title = "Аудит, права и Supabase",
                subtitle = "Синхронизация таблиц clients и bookings, матрица ролей и журнал действий"
            )
        }

        // Supabase Integration Status & Controls
        item {
            CrmCard(borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) {
                Text("Подключение к Supabase REST API", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = SUPABASE_REST_URL,
                    style = MaterialTheme.typography.labelSmall,
                    color = crmColors.info
                )
                Spacer(Modifier.height(8.dp))
                SupabaseSyncBanner(syncInfo = syncInfo, onSyncNow = onSyncNow)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onSeedDemoToSupabase,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seed_demo_supabase_btn")
                ) {
                    Icon(Icons.Filled.CloudUpload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Загрузить демо-данные в Supabase (clients + bookings)")
                }
            }
        }

        // Interface Comfort & Demo Reset Card
        item {
            CrmCard {
                Text("Комфорт интерфейса", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Тёмная ночная тема (#0F172A)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = state.settings.darkTheme, onCheckedChange = { onToggleTheme() })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Крупный размер шрифта", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = state.settings.largeFont, onCheckedChange = { onToggleFont() })
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = crmColors.danger),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_demo_data_btn")
                ) {
                    Icon(Icons.Filled.RestartAlt, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Обновить состояние и перечитать Supabase")
                }
            }
        }

        // Role Matrix Overview
        item {
            CrmCard {
                Text("Настройки прав ролевой модели", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("• Администратор / Руководитель: полный доступ ко всем разделам и таблицам Supabase.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text("• Менеджер по продажам: сводка, воронка Канбан, база клиентов 360°, календарь, турбазы, каталог, финансы/рассрочка 0%, единый чат.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text("• Специалист: персональная сводка, личный календарь (bookings), закреплённые клиенты (clients), заметки и отзывы.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text("• Клиент (Личный кабинет): профиль, дневник сна, самостоятельная запись и просмотр своих сессий из Supabase bookings.", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Audit Log
        item { Text("Журнал истории действий (${state.auditLog.size})", style = MaterialTheme.typography.titleMedium) }
        items(state.auditLog, key = { it.id }) { entry ->
            CrmCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(entry.action, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    StatusPill(entry.actorRole.shortLabel, crmColors.infoContainer, crmColors.info)
                }
                Spacer(Modifier.height(4.dp))
                Text(entry.details, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${entry.actorName} · ${entry.timestamp}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item { LegalFooterBanner() }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Сбросить демо-данные?") },
            text = { Text("Все локальные изменения будут заменены исходным набором данных «Кринж & Co» (12 турбаз, 4 специалиста, 12 клиентов, сделки и оплаты).") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetDemoData()
                        showResetConfirm = false
                    }
                ) { Text("Сбросить") }
            },
            dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Отмена") } }
        )
    }
}
