package com.antigravity.ai.data.repository

import com.antigravity.ai.data.api.AntigravityApiService
import com.antigravity.ai.data.api.StreamEvent
import com.antigravity.ai.data.model.*
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val apiService: AntigravityApiService = AntigravityApiService()) {

    fun observeStreamEvents(): Flow<StreamEvent> = apiService.observeEvents()

    suspend fun fetchSession(): Result<SessionResponse> = apiService.getSession()

    suspend fun sendMessage(prompt: String, continueChat: Boolean = true): Result<Unit> =
        apiService.sendPrompt(prompt, continueChat)

    suspend fun startNewChat(): Result<Unit> = apiService.newChat()

    suspend fun stopExecution(): Result<Unit> = apiService.stopGeneration()
}
