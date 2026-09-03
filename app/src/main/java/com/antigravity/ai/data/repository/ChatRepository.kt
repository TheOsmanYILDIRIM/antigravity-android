package com.antigravity.ai.data.repository

import com.antigravity.ai.data.api.*
import com.antigravity.ai.data.model.*
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val backend: ChatBackend) {

    fun observeStreamEvents(): Flow<StreamEvent> = backend.observeEvents()

    suspend fun fetchConversations(): Result<ConversationsResponse> = backend.getConversations()

    suspend fun loadConversation(id: String): Result<SessionResponse> = backend.loadConversation(id)

    suspend fun deleteConversation(id: String): Result<Unit> = backend.deleteConversation(id)

    suspend fun fetchModelsConfig(): Result<ModelsConfigResponse> = backend.getModelsConfig()

    suspend fun fetchSkills(): Result<SkillsResponse> = backend.getSkills()

    suspend fun fetchUsage(): Result<UsageResponse> = backend.getUsage()

    suspend fun uploadFile(name: String, base64: String, type: String): Result<UploadResponse> =
        backend.uploadFile(name, base64, type)

    suspend fun fetchVaultFiles(): Result<VaultResponse> = backend.fetchVaultFiles()
    suspend fun fetchVaultFileContent(relPath: String): Result<VaultFileContent> =
        backend.fetchVaultFileContent(relPath)

    suspend fun saveVaultNote(relPath: String?, title: String?, content: String): Result<Unit> =
        backend.saveVaultNote(relPath, title, content)

    suspend fun createVaultFolder(folderPath: String): Result<Unit> = backend.createVaultFolder(folderPath)
    suspend fun deleteVaultFile(relPath: String): Result<Unit> = backend.deleteVaultFile(relPath)
    suspend fun fetchFsList(dir: String? = null): Result<FsListResponse> = backend.getFsList(dir)
    suspend fun fetchFsProjects(): Result<FsProjectsResponse> = backend.getFsProjects()
    suspend fun fetchFsContent(path: String): Result<FsContentResponse> = backend.getFsContent(path)
    suspend fun saveFsFile(path: String, content: String): Result<FsSaveResponse> = backend.saveFsFile(path, content)
    suspend fun fetchSession(): Result<SessionResponse> = backend.fetchSession()

    suspend fun sendMessage(
        prompt: String,
        conversationId: String? = null,
        continueChat: Boolean = true,
        settings: ChatSettings = ChatSettings(),
        attachments: List<Attachment> = emptyList()
    ): Result<Unit> = backend.sendPrompt(prompt, conversationId, continueChat, settings, attachments)

    suspend fun startNewChat(): Result<SessionResponse> = backend.newChat()

    suspend fun stopExecution(): Result<Unit> = backend.stopGeneration()

    suspend fun replyPermission(
        sessionID: String,
        requestID: String,
        allow: Boolean,
        always: Boolean
    ): Result<Unit> = backend.replyPermission(sessionID, requestID, allow, always)

    suspend fun replyQuestion(
        sessionID: String,
        requestID: String,
        answers: List<String>
    ): Result<Unit> = backend.replyQuestion(sessionID, requestID, answers)

    suspend fun fetchAuthStatus(): Result<AuthStatusResponse> = backend.fetchAuthStatus()
    suspend fun startAgLogin(): Result<AgLoginResponse> = backend.startAgLogin()
    suspend fun submitAuthCode(code: String): Result<AgLoginCodeResponse> = backend.submitAuthCode(code)
}
