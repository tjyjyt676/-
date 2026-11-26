package com.example.data

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class CrmRepository(context: Context) {

    private val storageFile = File(context.filesDir, "kringe_crm_state_v1.json")

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(CrmDatabaseState::class.java)

    suspend fun loadState(): CrmDatabaseState = withContext(Dispatchers.IO) {
        if (storageFile.exists()) {
            runCatching {
                val json = storageFile.readText(Charsets.UTF_8)
                adapter.fromJson(json)
            }.getOrNull()?.let { return@withContext it }
        }
        val initial = DemoDataSeeder.createInitialState()
        saveStateSync(initial)
        initial
    }

    suspend fun saveState(state: CrmDatabaseState) = withContext(Dispatchers.IO) {
        saveStateSync(state)
    }

    suspend fun resetDemoData(preserveTheme: Boolean, currentSettings: CrmUiSettings): CrmDatabaseState =
        withContext(Dispatchers.IO) {
            val fresh = DemoDataSeeder.createInitialState()
            val updated = if (preserveTheme) {
                fresh.copy(
                    settings = fresh.settings.copy(
                        darkTheme = currentSettings.darkTheme,
                        largeFont = currentSettings.largeFont,
                        currentRole = currentSettings.currentRole
                    )
                )
            } else {
                fresh
            }
            saveStateSync(updated)
            updated
        }

    private fun saveStateSync(state: CrmDatabaseState) {
        runCatching {
            val tmp = File(storageFile.parentFile, "${storageFile.name}.tmp")
            tmp.writeText(adapter.toJson(state), Charsets.UTF_8)
            tmp.renameTo(storageFile)
        }
    }
}
