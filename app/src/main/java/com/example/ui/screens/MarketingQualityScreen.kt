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
fun PushVkScreen(
    state: CrmDatabaseState,
    onSendPush: (String, String, AgeSegment?, PushType) -> Unit,
    onAddVkBroadcast: (String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    val crmColors = LocalCrmColors.current
    var pushTitle by remember { mutableStateOf("") }
    var pushBody by remember { mutableStateOf("") }
    var targetSegment by remember { mutableStateOf<AgeSegment?>(null) }
    var pushType by remember { mutableStateOf(PushType.SEGMENT_MANUAL) }

    var streamTitle by remember { mutableStateOf("") }
    var streamUrl by remember { mutableStateOf(DemoDataSeeder.VK_COMMUNITY_URL) }
    var streamDateTime by remember { mutableStateOf("26.11.2026 · 19:00 МСК") }
    var streamDesc by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("push_vk_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionTitleBar(
                title = "Пуш-уведомления и ВК-эфиры",
                subtitle = "Рассылки по сегментам, напоминания за 24ч и трансляции из vk.ru/club241842043"
            )
        }

        // Push Composer Card
        item {
            CrmCard {
                Text("Создать пуш-рассылку в приложение", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = pushType == PushType.REMINDER_24H,
                        onClick = {
                            pushType = PushType.REMINDER_24H
                            if (pushTitle.isBlank()) {
                                pushTitle = "Напоминание о сессии за 24 часа"
                                pushBody = "Ждём вас на занятии «Кринж & Co». Бесплатный перенос доступен за 24ч."
                            }
                        },
                        label = { Text("Напоминание 24ч") }
                    )
                    FilterChip(
                        selected = pushType == PushType.SEGMENT_MANUAL,
                        onClick = { pushType = PushType.SEGMENT_MANUAL },
                        label = { Text("По сегменту") }
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = targetSegment == null,
                        onClick = { targetSegment = null },
                        label = { Text("Все сегменты (${state.clients.size})") }
                    )
                    AgeSegment.entries.forEach { seg ->
                        FilterChip(
                            selected = targetSegment == seg,
                            onClick = { targetSegment = seg },
                            label = { Text(seg.shortLabel) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pushTitle,
                    onValueChange = { pushTitle = it },
                    label = { Text("Заголовок пуш-уведомления") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = pushBody,
                    onValueChange = { pushBody = it },
                    label = { Text("Текст уведомления") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (pushTitle.isNotBlank() && pushBody.isNotBlank()) {
                            onSendPush(pushTitle, pushBody, targetSegment, pushType)
                            pushTitle = ""
                            pushBody = ""
                        }
                    },
                    modifier = Modifier.testTag("send_push_btn")
                ) {
                    Icon(Icons.Filled.NotificationsActive, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Отправить пуш")
                }
            }
        }

        // VK Broadcasts Creator (auto-creates announcement push with calendar tag!)
        item {
            CrmCard(borderColor = crmColors.info.copy(alpha = 0.5f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Прямые трансляции и видео ВК", style = MaterialTheme.typography.titleMedium)
                    TextButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(DemoDataSeeder.VK_COMMUNITY_URL)))
                            }
                        }
                    ) { Text("Сообщество ВК") }
                }
                Text(
                    "При добавлении ссылки автоматически формируется анонс-пуш для клиентского приложения с отметкой даты/времени для календаря.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = streamTitle, onValueChange = { streamTitle = it }, label = { Text("Название эфира / видео ВК") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = streamUrl, onValueChange = { streamUrl = it }, label = { Text("Ссылка ВК") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = streamDateTime, onValueChange = { streamDateTime = it }, label = { Text("Дата и время для календаря") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = streamDesc, onValueChange = { streamDesc = it }, label = { Text("Краткий анонс") }, modifier = Modifier.fillMaxWidth(), maxLines = 2)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (streamTitle.isNotBlank()) {
                            onAddVkBroadcast(streamTitle, streamUrl, streamDateTime, streamDesc.ifBlank { "Эфир экспертов «Кринж & Co»" })
                            streamTitle = ""
                            streamDesc = ""
                        }
                    },
                    modifier = Modifier.testTag("add_vk_stream_btn")
                ) {
                    Icon(Icons.Filled.LiveTv, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Добавить эфир + создать анонс-пуш")
                }
            }
        }

        // Scheduled VK Broadcasts list
        item { Text("Запланированные ВК-трансляции (${state.vkBroadcasts.size})", style = MaterialTheme.typography.titleMedium) }
        items(state.vkBroadcasts, key = { it.id }) { vb ->
            CrmCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(vb.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    StatusPill(vb.scheduledDateTime, crmColors.infoContainer, crmColors.info, icon = Icons.Filled.Event)
                }
                Spacer(Modifier.height(4.dp))
                Text(vb.description, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (vb.pushCreated) {
                        StatusPill("Анонс-пуш отправлен ✓", crmColors.successContainer, crmColors.success)
                    }
                    Text(vb.vkUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Push History
        item { Text("История пуш-рассылок (${state.pushCampaigns.size})", style = MaterialTheme.typography.titleMedium) }
        items(state.pushCampaigns, key = { it.id }) { pc ->
            CrmCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(pc.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    StatusPill("${pc.recipientCount} чел.", crmColors.warningContainer, crmColors.warning)
                }
                Spacer(Modifier.height(4.dp))
                Text(pc.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${pc.type.label} · ${pc.sentAt}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (pc.calendarEventTag != null) {
                        StatusPill("Календарь: ${pc.calendarEventTag}", crmColors.violetContainer, crmColors.violet)
                    }
                }
            }
        }

        item { LegalFooterBanner() }
    }
}

@Composable
fun ReviewsScreen(
    state: CrmDatabaseState,
    onTogglePublish: (String) -> Unit
) {
    val crmColors = LocalCrmColors.current
    val isAdmin = state.settings.currentRole == UserRole.ADMIN
    val isSpecialist = state.settings.currentRole == UserRole.SPECIALIST

    val visibleReviews = remember(state.reviews, isSpecialist, state.settings.activeSpecialistId) {
        if (isSpecialist) state.reviews.filter { it.specialistId == state.settings.activeSpecialistId } else state.reviews
    }

    val avgScore = if (visibleReviews.isEmpty()) 0.0 else visibleReviews.map { it.rating }.average()
    val csiPercent = ((avgScore / 5.0) * 100).toInt()
    val publishedCount = visibleReviews.count { it.publishedInApp }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reviews_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitleBar(
                title = "Контроль качества и отзывы",
                subtitle = if (isSpecialist) "Анкеты клиентов по вашим проведённым занятиям" else "Лента анкет после сессий, индекс CSI и модерация в приложение"
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                    title = "Индекс удовлетворённости (CSI)",
                    value = "$csiPercent%",
                    subtitle = "Средняя оценка ${"%.2f".format(avgScore)} / 5.0",
                    icon = Icons.Filled.SentimentVerySatisfied,
                    accentColor = crmColors.success,
                    accentContainer = crmColors.successContainer,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "В приложении",
                    value = "$publishedCount / ${visibleReviews.size}",
                    subtitle = "Опубликовано отзывов",
                    icon = Icons.Filled.Verified,
                    accentColor = MaterialTheme.colorScheme.primary,
                    accentContainer = crmColors.warningContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        items(visibleReviews, key = { it.id }) { rev ->
            val client = state.clients.find { it.id == rev.clientId }
            val sp = state.specialists.find { it.id == rev.specialistId }
            val prog = state.programs.find { it.id == rev.programId }

            CrmCard(modifier = Modifier.fillMaxWidth().testTag("review_card_${rev.id}")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(client?.fullName ?: "Клиент", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${prog?.title ?: "Программа"} · ${sp?.fullName ?: "Специалист"} · ${rev.date}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusPill(
                        text = "${"★".repeat(rev.rating)} ${rev.rating}/5",
                        containerColor = crmColors.warningContainer,
                        contentColor = crmColors.warning
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("Впечатления:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(rev.impression, style = MaterialTheme.typography.bodyMedium)
                if (rev.remarks.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Замечания / пожелания:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rev.remarks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = if (rev.publishedInApp) "Опубликован в приложении" else "На модерации",
                        containerColor = if (rev.publishedInApp) crmColors.successContainer else crmColors.infoContainer,
                        contentColor = if (rev.publishedInApp) crmColors.success else crmColors.info
                    )
                    if (isAdmin) {
                        Button(
                            onClick = { onTogglePublish(rev.id) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(if (rev.publishedInApp) "Скрыть" else "Опубликовать отзыв в приложении")
                        }
                    }
                }
            }
        }

        item { LegalFooterBanner() }
    }
}
