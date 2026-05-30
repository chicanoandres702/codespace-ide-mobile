package com.codespace.ide.data

import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable data class LoginRequest(val email: String, val password: String, val deviceId: String)
@Serializable data class AuthResponse(val accessToken: String, val refreshToken: String, val accessTokenExpiresIn: Int)
@Serializable data class RefreshRequest(val refreshToken: String)
@Serializable data class RepoDto(val id: Long, val name: String, val fullName: String, val defaultBranch: String, val private: Boolean)
@Serializable data class CreatePrRequest(val title: String, val head: String, val base: String, val body: String? = null)
@Serializable data class PullRequestDto(val number: Int, val title: String, val state: String, val url: String)

/** Backend REST surface used by the app (mirrors docs/03-api-design.md). */
interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthResponse

    @GET("github/repos")
    suspend fun listRepos(): List<RepoDto>

    @POST("github/repos/{owner}/{repo}/pulls")
    suspend fun createPr(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreatePrRequest,
    ): PullRequestDto
}
