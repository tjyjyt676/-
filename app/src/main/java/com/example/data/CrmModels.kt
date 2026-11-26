package com.example.data

enum class UserRole(val title: String, val subtitle: String, val shortLabel: String) {
    ADMIN("Администратор", "Руководитель центра", "Админ"),
    MANAGER("Менеджер", "Отдел продаж", "Менеджер"),
    SPECIALIST("Специалист", "Ведущий сессий", "Специалист")
}

enum class CrmSection(
    val route: String,
    val title: String,
    val shortTitle: String,
    val allowedRoles: Set<UserRole>
) {
    DASHBOARD("dashboard", "Сводка", "Сводка", setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.SPECIALIST)),
    KANBAN("kanban", "Воронка продаж", "Воронка", setOf(UserRole.ADMIN, UserRole.MANAGER)),
    CLIENTS("clients", "Клиенты 360°", "Клиенты", setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.SPECIALIST)),
    CALENDAR("calendar", "Календарь", "Календарь", setOf(UserRole.ADMIN, UserRole.MANAGER, UserRole.SPECIALIST)),
    TOURBASES("tourbases", "12 турбаз", "Турбазы", setOf(UserRole.ADMIN, UserRole.MANAGER)),
    CATALOG("catalog", "Программы и ВК", "Каталог", setOf(UserRole.ADMIN, UserRole.MANAGER)),
    FINANCE("finance", "Финансы и 0%", "Финансы", setOf(UserRole.ADMIN, UserRole.MANAGER)),
    TEAM("team", "Специалисты", "Команда", setOf(UserRole.ADMIN, UserRole.MANAGER)),
    CHAT("chat", "Чат поддержки", "Чат", setOf(UserRole.ADMIN, UserRole.MANAGER)),
    PUSH_VK("push_vk", "Пуши и ВК-эфиры", "Пуши/ВК", setOf(UserRole.ADMIN)),
    REVIEWS("reviews", "Отзывы и CSI", "Отзывы", setOf(UserRole.ADMIN, UserRole.SPECIALIST)),
    ANALYTICS("analytics", "Аналитика и CSV", "Аналитика", setOf(UserRole.ADMIN)),
    AUDIT("audit", "Аудит и права", "Аудит", setOf(UserRole.ADMIN))
}

enum class DashboardPeriod(val label: String, val multiplier: Double) {
    DAY("День", 0.08),
    WEEK("Неделя", 0.32),
    MONTH("Месяц", 1.0),
    YEAR("Год", 9.6)
}

enum class AgeSegment(val label: String, val shortLabel: String) {
    WOMEN_40_59("Женщины 40–59 лет", "40–59"),
    SENIORS_60_PLUS("Пожилые 60–70+ лет", "60–70+"),
    PROS_25_45("Специалисты 25–45 лет", "25–45")
}

enum class LeadSource(val label: String) {
    MOBILE_APP("Приложение"),
    VK("Сообщество ВК"),
    SITE("Сайт")
}

enum class DealStage(val label: String, val shortLabel: String, val requiresPdnConsent: Boolean) {
    NEW_LEAD("Новая заявка (приложение / ВК / сайт)", "Новая заявка", false),
    TEST_PASSED("Прошел экспресс-тест", "Экспресс-тест", false),
    CONSULTATION("Консультация / Подбор программы", "Консультация", false),
    AWAITING_PAYMENT("Ожидает оплаты / Рассрочка 0%", "Ожидает оплаты", true),
    PAID_BOOKED("Оплачено и забронировано", "Оплачено", true),
    IN_PROGRESS("Проходит программу", "Проходит", true),
    COMPLETED("Завершено", "Завершено", false),
    CANCELLED("Отмена / Возврат", "Отмена", false)
}

enum class ProgramFormat(val label: String) {
    ONLINE("Онлайн-курс"),
    GROUP_TOURBASE("Групповой интенсив (турбаза)"),
    VIP_TOURBASE("VIP-интенсив (медобразование)")
}

enum class BookingStatus(val label: String) {
    CONFIRMED("Подтверждено"),
    COMPLETED("Проведено"),
    CANCELLED("Отменено")
}

enum class TourbaseStatus(val label: String) {
    AVAILABLE("Свободно"),
    PARTIAL("Частично занято"),
    FULL("Мест нет")
}

enum class PaymentStatus(val label: String) {
    PAID("Оплачено"),
    INSTALLMENT("Рассрочка 0%"),
    OVERDUE("Просрочка"),
    REFUNDED("Возврат"),
    DEPOSIT("Депозит")
}

enum class InstallmentStepStatus(val label: String) {
    PAID("Оплачен"),
    UPCOMING("Ближайший"),
    OVERDUE("Просрочен")
}

enum class TicketStatus(val label: String) {
    NEW("Новый"),
    IN_WORK("В работе"),
    CLOSED("Закрыт")
}

enum class PushType(val label: String) {
    REMINDER_24H("Напоминание за 24ч"),
    SEGMENT_MANUAL("Рассылка по сегменту"),
    VK_STREAM("Анонс ВК-трансляции")
}

data class SleepDiaryProgress(
    val noScreenBeforeBedDays: Int,
    val roomVentilationDays: Int,
    val wakeUpScheduleDays: Int,
    val totalTrackedDays: Int,
    val avgSleepHours: Double
) {
    val adherencePercent: Int
        get() {
            if (totalTrackedDays <= 0) return 0
            val avg = (noScreenBeforeBedDays + roomVentilationDays + wakeUpScheduleDays) / 3.0
            return ((avg / totalTrackedDays) * 100).toInt().coerceIn(0, 100)
        }
}

data class SpecialistNote(
    val id: String,
    val specialistId: String,
    val specialistName: String,
    val date: String,
    val text: String
)

data class Client(
    val id: String,
    val fullName: String,
    val phone: String,
    val age: Int,
    val segment: AgeSegment,
    val source: LeadSource,
    val pdnConsentSigned: Boolean,
    val pdnConsentDate: String?,
    val expressTestScore: Int, // 0..100 (higher = deeper nervous exhaustion)
    val recommendedProgramId: String,
    val sleepDiary: SleepDiaryProgress,
    val assignedSpecialistId: String,
    val specialistNotes: List<SpecialistNote>,
    val createdAt: String
)

data class Deal(
    val id: String,
    val clientId: String,
    val programId: String,
    val amountRub: Int,
    val stage: DealStage,
    val isInstallment: Boolean,
    val tourbaseId: String?,
    val specialistId: String,
    val createdAt: String,
    val updatedAt: String,
    val notes: String
)

data class Specialist(
    val id: String,
    val fullName: String,
    val specialization: String,
    val hasMedicalEducation: Boolean,
    val assignedProgramIds: List<String>,
    val workSchedule: String,
    val rating: Double,
    val completedSessions: Int,
    val phone: String
)

data class Booking(
    val id: String,
    val clientId: String,
    val specialistId: String,
    val programId: String,
    val tourbaseId: String?,
    val dealId: String?,
    val dateLabel: String,      // e.g. "20.11.2026"
    val dayOfWeek: String,      // e.g. "Пт"
    val timeSlot: String,       // e.g. "10:00"
    val scheduledEpochMillis: Long,
    val status: BookingStatus,
    val isPaid: Boolean,
    val notes: String
)

data class Tourbase(
    val id: String,
    val name: String,
    val district: String,
    val capacity: Int,
    val bookedSeats: Int,
    val contactName: String,
    val contactPhone: String,
    val availableDates: List<String>,
    val supportsGroup: Boolean,
    val supportsVip: Boolean,
    val nextIntensiveType: String
) {
    val status: TourbaseStatus
        get() = when {
            bookedSeats >= capacity -> TourbaseStatus.FULL
            bookedSeats > 0 -> TourbaseStatus.PARTIAL
            else -> TourbaseStatus.AVAILABLE
        }
    val freeSeats: Int get() = (capacity - bookedSeats).coerceAtLeast(0)
}

data class Program(
    val id: String,
    val title: String,
    val format: ProgramFormat,
    val priceRub: Int,
    val durationDesc: String,
    val modulesCount: Int,
    val installmentAllowed: Boolean,
    val archived: Boolean,
    val description: String
)

data class VkProduct(
    val id: String,
    val name: String,
    val category: String,
    val priceRub: Int,
    val stock: Int,
    val soldCount: Int,
    val vkUrl: String
)

data class VkOrder(
    val id: String,
    val clientId: String,
    val productId: String,
    val quantity: Int,
    val totalRub: Int,
    val date: String,
    val status: String
)

data class InstallmentStep(
    val id: String,
    val number: Int,
    val dueDate: String,
    val amountRub: Int,
    val status: InstallmentStepStatus
)

data class PaymentRecord(
    val id: String,
    val dealId: String,
    val clientId: String,
    val programTitle: String,
    val totalAmountRub: Int,
    val paidAmountRub: Int,
    val status: PaymentStatus,
    val date: String,
    val isAutoFromApp: Boolean,
    val installments: List<InstallmentStep>
)

data class SupportMessage(
    val id: String,
    val fromClient: Boolean,
    val text: String,
    val timestamp: String
)

data class SupportTicket(
    val id: String,
    val clientId: String,
    val subject: String,
    val status: TicketStatus,
    val lastUpdated: String,
    val messages: List<SupportMessage>
)

data class PushCampaign(
    val id: String,
    val title: String,
    val body: String,
    val targetSegment: AgeSegment?,
    val type: PushType,
    val sentAt: String,
    val recipientCount: Int,
    val calendarEventTag: String? = null
)

data class VkBroadcast(
    val id: String,
    val title: String,
    val vkUrl: String,
    val scheduledDateTime: String,
    val description: String,
    val pushCreated: Boolean
)

data class Review(
    val id: String,
    val clientId: String,
    val specialistId: String,
    val programId: String,
    val rating: Int,
    val impression: String,
    val remarks: String,
    val date: String,
    val publishedInApp: Boolean
)

data class AuditLogEntry(
    val id: String,
    val timestamp: String,
    val actorRole: UserRole,
    val actorName: String,
    val action: String,
    val details: String
)

data class CrmUiSettings(
    val darkTheme: Boolean = true,
    val largeFont: Boolean = false,
    val currentRole: UserRole = UserRole.ADMIN,
    val activeSpecialistId: String = "sp_1"
)

data class CrmDatabaseState(
    val settings: CrmUiSettings,
    val programs: List<Program>,
    val specialists: List<Specialist>,
    val tourbases: List<Tourbase>,
    val clients: List<Client>,
    val deals: List<Deal>,
    val bookings: List<Booking>,
    val vkProducts: List<VkProduct>,
    val vkOrders: List<VkOrder>,
    val payments: List<PaymentRecord>,
    val tickets: List<SupportTicket>,
    val pushCampaigns: List<PushCampaign>,
    val vkBroadcasts: List<VkBroadcast>,
    val reviews: List<Review>,
    val auditLog: List<AuditLogEntry>
)

fun formatRub(amount: Int): String {
    val absStr = kotlin.math.abs(amount).toString()
    val formatted = absStr.reversed().chunked(3).joinToString(" ").reversed()
    return if (amount < 0) "-$formatted ₽" else "$formatted ₽"
}
