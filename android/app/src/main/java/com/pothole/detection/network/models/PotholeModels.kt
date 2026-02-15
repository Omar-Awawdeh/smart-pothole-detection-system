package com.pothole.detection.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PotholeResponse(
    val id: String,
    val isDuplicate: Boolean = false,
    val existingId: String? = null,
    val confirmationCount: Int? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val confidence: Double? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val status: String? = null,
    val severity: String? = null,
    @SerialName("detected_at") val detectedAt: String? = null
)

@Serializable
data class AuthTokens(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val user: UserInfo,
    val tokens: AuthTokens
)

@Serializable
data class UserInfo(
    val id: String,
    val email: String,
    val name: String,
    val role: String
)
