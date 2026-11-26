package com.example.data

import com.squareup.moshi.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit

const val SUPABASE_REST_URL = "https://fxqebsfraqngdqmfmrgy.supabase.co/rest/v1/"

data class SupabaseClientDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "full_name") val fullName: String? = null,
    @Json(name = "name") val legacyName: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "age") val age: Int? = null,
    @Json(name = "segment") val segment: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "pdn_consent_signed") val pdnConsentSigned: Boolean? = null,
    @Json(name = "pdn_consent_date") val pdnConsentDate: String? = null,
    @Json(name = "express_test_score") val expressTestScore: Int? = null,
    @Json(name = "recommended_program_id") val recommendedProgramId: String? = null,
    @Json(name = "assigned_specialist_id") val assignedSpecialistId: String? = null,
    @Json(name = "notes_json") val notesJson: String? = null,
    @Json(name = "sleep_diary_json") val sleepDiaryJson: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

data class SupabaseClientInsert(
    @Json(name = "id") val id: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "age") val age: Int,
    @Json(name = "segment") val segment: String,
    @Json(name = "source") val source: String,
    @Json(name = "pdn_consent_signed") val pdnConsentSigned: Boolean,
    @Json(name = "pdn_consent_date") val pdnConsentDate: String?,
    @Json(name = "express_test_score") val expressTestScore: Int,
    @Json(name = "recommended_program_id") val recommendedProgramId: String,
    @Json(name = "assigned_specialist_id") val assignedSpecialistId: String,
    @Json(name = "notes_json") val notesJson: String,
    @Json(name = "sleep_diary_json") val sleepDiaryJson: String
)

data class SupabaseBookingDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "client_id") val clientId: String? = null,
    @Json(name = "specialist_id") val specialistId: String? = null,
    @Json(name = "program_id") val programId: String? = null,
    @Json(name = "tourbase_id") val tourbaseId: String? = null,
    @Json(name = "deal_id") val dealId: String? = null,
    @Json(name = "date_label") val dateLabel: String? = null,
    @Json(name = "day_of_week") val dayOfWeek: String? = null,
    @Json(name = "time_slot") val timeSlot: String? = null,
    @Json(name = "scheduled_epoch_millis") val scheduledEpochMillis: Long? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "is_paid") val isPaid: Boolean? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

data class SupabaseBookingInsert(
    @Json(name = "id") val id: String,
    @Json(name = "client_id") val clientId: String,
    @Json(name = "specialist_id") val specialistId: String,
    @Json(name = "program_id") val programId: String,
    @Json(name = "tourbase_id") val tourbaseId: String?,
    @Json(name = "deal_id") val dealId: String?,
    @Json(name = "date_label") val dateLabel: String,
    @Json(name = "day_of_week") val dayOfWeek: String,
    @Json(name = "time_slot") val timeSlot: String,
    @Json(name = "scheduled_epoch_millis") val scheduledEpochMillis: Long,
    @Json(name = "status") val status: String,
    @Json(name = "is_paid") val isPaid: Boolean,
    @Json(name = "notes") val notes: String
)

data class SupabaseClientMinimalInsert(
    @Json(name = "id") val id: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "phone") val phone: String
)

data class SupabaseBookingMinimalInsert(
    @Json(name = "id") val id: String,
    @Json(name = "client_id") val clientId: String,
    @Json(name = "specialist_id") val specialistId: String,
    @Json(name = "date_label") val dateLabel: String,
    @Json(name = "time_slot") val timeSlot: String,
    @Json(name = "status") val status: String,
    @Json(name = "notes") val notes: String
)

data class SupabaseClientPdnPatch(
    @Json(name = "pdn_consent_signed") val pdnConsentSigned: Boolean,
    @Json(name = "pdn_consent_date") val pdnConsentDate: String?
)

data class SupabaseClientNotesPatch(
    @Json(name = "notes_json") val notesJson: String
)

data class SupabaseBookingStatusPatch(
    @Json(name = "status") val status: String
)

interface SupabaseRestApi {

    @GET("clients")
    suspend fun getClients(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SupabaseClientDto>>

    @GET("clients")
    suspend fun getClientsUnordered(
        @Query("select") select: String = "*"
    ): Response<List<SupabaseClientDto>>

    @POST("clients")
    suspend fun insertClient(
        @Header("Prefer") prefer: String = "return=representation",
        @Body payload: SupabaseClientInsert
    ): Response<List<SupabaseClientDto>>

    @POST("clients")
    suspend fun insertClientMinimal(
        @Header("Prefer") prefer: String = "return=representation",
        @Body payload: SupabaseClientMinimalInsert
    ): Response<List<SupabaseClientDto>>

    @POST("clients")
    suspend fun upsertClients(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
        @Body payload: List<SupabaseClientInsert>
    ): Response<List<SupabaseClientDto>>

    @PATCH("clients")
    suspend fun patchClientPdn(
        @Query("id") idFilter: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body patch: SupabaseClientPdnPatch
    ): Response<List<SupabaseClientDto>>

    @PATCH("clients")
    suspend fun patchClientNotes(
        @Query("id") idFilter: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body patch: SupabaseClientNotesPatch
    ): Response<List<SupabaseClientDto>>

    @GET("bookings")
    suspend fun getBookings(
        @Query("select") select: String = "*",
        @Query("order") order: String = "scheduled_epoch_millis.asc"
    ): Response<List<SupabaseBookingDto>>

    @GET("bookings")
    suspend fun getBookingsUnordered(
        @Query("select") select: String = "*"
    ): Response<List<SupabaseBookingDto>>

    @POST("bookings")
    suspend fun insertBooking(
        @Header("Prefer") prefer: String = "return=representation",
        @Body payload: SupabaseBookingInsert
    ): Response<List<SupabaseBookingDto>>

    @POST("bookings")
    suspend fun insertBookingMinimal(
        @Header("Prefer") prefer: String = "return=representation",
        @Body payload: SupabaseBookingMinimalInsert
    ): Response<List<SupabaseBookingDto>>

    @POST("bookings")
    suspend fun upsertBookings(
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
        @Body payload: List<SupabaseBookingInsert>
    ): Response<List<SupabaseBookingDto>>

    @PATCH("bookings")
    suspend fun patchBookingStatus(
        @Query("id") idFilter: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body patch: SupabaseBookingStatusPatch
    ): Response<List<SupabaseBookingDto>>
}

object SupabaseClientFactory {
    fun create(apiKey: String, moshi: Moshi = defaultMoshi()): SupabaseRestApi {
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $apiKey")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }

        val okHttp = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(SUPABASE_REST_URL)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseRestApi::class.java)
    }

    fun defaultMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
}
