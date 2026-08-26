package com.antigravity.ai.data.repository

import com.antigravity.ai.data.api.AntigravityApiService
import com.antigravity.ai.data.api.StreamEvent
import com.antigravity.ai.data.model.*
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val apiService: AntigravityApiService = AntigravityApiService()) {

    fun observeStreamEvents(): Flow<StreamEvent> = apiService.observeEvents()

    suspend fun fetchConversations(): Result<ConversationsResponse> = apiService.getConversations()

    suspend fun loadConversation(id: String): Result<SessionResponse> = apiService.loadConversation(id)

    suspend fun deleteConversation(id: String): Result<Unit> = apiService.deleteConversation(id)

    suspend fun fetchSession(): Result<SessionResponse> = apiService.getSession()

    suspend fun fetchVaultFiles(): Result<VaultResponse> = apiService.getVaultFiles()

    suspend fun sendMessage(
        prompt: String,
        continueChat: Boolean = true,
        settings: ChatSettings = ChatSettings(),
        attachments: List<Attachment> = emptyList()
    ): Result<Unit> = apiService.sendPrompt(prompt, continueChat, settings, attachments)

    suspend fun startNewChat(): Result<SessionResponse> = apiService.newChat()

    suspend fun stopExecution(): Result<Unit> = apiService.stopGeneration()
}
