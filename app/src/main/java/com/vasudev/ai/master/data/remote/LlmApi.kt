package com.vasudev.ai.master.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WireMessage(val role: String, val content: String)

@Serializable
data class CompletionRequest(
    val model: String,
    val messages: List<WireMessage>,
    val stream: Boolean = true,
    val temperature: Double = 0.7,
    val max_tokens: Int = 2048
)

@Serializable
data class StreamChunk(val choices: List<ChunkChoice> = emptyList()) {
    @Serializable
    data class ChunkChoice(val delta: Delta = Delta())
    @Serializable
    data class Delta(val content: String? = null)
}

@Serializable
data class CompleteResponse(val choices: List<CompleteChoice> = emptyList()) {
    @Serializable
    data class CompleteChoice(val message: WireMessage? = null)
}

/**
 * Streaming client for any OpenAI-compatible chat/completions endpoint
 * (OpenAI, Groq, OpenRouter, Together, Ollama on LAN, etc.).
 */
@Singleton
class LlmApi @Inject constructor(
    private val client: HttpClient,
    private val json: Json
) {

    fun streamChat(baseUrl: String, apiKey: String, request: CompletionRequest): Flow<String> =
        callbackFlow {
            val body = json.encodeToString(request)
            client.preparePost("$baseUrl/chat/completions") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                header(HttpHeaders.Accept, "text/event-stream")
                header(HttpHeaders.ContentType, "application/json")
                setBody(body)
            }.execute { response: HttpResponse ->
                if (!response.status.isSuccess()) {
                    val detail = runCatching { response.bodyAsText().take(300) }.getOrDefault("")
                    close(RuntimeException("Server error ${response.status.value}: $detail"))
                    return@execute
                }
                val channel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: break
                    if (line.startsWith("data:")) {
                        val payload = line.removePrefix("data:").trim()
                        if (payload == "[DONE]") break
                        runCatching { json.decodeFromString<StreamChunk>(payload) }
                            .getOrNull()
                            ?.choices
                            ?.firstOrNull()
                            ?.delta
                            ?.content
                            ?.let { trySend(it) }
                    }
                }
            }
            close()
            awaitClose { }
        }

    suspend fun completeOnce(baseUrl: String, apiKey: String, request: CompletionRequest): String {
        val response = client.preparePost("$baseUrl/chat/completions") {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header(HttpHeaders.ContentType, "application/json")
            setBody(json.encodeToString(request.copy(stream = false)))
        }.execute { it.bodyAsText() }
        val parsed = runCatching { json.decodeFromString<CompleteResponse>(response) }.getOrNull()
        return parsed?.choices?.firstOrNull()?.message?.content.orEmpty()
    }
}
