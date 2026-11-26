package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class BannerTone { INFO, SUCCESS, WARNING, ERROR }

data class CrmFeedback(
    val message: String,
    val tone: BannerTone = BannerTone.INFO
)

class CrmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CrmRepository()

    private val _state = MutableStateFlow(DemoDataSeeder.createInitialState())
    val state: StateFlow<CrmDatabaseState> = _state.asStateFlow()

    private val _syncInfo = MutableStateFlow(
        if (repository.isConfigured()) {
            SupabaseSyncInfo(status = SupabaseSyncStatus.IDLE)
        } else {
            SupabaseSyncInfo(
                status = SupabaseSyncStatus.MISSING_KEY,
                errorMessage = "Укажите SUPABASE_ANON_KEY в панели Secrets в AI Studio для живой синхронизации."
            )
        }
    )
    val syncInfo: StateFlow<SupabaseSyncInfo> = _syncInfo.asStateFlow()

    private val _feedback = MutableSharedFlow<CrmFeedback>(extraBufferCapacity = 8)
    val feedback: SharedFlow<CrmFeedback> = _feedback.asSharedFlow()

    private val dateTimeFmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.forLanguageTag("ru"))
    private val dateFmt = SimpleDateFormat("dd.MM.yyyy", Locale.forLanguageTag("ru"))
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("ru"))

    init {
        syncFromSupabase(showToast = false)
    }

    private fun nowDateTime(): String = dateTimeFmt.format(Date())
    private fun nowDate(): String = dateFmt.format(Date())
    private fun nowTime(): String = timeFmt.format(Date())
    private fun newId(prefix: String): String = "${prefix}_${UUID.randomUUID().toString().take(6)}"
    private fun newUuid(): String = UUID.randomUUID().toString()

    private fun updateState(
        auditAction: String? = null,
        auditDetails: String? = null,
        transform: (CrmDatabaseState) -> CrmDatabaseState
    ) {
        val prev = _state.value
        var next = transform(prev)
        if (auditAction != null && auditDetails != null) {
            val actorName = when (next.settings.currentRole) {
                UserRole.ADMIN -> "Руководитель центра"
                UserRole.MANAGER -> "Менеджер продаж"
                UserRole.SPECIALIST -> next.specialists.find { it.id == next.settings.activeSpecialistId }?.fullName ?: "Специалист"
                UserRole.CLIENT -> next.clients.find { it.id == next.settings.activeClientId }?.fullName ?: "Клиент"
            }
            val entry = AuditLogEntry(
                id = newId("al"),
                timestamp = nowDateTime(),
                actorRole = next.settings.currentRole,
                actorName = actorName,
                action = auditAction,
                details = auditDetails
            )
            next = next.copy(auditLog = listOf(entry) + next.auditLog)
        }
        _state.value = next
    }

    private fun emitFeedback(message: String, tone: BannerTone = BannerTone.INFO) {
        _feedback.tryEmit(CrmFeedback(message, tone))
    }

    // --- Supabase Sync (clients & bookings) ---

    fun syncFromSupabase(showToast: Boolean = true) {
        if (!repository.isConfigured()) {
            _syncInfo.value = SupabaseSyncInfo(
                status = SupabaseSyncStatus.MISSING_KEY,
                errorMessage = "Укажите SUPABASE_ANON_KEY в панели Secrets в AI Studio."
            )
            if (showToast) {
                emitFeedback("Задайте SUPABASE_ANON_KEY в Secrets в AI Studio для синхронизации", BannerTone.WARNING)
            }
            return
        }

        viewModelScope.launch {
            _syncInfo.value = _syncInfo.value.copy(status = SupabaseSyncStatus.SYNCING, errorMessage = null)
            repository.fetchClientsAndBookings()
                .onSuccess { (remoteClients, remoteBookings) ->
                    updateState { s ->
                        val nextClients = if (remoteClients.isNotEmpty()) remoteClients else s.clients
                        val nextActiveClientId = if (nextClients.any { it.id == s.settings.activeClientId }) {
                            s.settings.activeClientId
                        } else {
                            nextClients.firstOrNull()?.id ?: s.settings.activeClientId
                        }
                        s.copy(
                            clients = nextClients,
                            bookings = remoteBookings,
                            settings = s.settings.copy(activeClientId = nextActiveClientId)
                        )
                    }
                    _syncInfo.value = SupabaseSyncInfo(
                        status = SupabaseSyncStatus.SYNCED,
                        lastSyncedAt = nowTime(),
                        errorMessage = null,
                        remoteClientsCount = remoteClients.size,
                        remoteBookingsCount = remoteBookings.size
                    )
                    if (showToast) {
                        emitFeedback(
                            "Синхронизировано с Supabase: clients (${remoteClients.size}), bookings (${remoteBookings.size})",
                            BannerTone.SUCCESS
                        )
                    }
                }
                .onFailure { err ->
                    val msg = err.message ?: "Не удалось подключиться к Supabase"
                    _syncInfo.value = _syncInfo.value.copy(
                        status = SupabaseSyncStatus.ERROR,
                        errorMessage = msg
                    )
                    if (showToast) {
                        emitFeedback(msg, BannerTone.ERROR)
                    }
                }
        }
    }

    fun seedDemoToSupabase() {
        val fresh = DemoDataSeeder.createInitialState()
        val demoClientsWithUuid = fresh.clients.mapIndexed { idx, cl ->
            cl.copy(id = "00000000-0000-4000-8000-${(idx + 1).toString().padStart(12, '0')}")
        }
        val idMap = fresh.clients.mapIndexed { idx, cl -> cl.id to demoClientsWithUuid[idx].id }.toMap()
        val demoBookingsWithUuid = fresh.bookings.mapIndexed { idx, bk ->
            bk.copy(
                id = "10000000-0000-4000-8000-${(idx + 1).toString().padStart(12, '0')}",
                clientId = idMap[bk.clientId] ?: bk.clientId
            )
        }

        if (!repository.isConfigured()) {
            emitFeedback("Задайте SUPABASE_ANON_KEY в Secrets, чтобы выгрузить демо в Supabase", BannerTone.WARNING)
            return
        }

        viewModelScope.launch {
            _syncInfo.value = _syncInfo.value.copy(status = SupabaseSyncStatus.SYNCING, errorMessage = null)
            repository.seedDemoToSupabase(demoClientsWithUuid, demoBookingsWithUuid)
                .onSuccess { (cCount, bCount) ->
                    syncFromSupabase(showToast = false)
                    emitFeedback("В Supabase загружены демо-данные: clients ($cCount), bookings ($bCount)", BannerTone.SUCCESS)
                }
                .onFailure { err ->
                    val msg = err.message ?: "Ошибка выгрузки в Supabase"
                    _syncInfo.value = _syncInfo.value.copy(status = SupabaseSyncStatus.ERROR, errorMessage = msg)
                    emitFeedback(msg, BannerTone.ERROR)
                }
        }
    }

    // --- UI & Role Settings ---

    fun switchRole(role: UserRole) {
        updateState { it.copy(settings = it.settings.copy(currentRole = role)) }
        emitFeedback("Режим роли: ${role.title} (${role.subtitle})", BannerTone.INFO)
        if (repository.isConfigured()) {
            syncFromSupabase(showToast = false)
        }
    }

    fun selectActiveSpecialist(specialistId: String) {
        val sp = _state.value.specialists.find { it.id == specialistId } ?: return
        updateState { it.copy(settings = it.settings.copy(activeSpecialistId = specialistId)) }
        emitFeedback("Активный специалист: ${sp.fullName}", BannerTone.INFO)
    }

    fun selectActiveClient(clientId: String) {
        val cl = _state.value.clients.find { it.id == clientId } ?: return
        updateState { it.copy(settings = it.settings.copy(activeClientId = clientId)) }
        emitFeedback("Кабинет клиента: ${cl.fullName}", BannerTone.INFO)
    }

    fun toggleDarkTheme() {
        updateState { it.copy(settings = it.settings.copy(darkTheme = !it.settings.darkTheme)) }
    }

    fun toggleLargeFont() {
        val nextLarge = !_state.value.settings.largeFont
        updateState { it.copy(settings = it.settings.copy(largeFont = nextLarge)) }
        emitFeedback(
            if (nextLarge) "Включён крупный шрифт" else "Включён обычный размер шрифта",
            BannerTone.INFO
        )
    }

    fun resetDemoData() {
        val currentSettings = _state.value.settings
        val fresh = DemoDataSeeder.createInitialState()
        _state.value = fresh.copy(
            settings = fresh.settings.copy(
                darkTheme = currentSettings.darkTheme,
                largeFont = currentSettings.largeFont,
                currentRole = currentSettings.currentRole
            )
        )
        if (repository.isConfigured()) {
            syncFromSupabase(showToast = true)
        } else {
            emitFeedback("Демо-состояние обновлено (ожидает SUPABASE_ANON_KEY)", BannerTone.INFO)
        }
    }

    // --- Deals & Kanban (with 152-FZ PDn Consent Gate) ---

    fun moveDealToStage(dealId: String, targetStage: DealStage): Boolean {
        val current = _state.value
        val deal = current.deals.find { it.id == dealId } ?: return false
        if (deal.stage == targetStage) return true
        val client = current.clients.find { it.id == deal.clientId }

        if (targetStage.requiresPdnConsent && client != null && !client.pdnConsentSigned) {
            emitFeedback(
                "Блокировка 152-ФЗ: нельзя перевести «${client.fullName}» в «${targetStage.shortLabel}» без согласия на обработку ПДн!",
                BannerTone.ERROR
            )
            return false
        }

        updateState(
            auditAction = "Смена этапа сделки",
            auditDetails = "${client?.fullName ?: deal.clientId}: ${deal.stage.shortLabel} → ${targetStage.shortLabel}"
        ) { s ->
            val updatedDeals = s.deals.map {
                if (it.id == dealId) it.copy(stage = targetStage, updatedAt = nowDate()) else it
            }
            var updatedPayments = s.payments
            if (targetStage == DealStage.PAID_BOOKED && s.payments.none { it.dealId == dealId }) {
                val prog = s.programs.find { it.id == deal.programId }
                updatedPayments = listOf(
                    PaymentRecord(
                        id = newId("pay"),
                        dealId = deal.id,
                        clientId = deal.clientId,
                        programTitle = prog?.title ?: "Программа центра",
                        totalAmountRub = deal.amountRub,
                        paidAmountRub = if (deal.isInstallment) deal.amountRub / 2 else deal.amountRub,
                        status = if (deal.isInstallment) PaymentStatus.INSTALLMENT else PaymentStatus.PAID,
                        date = nowDate(),
                        isAutoFromApp = false,
                        installments = if (deal.isInstallment) {
                            listOf(
                                InstallmentStep(newId("ins"), 1, nowDate(), deal.amountRub / 2, InstallmentStepStatus.PAID),
                                InstallmentStep(newId("ins"), 2, "Через 14 дней", deal.amountRub - deal.amountRub / 2, InstallmentStepStatus.UPCOMING)
                            )
                        } else emptyList()
                    )
                ) + updatedPayments
            }
            s.copy(deals = updatedDeals, payments = updatedPayments)
        }
        emitFeedback("Сделка переведена: ${targetStage.shortLabel}", BannerTone.SUCCESS)
        return true
    }

    fun createDeal(
        clientId: String,
        programId: String,
        specialistId: String,
        tourbaseId: String?,
        isInstallment: Boolean,
        notes: String
    ) {
        val current = _state.value
        val program = current.programs.find { it.id == programId } ?: return
        val client = current.clients.find { it.id == clientId } ?: return
        val deal = Deal(
            id = newId("dl"),
            clientId = clientId,
            programId = programId,
            amountRub = program.priceRub,
            stage = DealStage.NEW_LEAD,
            isInstallment = isInstallment && program.installmentAllowed,
            tourbaseId = tourbaseId,
            specialistId = specialistId,
            createdAt = nowDate(),
            updatedAt = nowDate(),
            notes = notes.ifBlank { "Новая заявка по программе «${program.title}»" }
        )
        updateState(
            auditAction = "Создана сделка",
            auditDetails = "${client.fullName} · ${program.title} (${formatRub(program.priceRub)})"
        ) { s ->
            val updatedBases = if (tourbaseId != null) {
                s.tourbases.map { tb ->
                    if (tb.id == tourbaseId && tb.bookedSeats < tb.capacity) tb.copy(bookedSeats = tb.bookedSeats + 1) else tb
                }
            } else s.tourbases
            s.copy(deals = listOf(deal) + s.deals, tourbases = updatedBases)
        }
        emitFeedback("Сделка для ${client.fullName} добавлена в воронку", BannerTone.SUCCESS)
    }

    fun attachTourbaseToDeal(dealId: String, tourbaseId: String) {
        val current = _state.value
        val deal = current.deals.find { it.id == dealId } ?: return
        val targetBase = current.tourbases.find { it.id == tourbaseId } ?: return
        if (targetBase.bookedSeats >= targetBase.capacity && deal.tourbaseId != tourbaseId) {
            emitFeedback("На турбазе ${targetBase.name} нет свободных мест", BannerTone.WARNING)
            return
        }
        val oldBaseId = deal.tourbaseId
        val clientName = current.clients.find { it.id == deal.clientId }?.fullName ?: deal.clientId
        updateState(
            auditAction = "Прикреплена турбаза к сделке",
            auditDetails = "$clientName → ${targetBase.name}"
        ) { s ->
            val updatedBases = s.tourbases.map { tb ->
                when (tb.id) {
                    tourbaseId -> if (oldBaseId != tourbaseId) tb.copy(bookedSeats = (tb.bookedSeats + 1).coerceAtMost(tb.capacity)) else tb
                    oldBaseId -> tb.copy(bookedSeats = (tb.bookedSeats - 1).coerceAtLeast(0))
                    else -> tb
                }
            }
            val updatedDeals = s.deals.map { if (it.id == dealId) it.copy(tourbaseId = tourbaseId, updatedAt = nowDate()) else it }
            s.copy(deals = updatedDeals, tourbases = updatedBases)
        }
        emitFeedback("Турбаза ${targetBase.name} прикреплена к сделке", BannerTone.SUCCESS)
    }

    // --- Clients 360° (Supabase `clients` table) ---

    fun togglePdnConsent(clientId: String, signed: Boolean) {
        val client = _state.value.clients.find { it.id == clientId } ?: return
        val dateStr = if (signed) nowDate() else null
        updateState(
            auditAction = if (signed) "Подписано согласие 152-ФЗ" else "Отозвано согласие 152-ФЗ",
            auditDetails = "${client.fullName} · ${dateStr ?: "без подписи"}"
        ) { s ->
            s.copy(
                clients = s.clients.map {
                    if (it.id == clientId) it.copy(pdnConsentSigned = signed, pdnConsentDate = dateStr) else it
                }
            )
        }
        if (repository.isConfigured()) {
            viewModelScope.launch {
                repository.updateClientPdn(clientId, signed, dateStr)
                    .onSuccess {
                        emitFeedback(
                            if (signed) "Согласие 152-ФЗ сохранено в Supabase ($dateStr)" else "Согласие 152-ФЗ снято в Supabase",
                            BannerTone.SUCCESS
                        )
                    }
                    .onFailure { err ->
                        emitFeedback(err.message ?: "Ошибка обновления 152-ФЗ в Supabase", BannerTone.ERROR)
                    }
            }
        } else {
            emitFeedback(
                if (signed) "Согласие 152-ФЗ обновлено локально (задайте SUPABASE_ANON_KEY)" else "Согласие 152-ФЗ снято",
                BannerTone.WARNING
            )
        }
    }

    fun addSpecialistNote(clientId: String, text: String) {
        if (text.isBlank()) return
        val current = _state.value
        val client = current.clients.find { it.id == clientId } ?: return
        val sp = current.specialists.find { it.id == current.settings.activeSpecialistId } ?: current.specialists.first()
        val note = SpecialistNote(
            id = newId("sn"),
            specialistId = sp.id,
            specialistName = sp.fullName,
            date = nowDate(),
            text = text.trim()
        )
        val updatedNotes = listOf(note) + client.specialistNotes
        updateState(
            auditAction = "Заметка специалиста",
            auditDetails = "${client.fullName}: ${text.trim().take(60)}"
        ) { s ->
            s.copy(
                clients = s.clients.map {
                    if (it.id == clientId) it.copy(specialistNotes = updatedNotes) else it
                }
            )
        }
        if (repository.isConfigured()) {
            viewModelScope.launch {
                repository.updateClientNotes(clientId, updatedNotes)
                    .onSuccess { emitFeedback("Заметка сохранена в Supabase (clients)", BannerTone.SUCCESS) }
                    .onFailure { err -> emitFeedback(err.message ?: "Ошибка сохранения заметки в Supabase", BannerTone.ERROR) }
            }
        } else {
            emitFeedback("Заметка добавлена (задайте SUPABASE_ANON_KEY для облака)", BannerTone.WARNING)
        }
    }

    fun addClient(
        fullName: String,
        phone: String,
        age: Int,
        source: LeadSource,
        pdnSigned: Boolean,
        testScore: Int,
        specialistId: String
    ) {
        val segment = when {
            age >= 60 -> AgeSegment.SENIORS_60_PLUS
            age in 40..59 -> AgeSegment.WOMEN_40_59
            else -> AgeSegment.PROS_25_45
        }
        val recommendedProg = when {
            testScore >= 80 -> "prog_vip"
            testScore >= 65 -> "prog_group"
            else -> "prog_online"
        }
        val client = Client(
            id = newUuid(),
            fullName = fullName.trim(),
            phone = phone.trim(),
            age = age,
            segment = segment,
            source = source,
            pdnConsentSigned = pdnSigned,
            pdnConsentDate = if (pdnSigned) nowDate() else null,
            expressTestScore = testScore.coerceIn(10, 100),
            recommendedProgramId = recommendedProg,
            sleepDiary = SleepDiaryProgress(0, 0, 0, 7, 6.0),
            assignedSpecialistId = specialistId,
            specialistNotes = emptyList(),
            createdAt = nowDate()
        )

        if (!repository.isConfigured()) {
            updateState(
                auditAction = "Добавлен клиент",
                auditDetails = "${client.fullName} (${segment.shortLabel}, тест ${client.expressTestScore})"
            ) { s -> s.copy(clients = listOf(client) + s.clients) }
            emitFeedback("Клиент добавлен локально. Укажите SUPABASE_ANON_KEY в Secrets для записи в Supabase.", BannerTone.WARNING)
            return
        }

        viewModelScope.launch {
            _syncInfo.value = _syncInfo.value.copy(status = SupabaseSyncStatus.SYNCING, errorMessage = null)
            repository.insertClient(client)
                .onSuccess { saved ->
                    updateState(
                        auditAction = "Клиент сохранён в Supabase",
                        auditDetails = "${saved.fullName} (clients · ${segment.shortLabel})"
                    ) { s -> s.copy(clients = listOf(saved) + s.clients.filterNot { it.id == saved.id }) }
                    _syncInfo.value = _syncInfo.value.copy(
                        status = SupabaseSyncStatus.SYNCED,
                        lastSyncedAt = nowTime(),
                        remoteClientsCount = _state.value.clients.size
                    )
                    emitFeedback("Клиент «${saved.fullName}» сохранён в таблицу Supabase clients!", BannerTone.SUCCESS)
                }
                .onFailure { err ->
                    val msg = err.message ?: "Ошибка записи клиента в Supabase"
                    _syncInfo.value = _syncInfo.value.copy(status = SupabaseSyncStatus.ERROR, errorMessage = msg)
                    emitFeedback(msg, BannerTone.ERROR)
                }
        }
    }

    // --- Calendar & 24-Hour Business Rule + Conflict Guard (Supabase `bookings` table) ---

    fun hoursUntil(booking: Booking): Double {
        return (booking.scheduledEpochMillis - System.currentTimeMillis()) / 3_600_000.0
    }

    fun createBooking(
        clientId: String,
        specialistId: String,
        programId: String,
        tourbaseId: String?,
        dateLabel: String,
        dayOfWeek: String,
        timeSlot: String,
        hoursFromNow: Int,
        notes: String
    ): String? {
        val current = _state.value
        if (hoursFromNow < 24) {
            val warn = "Правило 24 часов: запись возможна не позднее чем за 24 часа до начала сессии!"
            emitFeedback(warn, BannerTone.ERROR)
            return warn
        }
        val conflict = current.bookings.any {
            it.status == BookingStatus.CONFIRMED &&
                it.specialistId == specialistId &&
                it.dateLabel == dateLabel &&
                it.timeSlot == timeSlot
        }
        if (conflict) {
            val spName = current.specialists.find { it.id == specialistId }?.fullName ?: "специалиста"
            val warn = "Защита от накладок: у $spName уже есть запись на $dateLabel в $timeSlot!"
            emitFeedback(warn, BannerTone.ERROR)
            return warn
        }

        val clientName = current.clients.find { it.id == clientId }?.fullName ?: clientId
        val booking = Booking(
            id = newUuid(),
            clientId = clientId,
            specialistId = specialistId,
            programId = programId,
            tourbaseId = tourbaseId,
            dealId = current.deals.find { it.clientId == clientId && it.stage != DealStage.CANCELLED }?.id,
            dateLabel = dateLabel,
            dayOfWeek = dayOfWeek,
            timeSlot = timeSlot,
            scheduledEpochMillis = System.currentTimeMillis() + hoursFromNow * 3_600_000L,
            status = BookingStatus.CONFIRMED,
            isPaid = current.deals.any { it.clientId == clientId && (it.stage == DealStage.PAID_BOOKED || it.stage == DealStage.IN_PROGRESS) },
            notes = notes.ifBlank { "Сессия восстановления сна" }
        )

        if (!repository.isConfigured()) {
            updateState(
                auditAction = "Создана запись в календаре",
                auditDetails = "$clientName · $dateLabel $timeSlot"
            ) { s -> s.copy(bookings = (s.bookings + booking).sortedBy { it.scheduledEpochMillis }) }
            emitFeedback("Запись добавлена в память. Укажите SUPABASE_ANON_KEY в Secrets для сохранения в Supabase.", BannerTone.WARNING)
            return null
        }

        viewModelScope.launch {
            _syncInfo.value = _syncInfo.value.copy(status = SupabaseSyncStatus.SYNCING, errorMessage = null)
            repository.insertBooking(booking)
                .onSuccess { saved ->
                    updateState(
                        auditAction = "Запись сохранена в Supabase",
                        auditDetails = "$clientName · $dateLabel $timeSlot (bookings)"
                    ) { s ->
                        s.copy(
                            bookings = (s.bookings.filterNot { it.id == saved.id } + saved)
                                .sortedBy { it.scheduledEpochMillis }
                        )
                    }
                    _syncInfo.value = _syncInfo.value.copy(
                        status = SupabaseSyncStatus.SYNCED,
                        lastSyncedAt = nowTime(),
                        remoteBookingsCount = _state.value.bookings.size
                    )
                    emitFeedback("Запись на $dateLabel $timeSlot сразу сохранена в Supabase (bookings)!", BannerTone.SUCCESS)
                }
                .onFailure { err ->
                    val msg = err.message ?: "Ошибка записи в таблицу bookings в Supabase"
                    _syncInfo.value = _syncInfo.value.copy(status = SupabaseSyncStatus.ERROR, errorMessage = msg)
                    emitFeedback(msg, BannerTone.ERROR)
                }
        }
        return null
    }

    fun cancelBooking(bookingId: String, refundToDeposit: Boolean, forceLateCancel: Boolean = false): Boolean {
        val current = _state.value
        val booking = current.bookings.find { it.id == bookingId } ?: return false
        val hrs = hoursUntil(booking)
        if (hrs < 24.0 && !forceLateCancel) {
            emitFeedback(
                "До сессии менее 24ч (${"%.0f".format(hrs.coerceAtLeast(0.0))} ч): бесплатная отмена недоступна!",
                BannerTone.WARNING
            )
            return false
        }
        val clientName = current.clients.find { it.id == booking.clientId }?.fullName ?: booking.clientId
        val resolutionLabel = if (refundToDeposit) "перенос на депозит" else "оформлен возврат"

        updateState(
            auditAction = "Отмена записи",
            auditDetails = "$clientName (${booking.dateLabel} ${booking.timeSlot}) · $resolutionLabel, слот и турбаза освобождены"
        ) { s ->
            val updatedBookings = s.bookings.map {
                if (it.id == bookingId) it.copy(status = BookingStatus.CANCELLED) else it
            }
            val updatedBases = if (booking.tourbaseId != null) {
                s.tourbases.map { tb ->
                    if (tb.id == booking.tourbaseId) tb.copy(bookedSeats = (tb.bookedSeats - 1).coerceAtLeast(0)) else tb
                }
            } else s.tourbases

            val updatedPayments = if (booking.isPaid && booking.dealId != null) {
                s.payments.map { p ->
                    if (p.dealId == booking.dealId) {
                        p.copy(
                            status = if (refundToDeposit) PaymentStatus.DEPOSIT else PaymentStatus.REFUNDED,
                            paidAmountRub = if (refundToDeposit) p.paidAmountRub else 0
                        )
                    } else p
                }
            } else s.payments

            val updatedDeals = if (booking.dealId != null) {
                s.deals.map { d ->
                    if (d.id == booking.dealId) d.copy(stage = DealStage.CANCELLED, updatedAt = nowDate()) else d
                }
            } else s.deals

            s.copy(
                bookings = updatedBookings,
                tourbases = updatedBases,
                payments = updatedPayments,
                deals = updatedDeals
            )
        }

        if (repository.isConfigured()) {
            viewModelScope.launch {
                repository.updateBookingStatus(bookingId, BookingStatus.CANCELLED)
                    .onSuccess {
                        emitFeedback("Отмена синхронизирована с Supabase ($resolutionLabel). Слот освобождён.", BannerTone.SUCCESS)
                    }
                    .onFailure { err ->
                        emitFeedback(err.message ?: "Ошибка обновления статуса в Supabase", BannerTone.ERROR)
                    }
            }
        } else {
            emitFeedback("Запись отменена ($resolutionLabel). Слот освобождён.", BannerTone.SUCCESS)
        }
        return true
    }

    // --- Finance, 0% Installments & Auto-Payment Simulation ---

    fun markInstallmentStepPaid(paymentId: String, stepId: String) {
        val current = _state.value
        val payment = current.payments.find { it.id == paymentId } ?: return
        val step = payment.installments.find { it.id == stepId } ?: return
        if (step.status == InstallmentStepStatus.PAID) return

        val clientName = current.clients.find { it.id == payment.clientId }?.fullName ?: payment.clientId
        updateState(
            auditAction = "Взнос по рассрочке 0%",
            auditDetails = "$clientName · платёж №${step.number} (${formatRub(step.amountRub)})"
        ) { s ->
            s.copy(
                payments = s.payments.map { p ->
                    if (p.id != paymentId) p else {
                        val nextSteps = p.installments.map { st ->
                            if (st.id == stepId) st.copy(status = InstallmentStepStatus.PAID) else st
                        }
                        val newPaid = nextSteps.filter { it.status == InstallmentStepStatus.PAID }.sumOf { it.amountRub }
                        val allPaid = nextSteps.all { it.status == InstallmentStepStatus.PAID }
                        val anyOverdue = nextSteps.any { it.status == InstallmentStepStatus.OVERDUE }
                        p.copy(
                            paidAmountRub = newPaid,
                            installments = nextSteps,
                            status = when {
                                allPaid -> PaymentStatus.PAID
                                anyOverdue -> PaymentStatus.OVERDUE
                                else -> PaymentStatus.INSTALLMENT
                            }
                        )
                    }
                }
            )
        }
        emitFeedback("Взнос №${step.number} (${formatRub(step.amountRub)}) отмечен оплаченным", BannerTone.SUCCESS)
    }

    fun cancelPaidPayment(paymentId: String, toDeposit: Boolean) {
        val current = _state.value
        val payment = current.payments.find { it.id == paymentId } ?: return
        val clientName = current.clients.find { it.id == payment.clientId }?.fullName ?: payment.clientId
        val deal = current.deals.find { it.id == payment.dealId }
        val affectedBookingIds = current.bookings
            .filter { it.dealId == payment.dealId && it.status == BookingStatus.CONFIRMED }
            .map { it.id }

        updateState(
            auditAction = if (toDeposit) "Перенос оплаты на депозит" else "Оформлен возврат средств",
            auditDetails = "$clientName · ${payment.programTitle} (${formatRub(payment.totalAmountRub)})"
        ) { s ->
            val nextPayments = s.payments.map { p ->
                if (p.id == paymentId) {
                    p.copy(
                        status = if (toDeposit) PaymentStatus.DEPOSIT else PaymentStatus.REFUNDED,
                        paidAmountRub = if (toDeposit) p.paidAmountRub else 0
                    )
                } else p
            }
            val nextDeals = s.deals.map { d ->
                if (d.id == payment.dealId) d.copy(stage = DealStage.CANCELLED, updatedAt = nowDate()) else d
            }
            val nextBookings = s.bookings.map { b ->
                if (b.dealId == payment.dealId && b.status == BookingStatus.CONFIRMED) b.copy(status = BookingStatus.CANCELLED) else b
            }
            val nextBases = if (deal?.tourbaseId != null) {
                s.tourbases.map { tb ->
                    if (tb.id == deal.tourbaseId) tb.copy(bookedSeats = (tb.bookedSeats - 1).coerceAtLeast(0)) else tb
                }
            } else s.tourbases

            s.copy(payments = nextPayments, deals = nextDeals, bookings = nextBookings, tourbases = nextBases)
        }
        if (repository.isConfigured() && affectedBookingIds.isNotEmpty()) {
            viewModelScope.launch {
                affectedBookingIds.forEach { bkId -> repository.updateBookingStatus(bkId, BookingStatus.CANCELLED) }
            }
        }
        emitFeedback(
            if (toDeposit) "Сумма перенесена на депозит клиента, слот освобождён"
            else "Возврат оформлен, выручка пересчитана, слот и турбаза освобождены",
            BannerTone.SUCCESS
        )
    }

    fun simulateAutoAppPayment() {
        val current = _state.value
        val candidateDeal = current.deals.firstOrNull { d ->
            val cl = current.clients.find { it.id == d.clientId }
            (d.stage == DealStage.AWAITING_PAYMENT || d.stage == DealStage.CONSULTATION) &&
                (cl?.pdnConsentSigned == true)
        } ?: current.deals.firstOrNull { d ->
            current.clients.find { it.id == d.clientId }?.pdnConsentSigned == true
        } ?: return

        val client = current.clients.find { it.id == candidateDeal.clientId } ?: return
        val prog = current.programs.find { it.id == candidateDeal.programId }
        val newPay = PaymentRecord(
            id = newId("pay"),
            dealId = candidateDeal.id,
            clientId = client.id,
            programTitle = prog?.title ?: "Онлайн-курс «Здоровый сон»",
            totalAmountRub = candidateDeal.amountRub,
            paidAmountRub = candidateDeal.amountRub,
            status = PaymentStatus.PAID,
            date = nowDate(),
            isAutoFromApp = true,
            installments = emptyList()
        )
        updateState(
            auditAction = "Автооплата из приложения",
            auditDetails = "${client.fullName} · ${newPay.programTitle} (${formatRub(newPay.totalAmountRub)})"
        ) { s ->
            s.copy(
                payments = listOf(newPay) + s.payments,
                deals = s.deals.map {
                    if (it.id == candidateDeal.id) it.copy(stage = DealStage.PAID_BOOKED, updatedAt = nowDate()) else it
                }
            )
        }
        emitFeedback("Поступила автооплата из приложения: ${client.fullName} (${formatRub(newPay.totalAmountRub)})", BannerTone.SUCCESS)
    }

    // --- Catalog & VK Store ---

    fun upsertProgram(
        existingId: String?,
        title: String,
        format: ProgramFormat,
        priceRub: Int,
        durationDesc: String,
        modulesCount: Int,
        installmentAllowed: Boolean,
        description: String
    ) {
        val id = existingId ?: newId("prog")
        val prog = Program(
            id = id,
            title = title.trim(),
            format = format,
            priceRub = priceRub,
            durationDesc = durationDesc.trim(),
            modulesCount = modulesCount,
            installmentAllowed = installmentAllowed,
            archived = false,
            description = description.trim()
        )
        updateState(
            auditAction = if (existingId == null) "Создан курс" else "Изменён курс",
            auditDetails = "${prog.title} · ${formatRub(prog.priceRub)}"
        ) { s ->
            val next = if (existingId == null) s.programs + prog else s.programs.map { if (it.id == existingId) prog else it }
            s.copy(programs = next)
        }
        emitFeedback("Программа «${prog.title}» сохранена", BannerTone.SUCCESS)
    }

    fun toggleArchiveProgram(programId: String) {
        val prog = _state.value.programs.find { it.id == programId } ?: return
        val nextArchived = !prog.archived
        updateState(
            auditAction = if (nextArchived) "Курс архивирован" else "Курс возвращён из архива",
            auditDetails = prog.title
        ) { s ->
            s.copy(programs = s.programs.map { if (it.id == programId) it.copy(archived = nextArchived) else it })
        }
        emitFeedback(if (nextArchived) "Курс «${prog.title}» в архиве" else "Курс «${prog.title}» активен", BannerTone.INFO)
    }

    fun adjustVkProductStock(productId: String, delta: Int) {
        val product = _state.value.vkProducts.find { it.id == productId } ?: return
        val newStock = (product.stock + delta).coerceAtLeast(0)
        updateState(
            auditAction = "Остаток товара ВК",
            auditDetails = "${product.name}: ${product.stock} → $newStock шт."
        ) { s ->
            s.copy(vkProducts = s.vkProducts.map { if (it.id == productId) it.copy(stock = newStock) else it })
        }
    }

    fun createVkOrder(clientId: String, productId: String, quantity: Int) {
        val current = _state.value
        val product = current.vkProducts.find { it.id == productId } ?: return
        val client = current.clients.find { it.id == clientId } ?: return
        if (product.stock < quantity) {
            emitFeedback("Недостаточно остатка на складе ВК (${product.stock} шт.)", BannerTone.WARNING)
            return
        }
        val order = VkOrder(
            id = newId("vko"),
            clientId = clientId,
            productId = productId,
            quantity = quantity,
            totalRub = product.priceRub * quantity,
            date = nowDate(),
            status = "Новый"
        )
        updateState(
            auditAction = "Заказ товара ВК",
            auditDetails = "${client.fullName} · ${product.name} × $quantity (${formatRub(order.totalRub)})"
        ) { s ->
            s.copy(
                vkOrders = listOf(order) + s.vkOrders,
                vkProducts = s.vkProducts.map {
                    if (it.id == productId) it.copy(stock = it.stock - quantity, soldCount = it.soldCount + quantity) else it
                }
            )
        }
        emitFeedback("Заказ товара ВК оформлен", BannerTone.SUCCESS)
    }

    // --- Specialists ---

    fun addSpecialist(
        fullName: String,
        specialization: String,
        hasMedicalEducation: Boolean,
        workSchedule: String,
        phone: String,
        assignedProgramIds: List<String>
    ) {
        val sp = Specialist(
            id = newId("sp"),
            fullName = fullName.trim(),
            specialization = specialization.trim(),
            hasMedicalEducation = hasMedicalEducation,
            assignedProgramIds = assignedProgramIds.ifEmpty { listOf("prog_online") },
            workSchedule = workSchedule.trim(),
            rating = 5.0,
            completedSessions = 0,
            phone = phone.trim()
        )
        updateState(
            auditAction = "Добавлен специалист",
            auditDetails = "${sp.fullName} (медобразование: ${if (sp.hasMedicalEducation) "да" else "нет"})"
        ) { s ->
            s.copy(specialists = s.specialists + sp)
        }
        emitFeedback("Сотрудник ${sp.fullName} добавлен в команду", BannerTone.SUCCESS)
    }

    // --- Support Chat ---

    fun sendTicketReply(ticketId: String, text: String) {
        if (text.isBlank()) return
        val msg = SupportMessage(
            id = newId("m"),
            fromClient = false,
            text = text.trim(),
            timestamp = "Только что"
        )
        updateState { s ->
            s.copy(
                tickets = s.tickets.map { tk ->
                    if (tk.id != ticketId) tk else tk.copy(
                        status = if (tk.status == TicketStatus.NEW) TicketStatus.IN_WORK else tk.status,
                        lastUpdated = "Только что",
                        messages = tk.messages + msg
                    )
                }
            )
        }
    }

    fun updateTicketStatus(ticketId: String, status: TicketStatus) {
        val tk = _state.value.tickets.find { it.id == ticketId } ?: return
        updateState(
            auditAction = "Статус тикета чата",
            auditDetails = "«${tk.subject}» → ${status.label}"
        ) { s ->
            s.copy(tickets = s.tickets.map { if (it.id == ticketId) it.copy(status = status) else it })
        }
        emitFeedback("Тикет переведён в статус «${status.label}»", BannerTone.INFO)
    }

    // --- Push & VK Broadcasts ---

    fun sendPushCampaign(title: String, body: String, segment: AgeSegment?, type: PushType) {
        val current = _state.value
        val recipients = if (segment == null) current.clients.size else current.clients.count { it.segment == segment }
        val campaign = PushCampaign(
            id = newId("ps"),
            title = title.trim(),
            body = body.trim(),
            targetSegment = segment,
            type = type,
            sentAt = nowDateTime(),
            recipientCount = recipients.coerceAtLeast(1)
        )
        updateState(
            auditAction = "Отправлено пуш-уведомление",
            auditDetails = "«${campaign.title}» (${segment?.label ?: "Все сегменты"}, ${campaign.recipientCount} чел.)"
        ) { s ->
            s.copy(pushCampaigns = listOf(campaign) + s.pushCampaigns)
        }
        emitFeedback("Пуш отправлен (${campaign.recipientCount} получателей)", BannerTone.SUCCESS)
    }

    fun addVkBroadcastWithAutoPush(title: String, vkUrl: String, scheduledDateTime: String, description: String) {
        val cleanUrl = vkUrl.trim().ifBlank { DemoDataSeeder.VK_COMMUNITY_URL }
        val broadcast = VkBroadcast(
            id = newId("vb"),
            title = title.trim(),
            vkUrl = cleanUrl,
            scheduledDateTime = scheduledDateTime.trim(),
            description = description.trim(),
            pushCreated = true
        )
        val autoPush = PushCampaign(
            id = newId("ps"),
            title = "Эфир ВК: ${broadcast.title}",
            body = "${broadcast.scheduledDateTime} · ${broadcast.description}. Ссылка: $cleanUrl — добавьте в календарь!",
            targetSegment = null,
            type = PushType.VK_STREAM,
            sentAt = nowDateTime(),
            recipientCount = _state.value.clients.size,
            calendarEventTag = broadcast.scheduledDateTime
        )
        updateState(
            auditAction = "Добавлена трансляция ВК + анонс-пуш",
            auditDetails = "${broadcast.title} (${broadcast.scheduledDateTime})"
        ) { s ->
            s.copy(
                vkBroadcasts = listOf(broadcast) + s.vkBroadcasts,
                pushCampaigns = listOf(autoPush) + s.pushCampaigns
            )
        }
        emitFeedback("Эфир ВК добавлен и сформирован анонс-пуш с меткой календаря", BannerTone.SUCCESS)
    }

    // --- Reviews Moderation ---

    fun togglePublishReview(reviewId: String) {
        val rev = _state.value.reviews.find { it.id == reviewId } ?: return
        val nextPub = !rev.publishedInApp
        val clientName = _state.value.clients.find { it.id == rev.clientId }?.fullName ?: rev.clientId
        updateState(
            auditAction = if (nextPub) "Отзыв опубликован в приложении" else "Отзыв снят с публикации",
            auditDetails = "$clientName (${rev.rating}★)"
        ) { s ->
            s.copy(reviews = s.reviews.map { if (it.id == reviewId) it.copy(publishedInApp = nextPub) else it })
        }
        emitFeedback(
            if (nextPub) "Отзыв опубликован в клиентском приложении" else "Отзыв скрыт из приложения",
            BannerTone.SUCCESS
        )
    }

    // --- CSV Export Generators ---

    fun buildClientsCsv(): String {
        val s = _state.value
        val sb = StringBuilder("\uFEFF")
        sb.appendLine("ID;ФИО;Телефон;Возраст;Сегмент;Источник;Согласие 152-ФЗ;Дата ПДн;Балл теста;Гигиена сна %;Специалист")
        s.clients.forEach { cl ->
            val spName = s.specialists.find { it.id == cl.assignedSpecialistId }?.fullName ?: ""
            sb.appendLine(
                "${cl.id};\"${cl.fullName}\";${cl.phone};${cl.age};\"${cl.segment.label}\";\"${cl.source.label}\";${if (cl.pdnConsentSigned) "Да" else "Нет"};${cl.pdnConsentDate ?: "-"};${cl.expressTestScore};${cl.sleepDiary.adherencePercent}%;\"$spName\""
            )
        }
        return sb.toString()
    }

    fun buildFinanceCsv(): String {
        val s = _state.value
        val sb = StringBuilder("\uFEFF")
        sb.appendLine("ID;Дата;Клиент;Программа;Сумма;Оплачено;Статус;Автооплата из приложения")
        s.payments.forEach { p ->
            val clName = s.clients.find { it.id == p.clientId }?.fullName ?: p.clientId
            sb.appendLine(
                "${p.id};${p.date};\"$clName\";\"${p.programTitle}\";${p.totalAmountRub};${p.paidAmountRub};\"${p.status.label}\";${if (p.isAutoFromApp) "Да" else "Нет"}"
            )
        }
        return sb.toString()
    }
}
