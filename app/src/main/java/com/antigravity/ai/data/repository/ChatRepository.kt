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

    suspend fun fetchModelsConfig(): Result<ModelsConfigResponse> = apiService.getModelsConfig()

    suspend fun fetchSkills(): Result<SkillsResponse> = apiService.getSkills()

    suspend fun fetchUsage(): Result<UsageResponse> = apiService.getUsage()

    suspend fun uploadFile(name: String, base64: String, type: String): Result<UploadResponse> =
        apiService.uploadFile(name, base64, type)

    suspend fun fetchVaultFiles(): Result<VaultResponse> = apiService.getVaultFiles()

    suspend fun fetchVaultFileContent(relPath: String): Result<VaultFileContent> =
        apiService.getVaultFileContent(relPath)

    suspend fun saveVaultNote(relPath: String?, title: String?, content: String): Result<Unit> =
        apiService.saveVaultNote(relPath, title, content)

    suspend fun createVaultFolder(folderPath: String): Result<Unit> =
        apiService.createVaultFolder(folderPath)

    suspend fun deleteVaultFile(relPath: String): Result<Unit> =
        apiService.deleteVaultFile(relPath)

    suspend fun fetchSession(): Result<SessionResponse> = apiService.getSession()

    suspend fun sendMessage(
        prompt: String,
        continueChat: Boolean = true,
        settings: ChatSettings = ChatSettings(),
        attachments: List<Attachment> = emptyList()
    ): Result<Unit> = apiService.sendPrompt(prompt, continueChat, settings, attachments)

    suspend fun startNewChat(): Result<SessionResponse> = apiService.newChat()

    suspend fun stopExecution(): Result<Unit> = apiService.stopGeneration()

    suspend fun fetchAuthStatus(): Result<AuthStatusResponse> = apiService.getAuthStatus()

    suspend fun submitAuthToken(token: String): Result<AuthTokenResponse> = apiService.submitAuthToken(token)
}
