package com.pothole.detection.network

import android.content.SharedPreferences
import com.pothole.detection.network.models.LoginRequest
import com.pothole.detection.network.models.LoginResponse
import com.pothole.detection.network.models.PotholeResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiService(private val sharedPreferences: SharedPreferences) {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        install(Logging) {
            level = LogLevel.HEADERS
        }
    }

    private var accessToken: String? = null

    private fun baseUrl(): String {
        val configured = sharedPreferences.getString(KEY_API_BASE_URL, DEFAULT_API_BASE_URL)
            ?.trim()
            .orEmpty()

        if (configured.isBlank()) return DEFAULT_API_BASE_URL

        val withScheme = if (configured.startsWith("http://") || configured.startsWith("https://")) {
            configured
        } else {
            "https://$configured"
        }

        return withScheme.trimEnd('/')
    }

    fun setAccessToken(token: String) {
        accessToken = token
    }

    fun hasAccessToken(): Boolean = accessToken != null

    fun clearAccessToken() {
        accessToken = null
    }

    suspend fun uploadPothole(
        imageBytes: ByteArray,
        latitude: Double,
        longitude: Double,
        confidence: Float,
        vehicleId: String,
        timestamp: Long
    ): Result<PotholeResponse> {
        return try {
            val response = client.submitFormWithBinaryData(
                url = "${baseUrl()}/api/potholes",
                formData = formData {
                    append("image", imageBytes, Headers.build {
                        append(HttpHeaders.ContentType, "image/jpeg")
                        append(
                            HttpHeaders.ContentDisposition,
                            "filename=\"pothole_${System.currentTimeMillis()}.jpg\""
                        )
                    })
                    append("latitude", latitude.toString())
                    append("longitude", longitude.toString())
                    append("confidence", confidence.toString())
                    append("vehicleId", vehicleId)
                    append("timestamp", timestamp.toString())
                }
            ) {
                accessToken?.let { bearerAuth(it) }
            }

            if (response.status.isSuccess()) {
                Result.success(response.body<PotholeResponse>())
            } else {
                Result.failure(Exception("Upload failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<LoginResponse> {
        return try {
            val response = client.post("${baseUrl()}/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email, password))
            }

            if (response.status.isSuccess()) {
                val loginResponse: LoginResponse = response.body()
                accessToken = loginResponse.tokens.accessToken
                Result.success(loginResponse)
            } else {
                Result.failure(Exception("Login failed: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val DEFAULT_API_BASE_URL = "https://api.potholesystem.tech"
    }
}
