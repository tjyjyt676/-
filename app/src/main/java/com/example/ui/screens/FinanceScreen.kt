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
fun FinanceScreen(
    state: CrmDatabaseState,
    onMarkInstallmentPaid: (paymentId: String, stepId: String) -> Unit,
    onCancelPaidPayment: (paymentId: String, toDeposit: Boolean) -> Unit,
    onSimulateAutoPayment: () -> Unit
) {
    val crmColors = LocalCrmColors.current
    var statusFilter by remember { mutableStateOf<PaymentStatus?>(null) }
    var cancelTarget by remember { mutableStateOf<PaymentRecord?>(null) }

    val netPaid = state.payments.filter { it.status != PaymentStatus.REFUNDED }.sumOf { it.paidAmountRub }
    val pendingInstallmentsSum = state.payments.flatMap { it.installments }
        .filter { it.status != InstallmentStepStatus.PAID }
        .sumOf { it.amountRub }
    val overdueCount = state.payments.count { it.status == PaymentStatus.OVERDUE }

    val filtered = remember(state.payments, statusFilter) {
        if (statusFilter == null) state.payments else state.payments.filter { it.status == statusFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("finance_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitleBar(
                title = "Финансы, рассрочка 0% и отмены",
                subtitle = "Реестр оплат, график взносов и автоматическое освобождение слотов при отмене",
                trailing = {
                    Button(
                        onClick = onSimulateAutoPayment,
                        modifier = Modifier.testTag("simulate_app_payment_btn")
                    ) {
                        Icon(Icons.Filled.PhoneAndroid, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Автооплата")
                    }
                }
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KpiMetricCard(
                    title = "Факт. поступления",
                    value = formatRub(netPaid),
                    subtitle = "За вычетом возвратов",
                    icon = Icons.Filled.AccountBalanceWallet,
                    accentColor = crmColors.success,
                    accentContainer = crmColors.successContainer,
                    modifier = Modifier.weight(1f)
                )
                KpiMetricCard(
                    title = "К получению (0%)",
                    value = formatRub(pendingInstallmentsSum),
                    subtitle = "Просрочек: $overdueCount",
                    icon = Icons.Filled.Schedule,
                    accentColor = if (overdueCount > 0) crmColors.danger else MaterialTheme.colorScheme.primary,
                    accentContainer = if (overdueCount > 0) crmColors.dangerContainer else crmColors.warningContainer,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { statusFilter = null },
                    label = { Text("Все (${state.payments.size})") }
                )
                PaymentStatus.entries.forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = if (statusFilter == st) null else st },
                        label = { Text(st.label) }
                    )
                }
            }
        }

        items(filtered, key = { it.id }) { pay ->
            val client = state.clients.find { it.id == pay.clientId }
            val (bg, fg) = when (pay.status) {
                PaymentStatus.PAID -> crmColors.successContainer to crmColors.success
                PaymentStatus.INSTALLMENT -> crmColors.warningContainer to crmColors.warning
                PaymentStatus.OVERDUE -> crmColors.dangerContainer to crmColors.danger
                PaymentStatus.REFUNDED -> crmColors.dangerContainer to crmColors.danger
                PaymentStatus.DEPOSIT -> crmColors.infoContainer to crmColors.info
            }

            CrmCard(modifier = Modifier.fillMaxWidth().testTag("payment_card_${pay.id}")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(client?.fullName ?: "Клиент", style = MaterialTheme.typography.titleMedium)
                        Text(pay.programTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    StatusPill(pay.status.label, bg, fg)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Оплачено: ${formatRub(pay.paidAmountRub)} из ${formatRub(pay.totalAmountRub)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${pay.date} · ${if (pay.isAutoFromApp) "Автооплата из приложения" else "Ручное оформление"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (pay.status == PaymentStatus.PAID || pay.status == PaymentStatus.INSTALLMENT || pay.status == PaymentStatus.OVERDUE) {
                        OutlinedButton(
                            onClick = { cancelTarget = pay },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = crmColors.danger),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Возврат / Депозит", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // 0% Installment Schedule Breakdown
                if (pay.installments.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = crmColors.subtleBorder.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("График платежей по рассрочке 0%:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(6.dp))
                    pay.installments.forEach { step ->
                        val (sBg, sFg) = when (step.status) {
                            InstallmentStepStatus.PAID -> crmColors.successContainer to crmColors.success
                            InstallmentStepStatus.UPCOMING -> crmColors.warningContainer to crmColors.warning
                            InstallmentStepStatus.OVERDUE -> crmColors.dangerContainer to crmColors.danger
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = sBg.copy(alpha = 0.55f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Взнос №${step.number} · ${step.dueDate}", style = MaterialTheme.typography.labelMedium)
                                    Text("${formatRub(step.amountRub)} · ${step.status.label}", style = MaterialTheme.typography.labelSmall, color = sFg)
                                }
                                if (step.status != InstallmentStepStatus.PAID) {
                                    TextButton(onClick = { onMarkInstallmentPaid(pay.id, step.id) }) {
                                        Text("Оплачен ✓")
                                    }
                                } else {
                                    Icon(Icons.Filled.CheckCircle, null, tint = crmColors.success, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        item { LegalFooterBanner() }
    }

    cancelTarget?.let { pay ->
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Отмена оплаченного занятия") },
            text = {
                Text(
                    "Выберите сценарий для оплаты «${pay.programTitle}» (${formatRub(pay.paidAmountRub)}). Система автоматически пересчитает выручку и освободит слот в календаре специалиста и на турбазе.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { onCancelPaidPayment(pay.id, true); cancelTarget = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Перенести на депозит клиента") }
                    OutlinedButton(
                        onClick = { onCancelPaidPayment(pay.id, false); cancelTarget = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Оформить возврат средств") }
                }
            },
            dismissButton = { TextButton(onClick = { cancelTarget = null }) { Text("Закрыть") } }
        )
    }
}
