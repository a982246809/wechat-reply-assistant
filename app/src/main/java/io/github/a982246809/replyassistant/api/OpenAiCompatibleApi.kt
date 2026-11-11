package io.github.a982246809.replyassistant.api

import io.github.a982246809.replyassistant.dto.ChatCompletionRequest
import io.github.a982246809.replyassistant.dto.ChatCompletionResponse
import io.github.a982246809.replyassistant.dto.ModelListResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface OpenAiCompatibleApi {
    @GET
    suspend fun listModels(
        @Url url: String,
        @Header("Authorization") authorization: String?
    ): ModelListResponse

    @POST
    suspend fun chatCompletions(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body body: ChatCompletionRequest
    ): ChatCompletionResponse
}
