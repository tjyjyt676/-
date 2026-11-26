package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.UUID

class CrmRepository {

    private val moshi = SupabaseClientFactory.defaultMoshi()
    private val notesListAdapter = moshi.adapter<List<SpecialistNote>>(
        Types.newParameterizedType(List::class.java, SpecialistNote::class.java)
    )
    private val sleepDiaryAdapter = moshi.adapter(SleepDiaryProgress::class.java)

    private val safeIdRegex = Regex("^[A-Za-z0-9_-]{1,64}$")

    private val apiKey: String = BuildConfig.SUPABASE_ANON_KEY.trim()

    private val api: SupabaseRestApi? = if (isConfigured()) {
        SupabaseClientFactory.create(apiKey, moshi)
    } else {
        null
    }

    fun isConfigured(): Boolean {
        return apiKey.isNotBlank() && apiKey != "MY_SUPABASE_ANON_KEY"
    }

    private fun validateIdFilter(id: String): String {
        require(safeIdRegex.matches(id)) { "Некорректный идентификатор записи" }
        return "eq.$id"
    }

    private fun <T> Response<T>.extractError(table: String): String {
        val raw = runCatching { errorBody()?.string() }.getOrNull().orEmpty().take(240)
        return "Supabase ($table) HTTP ${code()}${if (raw.isNotBlank()) ": $raw" else ""}"
    }

    suspend fun fetchClientsAndBookings(): Result<Pair<List<Client>, List<Booking>>> = withContext(Dispatchers.IO) {
        val service = api ?: return@withContext Result.failure(
            IllegalStateException("Добавьте SUPABASE_ANON_KEY в панель Secrets в AI Studio")
        )
        runCatching {
            var clientsResp = service.getClients()
            if (!clientsResp.isSuccessful && clientsResp.code() == 400) {
                clientsResp = service.getClientsUnordered()
            }
            if (!clientsResp.isSuccessful) {
                error(clientsResp.extractError("clients"))
            }

            var bookingsResp = service.getBookings()
            if (!bookingsResp.isSuccessful && bookingsResp.code() == 400) {
                bookingsResp = service.getBookingsUnordered()
            }
            if (!bookingsResp.isSuccessful) {
                error(bookingsResp.extractError("bookings"))
            }

            val clients = clientsResp.body().orEmpty().map { it.toDomain() }
            val bookings = bookingsResp.body().orEmpty().map { it.toDomain() }
            clients to bookings
        }
    }

    suspend fun insertClient(client: Client): Result<Client> = withContext(Dispatchers.IO) {
        val service = api ?: return@withContext Result.failure(
            IllegalStateException("Добавьте SUPABASE_ANON_KEY в панель Secrets в AI Studio")
        )
        runCatching {
            val payload = client.toInsertDto()
            var resp = service.insertClient(payload = payload)
            if (!resp.isSuccessful && (resp.code() == 400 || resp.code() == 404)) {
                val minimal = SupabaseClientMinimalInsert(
                    id = client.id,
                    fullName = client.fullName,
                    phone = client.phone
                )
                val fallbackResp = service.insertClientMinimal(payload = minimal)
                if (fallbackResp.isSuccessful) {
                    resp = fallbackResp
                }
            }
            if (!resp.isSuccessful) {
                error(resp.extractError("clients"))
            }
            resp.body()?.firstOrNull()?.toDomain(fallback = client) ?: client
        }
    }

    suspend fun updateClientPdn(clientId: String, signed: Boolean, date: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            val service = api ?: return@withContext Result.failure(
                IllegalStateException("Добавьте SUPABASE_ANON_KEY в панель Secrets в AI Studio")
            )
            runCatching {
                val filter = validateIdFilter(clientId)
                val resp = service.patchClientPdn(
                    idFilter = filter,
                    patch = SupabaseClientPdnPatch(pdnConsentSigned = signed, pdnConsentDate = date)
                )
                if (!resp.isSuccessful) error(resp.extractError("clients"))
            }
        }

    suspend fun updateClientNotes(clientId: String, notes: List<SpecialistNote>): Result<Unit> =
        withContext(Dispatchers.IO) {
            val service = api ?: return@withContext Result.failure(
                IllegalStateException("Добавьте SUPABASE_ANON_KEY в панель Secrets в AI Studio")
            )
            runCatching {
                val filter = validateIdFilter(clientId)
                val json = notesListAdapter.toJson(notes)
                val resp = service.patchClientNotes(
                    idFilter = filter,
                    patch = SupabaseClientNotesPatch(notesJson = json)
                )
                if (!resp.isSuccessful) error(resp.extractError("clients"))
            }
        }

    suspend fun insertBooking(booking: Booking): Result<Booking> = withContext(Dispatchers.IO) {
        val service = api ?: return@withContext Result.failure(
            IllegalStateException("Добавьте SUPABASE_ANON_KEY в панель Secrets в AI Studio")
        )
        runCatching {
            val payload = booking.toInsertDto()
            var resp = service.insertBooking(payload = payload)
            if (!resp.isSuccessful && (resp.code() == 400 || resp.code() == 404)) {
                val minimal = SupabaseBookingMinimalInsert(
                    id = booking.id,
                    clientId = booking.clientId,
                    specialistId = booking.specialistId,
                    dateLabel = booking.dateLabel,
                    timeSlot = booking.timeSlot,
                    status = booking.status.name,
                    notes = booking.notes
                )
                val fallbackResp = service.insertBookingMinimal(payload = minimal)
                if (fallbackResp.isSuccessful) {
                    resp = fallbackResp
                }
            }
            if (!resp.isSuccessful) {
                error(resp.extractError("bookings"))
            }
            resp.body()?.firstOrNull()?.toDomain(fallback = booking) ?: booking
        }
    }

    suspend fun updateBookingStatus(bookingId: String, status: BookingStatus): Result<Unit> =
        withContext(Dispatchers.IO) {
            val service = api ?: return@withContext Result.failure(
                IllegalStateException("Добавьте SUPABASE_ANON_KEY в панель Secrets в AI Studio")
            )
            runCatching {
                val filter = validateIdFilter(bookingId)
                val resp = service.patchBookingStatus(
                    idFilter = filter,
                    patch = SupabaseBookingStatusPatch(status = status.name)
                )
                if (!resp.isSuccessful) error(resp.extractError("bookings"))
            }
        }

    suspend fun seedDemoToSupabase(clients: List<Client>, bookings: List<Booking>): Result<Pair<Int, Int>> =
        withContext(Dispatchers.IO) {
            val service = api ?: return@withContext Result.failure(
                IllegalStateException("Добавьте SUPABASE_ANON_KEY в панель Secrets в AI Studio")
            )
            runCatching {
                val cResp = service.upsertClients(payload = clients.map { it.toInsertDto() })
                if (!cResp.isSuccessful) error(cResp.extractError("clients"))

                val bResp = service.upsertBookings(payload = bookings.map { it.toInsertDto() })
                if (!bResp.isSuccessful) error(bResp.extractError("bookings"))

                (cResp.body()?.size ?: clients.size) to (bResp.body()?.size ?: bookings.size)
            }
        }

    private fun Client.toInsertDto(): SupabaseClientInsert = SupabaseClientInsert(
        id = id,
        fullName = fullName,
        phone = phone,
        age = age,
        segment = segment.name,
        source = source.name,
        pdnConsentSigned = pdnConsentSigned,
        pdnConsentDate = pdnConsentDate,
        expressTestScore = expressTestScore,
        recommendedProgramId = recommendedProgramId,
        assignedSpecialistId = assignedSpecialistId,
        notesJson = notesListAdapter.toJson(specialistNotes),
        sleepDiaryJson = sleepDiaryAdapter.toJson(sleepDiary)
    )

    private fun Booking.toInsertDto(): SupabaseBookingInsert = SupabaseBookingInsert(
        id = id,
        clientId = clientId,
        specialistId = specialistId,
        programId = programId,
        tourbaseId = tourbaseId,
        dealId = dealId,
        dateLabel = dateLabel,
        dayOfWeek = dayOfWeek,
        timeSlot = timeSlot,
        scheduledEpochMillis = scheduledEpochMillis,
        status = status.name,
        isPaid = isPaid,
        notes = notes
    )

    private fun SupabaseClientDto.toDomain(fallback: Client? = null): Client {
        val parsedSegment = runCatching { segment?.let { AgeSegment.valueOf(it) } }.getOrNull()
            ?: fallback?.segment
            ?: AgeSegment.WOMEN_40_59
        val parsedSource = runCatching { source?.let { LeadSource.valueOf(it) } }.getOrNull()
            ?: fallback?.source
            ?: LeadSource.MOBILE_APP
        val parsedNotes = notesJson?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { notesListAdapter.fromJson(raw) }.getOrNull()
        } ?: fallback?.specialistNotes ?: emptyList()
        val parsedDiary = sleepDiaryJson?.takeIf { it.isNotBlank() }?.let { raw ->
            runCatching { sleepDiaryAdapter.fromJson(raw) }.getOrNull()
        } ?: fallback?.sleepDiary ?: SleepDiaryProgress(5, 7, 6, 10, 6.3)

        return Client(
            id = id ?: fallback?.id ?: UUID.randomUUID().toString(),
            fullName = fullName ?: legacyName ?: fallback?.fullName ?: "Клиент",
            phone = phone ?: fallback?.phone ?: "",
            age = age ?: fallback?.age ?: 40,
            segment = parsedSegment,
            source = parsedSource,
            pdnConsentSigned = pdnConsentSigned ?: fallback?.pdnConsentSigned ?: false,
            pdnConsentDate = pdnConsentDate ?: fallback?.pdnConsentDate,
            expressTestScore = expressTestScore ?: fallback?.expressTestScore ?: 65,
            recommendedProgramId = recommendedProgramId ?: fallback?.recommendedProgramId ?: "prog_online",
            sleepDiary = parsedDiary,
            assignedSpecialistId = assignedSpecialistId ?: fallback?.assignedSpecialistId ?: "sp_1",
            specialistNotes = parsedNotes,
            createdAt = createdAt?.take(10) ?: fallback?.createdAt ?: "Сегодня"
        )
    }

    private fun SupabaseBookingDto.toDomain(fallback: Booking? = null): Booking {
        val parsedStatus = runCatching { status?.let { BookingStatus.valueOf(it) } }.getOrNull()
            ?: fallback?.status
            ?: BookingStatus.CONFIRMED
        val defaultEpoch = System.currentTimeMillis() + 48L * 3_600_000L
        return Booking(
            id = id ?: fallback?.id ?: UUID.randomUUID().toString(),
            clientId = clientId ?: fallback?.clientId ?: "",
            specialistId = specialistId ?: fallback?.specialistId ?: "sp_1",
            programId = programId ?: fallback?.programId ?: "prog_online",
            tourbaseId = tourbaseId ?: fallback?.tourbaseId,
            dealId = dealId ?: fallback?.dealId,
            dateLabel = dateLabel ?: fallback?.dateLabel ?: "28.11.2026",
            dayOfWeek = dayOfWeek ?: fallback?.dayOfWeek ?: "Сб",
            timeSlot = timeSlot ?: fallback?.timeSlot ?: "14:00",
            scheduledEpochMillis = scheduledEpochMillis ?: fallback?.scheduledEpochMillis ?: defaultEpoch,
            status = parsedStatus,
            isPaid = isPaid ?: fallback?.isPaid ?: false,
            notes = notes ?: fallback?.notes ?: ""
        )
    }
}
