package com.discipline.app.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.discipline.app.BuildConfig
import com.discipline.app.data.model.DailyReflection
import com.discipline.app.data.model.Habit
import com.discipline.app.data.model.HabitCategory
import com.discipline.app.data.model.HabitLog
import com.discipline.app.data.model.HabitType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SupabaseService(private val context: Context) {

    private val tag = "SupabaseService"
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Supabase URL & Public Anon Key (can be loaded from BuildConfig or overridden in Settings)
    var supabaseUrl: String
        get() = prefs.getString("custom_url", null) ?: BuildConfig.SUPABASE_URL.trimEnd('/')
        set(value) = prefs.edit().putString("custom_url", value.trimEnd('/')).apply()

    var supabaseAnonKey: String
        get() = prefs.getString("custom_anon_key", null) ?: BuildConfig.SUPABASE_ANON_KEY
        set(value) = prefs.edit().putString("custom_anon_key", value).apply()

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()

    var accessToken: String?
        get() = prefs.getString("access_token", null)
        private set(value) = prefs.edit().putString("access_token", value).apply()

    var currentUserId: String?
        get() = prefs.getString("user_id", null)
        private set(value) = prefs.edit().putString("user_id", value).apply()

    var currentUserEmail: String?
        get() = prefs.getString("user_email", null)
        private set(value) = prefs.edit().putString("user_email", value).apply()

    val isUserAuthenticated: Boolean
        get() = !accessToken.isNullOrBlank() && !currentUserId.isNullOrBlank()

    // --------------------------------------------------------------------------
    // AUTHENTICATION (GoTrue /auth/v1 REST API)
    // --------------------------------------------------------------------------

    suspend fun signUp(email: String, pass: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(Exception("Supabase is not configured yet. See SUPABASE_SETUP.md"))
        }

        val url = "$supabaseUrl/auth/v1/signup"
        val payload = JSONObject().apply {
            put("email", email.trim())
            put("password", pass)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val errorMsg = try { JSONObject(body).optString("msg", JSONObject(body).optString("error_description", body)) } catch (_: Exception) { body }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(body)
            val token = json.optString("access_token", null)
            val userObj = json.optJSONObject("user") ?: json
            val uid = userObj.optString("id", "")
            val userEmail = userObj.optString("email", email)

            if (!token.isNullOrBlank()) {
                accessToken = token
                currentUserId = uid
                currentUserEmail = userEmail
            }

            Result.success(uid to userEmail)
        } catch (e: Exception) {
            Log.e(tag, "signUp failed", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, pass: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(Exception("Supabase is not configured yet. See SUPABASE_SETUP.md"))
        }

        val url = "$supabaseUrl/auth/v1/token?grant_type=password"
        val payload = JSONObject().apply {
            put("email", email.trim())
            put("password", pass)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val errorMsg = try { JSONObject(body).optString("error_description", JSONObject(body).optString("msg", body)) } catch (_: Exception) { body }
                return@withContext Result.failure(Exception(errorMsg))
            }

            val json = JSONObject(body)
            val token = json.getString("access_token")
            val userObj = json.getJSONObject("user")
            val uid = userObj.getString("id")
            val userEmail = userObj.optString("email", email)

            accessToken = token
            currentUserId = uid
            currentUserEmail = userEmail

            Result.success(uid to userEmail)
        } catch (e: Exception) {
            Log.e(tag, "signIn failed", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(Exception("Supabase is not configured yet."))
        }

        val url = "$supabaseUrl/auth/v1/recover"
        val payload = JSONObject().apply {
            put("email", email.trim())
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                return@withContext Result.failure(Exception(body))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        prefs.edit()
            .remove("access_token")
            .remove("user_id")
            .remove("user_email")
            .apply()
    }

    // --------------------------------------------------------------------------
    // DATABASE CRUD (PostgREST /rest/v1 REST API with RLS protection)
    // --------------------------------------------------------------------------

    suspend fun uploadHabit(habit: Habit): Result<Unit> = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext Result.failure(Exception("User not authenticated"))
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Missing user id"))

        val url = "$supabaseUrl/rest/v1/habits"
        val json = JSONObject().apply {
            put("id", habit.id)
            put("user_id", uid)
            put("title", habit.title)
            put("description", habit.description)
            put("category", habit.category.name)
            if (habit.customCategory != null) put("custom_category", habit.customCategory)
            put("type", habit.type.name)
            put("target_value", habit.targetValue)
            put("unit", habit.unit)
            put("scheduled_days_csv", habit.scheduledDays.sorted().joinToString(","))
            if (habit.reminderHour != null) put("reminder_hour", habit.reminderHour)
            if (habit.reminderMinute != null) put("reminder_minute", habit.reminderMinute)
            put("is_paused", habit.isPaused)
            put("is_archived", habit.isArchived)
            put("created_at", habit.createdAt)
            put("updated_at", habit.updatedAt)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                return@withContext Result.failure(Exception("Supabase uploadHabit error: $err"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteHabit(habitId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext Result.failure(Exception("User not authenticated"))
        val url = "$supabaseUrl/rest/v1/habits?id=eq.$habitId"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .delete()
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                return@withContext Result.failure(Exception("Supabase deleteHabit error: $err"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadHabitLog(log: HabitLog): Result<Unit> = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext Result.failure(Exception("User not authenticated"))
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Missing user id"))

        val url = "$supabaseUrl/rest/v1/daily_logs"
        val json = JSONObject().apply {
            put("id", log.id)
            put("habit_id", log.habitId)
            put("user_id", uid)
            put("date", log.date)
            put("progress_value", log.progressValue)
            put("is_completed", log.isCompleted)
            put("updated_at", log.updatedAt)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                return@withContext Result.failure(Exception("Supabase uploadHabitLog error: $err"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadReflection(reflection: DailyReflection): Result<Unit> = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext Result.failure(Exception("User not authenticated"))
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Missing user id"))

        val url = "$supabaseUrl/rest/v1/reflections"
        val json = JSONObject().apply {
            put("id", "${uid}_${reflection.date}")
            put("user_id", uid)
            put("date", reflection.date)
            put("reflection_text", reflection.reflectionText)
            put("mood_rating", reflection.moodRating)
            put("updated_at", reflection.updatedAt)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Prefer", "resolution=merge-duplicates")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val err = response.body?.string().orEmpty()
                return@withContext Result.failure(Exception("Supabase uploadReflection error: $err"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchHabitLogsFromCloud(): Result<List<HabitLog>> = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext Result.failure(Exception("User not authenticated"))
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Missing user id"))
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/daily_logs?select=*&order=updated_at.asc")
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
        try {
            val response = client.newCall(request).execute()
            val body = response.use { it.body?.string().orEmpty() }
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch daily logs: $body"))
            }
            val array = JSONArray(body)
            val logs = buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(HabitLog(
                        id = obj.optString("id", ""),
                        habitId = obj.getString("habit_id"),
                        ownerUid = uid,
                        date = obj.getString("date"),
                        progressValue = obj.optDouble("progress_value", 0.0),
                        isCompleted = obj.optBoolean("is_completed", false),
                        updatedAt = obj.optLong("updated_at", 0L)
                    ))
                }
            }
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchReflectionsFromCloud(): Result<List<DailyReflection>> = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext Result.failure(Exception("User not authenticated"))
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Missing user id"))
        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/reflections?select=*&order=updated_at.asc")
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
        try {
            val response = client.newCall(request).execute()
            val body = response.use { it.body?.string().orEmpty() }
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch reflections: $body"))
            }
            val array = JSONArray(body)
            val reflections = buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(DailyReflection(
                        date = obj.getString("date"),
                        ownerUid = uid,
                        reflectionText = obj.optString("reflection_text", ""),
                        moodRating = obj.optInt("mood_rating", 0),
                        updatedAt = obj.optLong("updated_at", 0L)
                    ))
                }
            }
            Result.success(reflections)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchHabitsFromCloud(): Result<List<Habit>> = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext Result.failure(Exception("User not authenticated"))
        val uid = currentUserId ?: return@withContext Result.failure(Exception("Missing user id"))

        val url = "$supabaseUrl/rest/v1/habits?select=*&order=created_at.asc"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to fetch habits: $body"))
            }

            val array = JSONArray(body)
            val habits = mutableListOf<Habit>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val daysCsv = obj.optString("scheduled_days_csv", "1,2,3,4,5,6,7")
                val days = daysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

                habits.add(
                    Habit(
                        id = obj.getString("id"),
                        ownerUid = uid,
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        category = HabitCategory.fromString(obj.optString("category", "PERSONAL")),
                        customCategory = if (obj.has("custom_category")) obj.getString("custom_category") else null,
                        type = try { HabitType.valueOf(obj.optString("type", "YES_NO")) } catch (_: Exception) { HabitType.YES_NO },
                        targetValue = obj.optDouble("target_value", 1.0),
                        unit = obj.optString("unit", ""),
                        scheduledDays = days,
                        reminderHour = if (obj.has("reminder_hour") && !obj.isNull("reminder_hour")) obj.getInt("reminder_hour") else null,
                        reminderMinute = if (obj.has("reminder_minute") && !obj.isNull("reminder_minute")) obj.getInt("reminder_minute") else null,
                        isPaused = obj.optBoolean("is_paused", false),
                        isArchived = obj.optBoolean("is_archived", false),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
            Result.success(habits)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserAccount(): Result<Unit> = withContext(Dispatchers.IO) {
        val token = accessToken
            ?: return@withContext Result.failure(Exception("User not authenticated"))
        if (!isConfigured) {
            return@withContext Result.failure(Exception("Supabase is not configured"))
        }

        val request = Request.Builder()
            .url("$supabaseUrl/rest/v1/rpc/delete_user_account")
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .post("{}".toRequestBody(jsonMediaType))
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.use { it.body?.string().orEmpty() }
            if (!response.isSuccessful) {
                // Never report success or silently fall back to deleting only data rows.
                return@withContext Result.failure(
                    Exception("Account deletion failed (${response.code}). Verify the Supabase delete_user_account() function and permissions. $body")
                )
            }
            // Clear the local session only after the server confirms account deletion.
            signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            // Keep the session intact so the user can retry; do not falsely imply deletion succeeded.
            Result.failure(Exception("Account deletion request failed: ${e.message ?: "network error"}", e))
        }
    }
}
