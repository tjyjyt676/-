package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.*
import com.example.ui.components.*
import com.example.ui.theme.LocalCrmColors
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    state: CrmDatabaseState,
    syncInfo: SupabaseSyncInfo,
    onSyncNow: () -> Unit,
    onNavigate: (CrmSection) -> Unit,
    onOpenClient: (String) -> Unit,
    onSelectSpecialist: (String) -> Unit,
    onSelectClient: (String) -> Unit
) {
    val crmColors = LocalCrmColors.current
    var selectedPeriod by remember { mutableStateOf(DashboardPeriod.MONTH) }
    val role = state.settings.currentRole
    val activeSpecialist = state.specialists.find { it.id == state.settings.activeSpecialistId } ?: state.specialists.first()
    val activeClient = state.clients.find { it.id == state.settings.activeClientId } ?: state.clients.firstOrNull()

    // Base monthly metrics scaled smoothly by period
    val netRevenueMonth = state.payments
        .filter { it.status != PaymentStatus.REFUNDED }
        .sumOf { it.paidAmountRub } + state.vkOrders.sumOf { it.totalRub }
    val scaledRevenue = (netRevenueMonth * selectedPeriod.multiplier).roundToInt()

    val totalDeals = state.deals.size
    val scaledLeads = ((totalDeals * selectedPeriod.multiplier).roundToInt()).coerceAtLeast(1)
    val activeInstallments = state.payments.count { it.status == PaymentStatus.INSTALLMENT || it.status == PaymentStatus.OVERDUE }
    val paidOrDoneDeals = state.deals.count {
        it.stage == DealStage.PAID_BOOKED || it.stage == DealStage.IN_PROGRESS || it.stage == DealStage.COMPLETED
    }
    val conversionPct = if (totalDeals > 0) (paidOrDoneDeals * 100) / totalDeals else 0
    val cancelledCount = state.deals.count { it.stage == DealStage.CANCELLED } +
        state.bookings.count { it.status == BookingStatus.CANCELLED }
    val scaledCancels = ((cancelledCount * selectedPeriod.multiplier).roundToInt()).coerceAtLeast(if (cancelledCount > 0) 1 else 0)

    val totalSeats = state.tourbases.sumOf { it.capacity }.coerceAtLeast(1)
    val bookedSeats = state.tourbases.sumOf { it.bookedSeats }
    val tourbaseOccupancyPct = (bookedSeats * 100) / totalSeats

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Retreat Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_volga_retreat_banner_1795118713185),
                    contentDescription = stringResource(R.string.cd_retreat_banner),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xE60F172A),
                                    Color(0x990F172A),
                                    Color(0x440F172A)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusPill(
                            text = "${role.title} · ${role.subtitle}",
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color(0xFF1C1302),
                            icon = Icons.Filled.VerifiedUser
                        )
                        StatusPill(
                            text = "Волгоград · 15 лет",
                            containerColor = Color(0xAA1E293B),
                            contentColor = Color(0xFFF8FAFC)
                        )
                    }
                    Column {
                        Text(
                            text = when (role) {
                                UserRole.SPECIALIST -> "Кабинет: ${activeSpecialist.fullName}"
                                UserRole.CLIENT -> "Личный кабинет: ${activeClient?.fullName ?: "Клиент"}"
                                else -> "Сводка центра «Кринж & Co»"
                            },
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Восстановление сна и преодоление реваншистской прокрастинации · 12 турбаз · Синхронизация Supabase",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Live Supabase Sync Banner
        item {
            SupabaseSyncBanner(
                syncInfo = syncInfo,
                onSyncNow = onSyncNow
            )
        }

        // Client switcher when in Client Cabinet role
        if (role == UserRole.CLIENT && state.clients.isNotEmpty()) {
            item {
                CrmCard {
                    Text(
                        text = "Выберите профиль клиента (таблица Supabase clients):",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.clients) { cl ->
                            FilterChip(
                                selected = cl.id == activeClient?.id,
                                onClick = { onSelectClient(cl.id) },
                                label = { Text(cl.fullName.substringBeforeLast(" ")) }
                            )
                        }
                    }
                }
            }
        }

        // Specialist switcher when in Specialist role
        if (role == UserRole.SPECIALIST) {
            item {
                CrmCard {
                    Text(
                        text = "Выберите профиль специалиста для просмотра:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.specialists) { sp ->
                            FilterChip(
                                selected = sp.id == activeSpecialist.id,
                                onClick = { onSelectSpecialist(sp.id) },
                                label = { Text(sp.fullName.substringBeforeLast(" ")) },
                                leadingIcon = if (sp.hasMedicalEducation) {
                                    { Icon(Icons.Filled.MedicalServices, null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        // Period Filter Chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ключевые показатели",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DashboardPeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = { Text(period.label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag("period_${period.name.lowercase()}")
                        )
                    }
                }
            }
        }

        // Client Cabinet Dashboard
        if (role == UserRole.CLIENT && activeClient != null) {
            val clientBookings = state.bookings.filter { it.clientId == activeClient.id }
            val activeCount = clientBookings.count { it.status == BookingStatus.CONFIRMED }
            val recProg = state.programs.find { it.id == activeClient.recommendedProgramId }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiMetricCard(
                            title = "Мои записи (Supabase)",
                            value = "$activeCount",
                            subtitle = "Всего в истории: ${clientBookings.size}",
                            icon = Icons.Filled.CalendarMonth,
                            accentColor = crmColors.info,
                            accentContainer = crmColors.infoContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(CrmSection.CALENDAR) }
                        )
                        KpiMetricCard(
                            title = "Экспресс-тест сна",
                            value = "${activeClient.expressTestScore}/100",
                            subtitle = recProg?.title?.take(20) ?: "Онлайн-курс",
                            icon = Icons.Filled.Psychology,
                            accentColor = MaterialTheme.colorScheme.primary,
                            accentContainer = crmColors.warningContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenClient(activeClient.id) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiMetricCard(
                            title = "Дневник гигиены сна",
                            value = "${activeClient.sleepDiary.adherencePercent}%",
                            subtitle = "Средний сон: ${activeClient.sleepDiary.avgSleepHours} ч",
                            icon = Icons.Filled.Bedtime,
                            accentColor = crmColors.violet,
                            accentContainer = crmColors.violetContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenClient(activeClient.id) }
                        )
                        KpiMetricCard(
                            title = "Согласие 152-ФЗ",
                            value = if (activeClient.pdnConsentSigned) "Подписано" else "Требуется",
                            subtitle = activeClient.pdnConsentDate ?: "Откройте профиль",
                            icon = Icons.Filled.VerifiedUser,
                            accentColor = if (activeClient.pdnConsentSigned) crmColors.success else crmColors.danger,
                            accentContainer = if (activeClient.pdnConsentSigned) crmColors.successContainer else crmColors.dangerContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenClient(activeClient.id) }
                        )
                    }
                }
            }

            item {
                CrmCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Мои ближайшие записи", style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { onNavigate(CrmSection.CALENDAR) }) {
                            Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Записаться")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (clientBookings.isEmpty()) {
                        Text(
                            "Пока нет активных записей. Нажмите «Записаться», чтобы создать запись напрямую в Supabase.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        clientBookings.forEach { bk ->
                            val sp = state.specialists.find { it.id == bk.specialistId }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${bk.dayOfWeek}, ${bk.dateLabel} · ${bk.timeSlot}", style = MaterialTheme.typography.titleSmall)
                                        Text(sp?.fullName ?: "Специалист центра", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(
                                        text = bk.status.label,
                                        containerColor = if (bk.status == BookingStatus.CONFIRMED) crmColors.successContainer else crmColors.dangerContainer,
                                        contentColor = if (bk.status == BookingStatus.CONFIRMED) crmColors.success else crmColors.danger
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (role == UserRole.SPECIALIST) {
            val myClients = state.clients.filter { it.assignedSpecialistId == activeSpecialist.id }
            val myBookings = state.bookings.filter { it.specialistId == activeSpecialist.id && it.status == BookingStatus.CONFIRMED }
            val myReviews = state.reviews.filter { it.specialistId == activeSpecialist.id }
            val avgRating = if (myReviews.isEmpty()) activeSpecialist.rating else myReviews.map { it.rating }.average()

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiMetricCard(
                            title = "Мои клиенты",
                            value = "${myClients.size}",
                            subtitle = "Закреплены за вами",
                            icon = Icons.Filled.People,
                            accentColor = MaterialTheme.colorScheme.primary,
                            accentContainer = crmColors.warningContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(CrmSection.CLIENTS) }
                        )
                        KpiMetricCard(
                            title = "Предстоящие сессии",
                            value = "${myBookings.size}",
                            subtitle = "В личном календаре",
                            icon = Icons.Filled.CalendarMonth,
                            accentColor = crmColors.info,
                            accentContainer = crmColors.infoContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(CrmSection.CALENDAR) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiMetricCard(
                            title = "Рейтинг по отзывам",
                            value = "${"%.2f".format(avgRating)} ★",
                            subtitle = "${myReviews.size} анкет после сессий",
                            icon = Icons.Filled.StarRate,
                            accentColor = crmColors.success,
                            accentContainer = crmColors.successContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(CrmSection.REVIEWS) }
                        )
                        KpiMetricCard(
                            title = "Проведено сессий",
                            value = "${activeSpecialist.completedSessions}",
                            subtitle = if (activeSpecialist.hasMedicalEducation) "Медобразование" else "Коуч сна",
                            icon = Icons.Filled.WorkspacePremium,
                            accentColor = crmColors.violet,
                            accentContainer = crmColors.violetContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                CrmCard {
                    Text("Закреплённые клиенты (тест и дневник сна)", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    myClients.forEach { cl ->
                        Surface(
                            onClick = { onOpenClient(cl.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cl.fullName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${cl.segment.label} · Дневник сна: ${cl.sleepDiary.adherencePercent}%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                StatusPill(
                                    text = "Тест ${cl.expressTestScore}/100",
                                    containerColor = crmColors.warningContainer,
                                    contentColor = crmColors.warning
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiMetricCard(
                            title = "Новые заявки (${selectedPeriod.label})",
                            value = "$scaledLeads",
                            subtitle = "Приложение, ВК и сайт",
                            icon = Icons.Filled.TrendingUp,
                            accentColor = crmColors.info,
                            accentContainer = crmColors.infoContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(CrmSection.KANBAN) }
                        )
                        KpiMetricCard(
                            title = "Выручка (${selectedPeriod.label})",
                            value = formatRub(scaledRevenue),
                            subtitle = "Курсы + товары ВК",
                            icon = Icons.Filled.Payments,
                            accentColor = crmColors.success,
                            accentContainer = crmColors.successContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(CrmSection.FINANCE) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiMetricCard(
                            title = "Рассрочки 0%",
                            value = "$activeInstallments акт.",
                            subtitle = "${state.payments.count { it.status == PaymentStatus.OVERDUE }} просроч.",
                            icon = Icons.Filled.CreditScore,
                            accentColor = MaterialTheme.colorScheme.primary,
                            accentContainer = crmColors.warningContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(CrmSection.FINANCE) }
                        )
                        KpiMetricCard(
                            title = "Конверсия тест → оплата",
                            value = "$conversionPct%",
                            subtitle = "Отмен за период: $scaledCancels",
                            icon = Icons.Filled.Insights,
                            accentColor = crmColors.violet,
                            accentContainer = crmColors.violetContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { if (role == UserRole.ADMIN) onNavigate(CrmSection.ANALYTICS) }
                        )
                    }
                }
            }

            // Specialist Workload
            item {
                CrmCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Загрузка специалистов", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { onNavigate(CrmSection.CALENDAR) }) {
                            Text("Календарь")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    state.specialists.forEach { sp ->
                        val activeSlots = state.bookings.count { it.specialistId == sp.id && it.status == BookingStatus.CONFIRMED }
                        val loadFrac = ((activeSlots + 2) / 7f).coerceIn(0.15f, 1f)
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = sp.fullName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$activeSlots зап. · ${(loadFrac * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { loadFrac },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }

            // 12 Partner Tourbases Occupancy Summary
            item {
                CrmCard(onClick = { onNavigate(CrmSection.TOURBASES) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Загрузка 12 турбаз Волгоградской обл.", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Занято $bookedSeats из $totalSeats мест под 3-дневные интенсивы ($tourbaseOccupancyPct%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StatusPill(
                            text = "${state.tourbases.count { it.status != TourbaseStatus.FULL }} доступны",
                            containerColor = crmColors.successContainer,
                            contentColor = crmColors.success
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { bookedSeats.toFloat() / totalSeats.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = crmColors.success,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.tourbases.take(6)) { tb ->
                            val (bg, fg) = when (tb.status) {
                                TourbaseStatus.AVAILABLE -> crmColors.successContainer to crmColors.success
                                TourbaseStatus.PARTIAL -> crmColors.warningContainer to crmColors.warning
                                TourbaseStatus.FULL -> crmColors.dangerContainer to crmColors.danger
                            }
                            StatusPill(
                                text = "${tb.name}: ${tb.bookedSeats}/${tb.capacity}",
                                containerColor = bg,
                                contentColor = fg
                            )
                        }
                    }
                }
            }
        }

        // Legal Footer
        item {
            LegalFooterBanner()
        }
    }
}
