package com.example.data.remote

import com.example.BuildConfig
import com.example.ai.ExtractedEvent
import com.example.ai.ExtractedSyllabus
import com.example.data.model.ActivityType
import com.example.data.model.Priority
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

interface AcademicApi {
    @Multipart
    @POST("api/v1/documents/extract")
    suspend fun extractDocuments(@Part files: List<MultipartBody.Part>): ExtractDocumentResponse
}

@JsonClass(generateAdapter = true)
data class ExtractDocumentResponse(
    val data: ExtractedSyllabusDto,
    val meta: ExtractDocumentMetaDto
)

@JsonClass(generateAdapter = true)
data class ExtractDocumentMetaDto(
    val fileName: String,
    val fileSize: Long,
    val eventsCount: Int
)

@JsonClass(generateAdapter = true)
data class ExtractedSyllabusDto(
    val documentTitle: String,
    val disciplineName: String,
    val disciplineCode: String = "",
    val professor: String = "",
    val term: String = "",
    val classroom: String = "",
    val schedule: String = "",
    val recommendedColorHex: String = "#2563EB",
    val events: List<ExtractedEventDto> = emptyList()
) {
    fun toDomain(): ExtractedSyllabus {
        val scheduleStartTime = Regex("\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b")
            .find(schedule)
            ?.value
            ?.padStart(5, '0')
            .orEmpty()
        return ExtractedSyllabus(
            documentTitle = documentTitle,
            disciplineName = disciplineName,
            disciplineCode = disciplineCode,
            professor = professor,
            term = term,
            classroom = classroom,
            schedule = schedule,
            recommendedColorHex = recommendedColorHex,
            events = events.map { dto ->
                dto.toDomain().let { event ->
                    if (event.time.isBlank() && scheduleStartTime.isNotBlank()) {
                        event.copy(time = scheduleStartTime)
                    } else {
                        event
                    }
                }
            }
        )
    }
}

@JsonClass(generateAdapter = true)
data class ExtractedEventDto(
    val title: String,
    val date: String,
    val time: String = "",
    val activityType: String,
    val priority: String,
    val description: String = "",
    val dueDate: String? = null
) {
    fun toDomain() = ExtractedEvent(
        title = title,
        date = date,
        time = time,
        activityType = runCatching { ActivityType.valueOf(activityType) }.getOrDefault(ActivityType.TRABALHO),
        priority = runCatching { Priority.valueOf(priority) }.getOrDefault(Priority.MEDIA),
        description = description,
        dueDate = dueDate
    )
}

object AcademicApiFactory {
    fun create(): AcademicApi {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val baseUrl = BuildConfig.BACKEND_BASE_URL.trim().let {
            if (it.endsWith('/')) it else "$it/"
        }
        val parsedBaseUrl = requireNotNull(baseUrl.toHttpUrlOrNull()) {
            "BACKEND_BASE_URL must be a valid HTTP(S) URL"
        }
        require(BuildConfig.DEBUG || parsedBaseUrl.isHttps) {
            "BACKEND_BASE_URL must use HTTPS in release builds"
        }

        return Retrofit.Builder()
            .baseUrl(parsedBaseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AcademicApi::class.java)
    }
}
