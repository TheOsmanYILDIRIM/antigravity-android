package com.antigravity.ai.data.api

import com.antigravity.ai.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Mevcut agy CLI sunucusunu saran backend. Tüm metotlar doğrudan
 * [AntigravityApiService]'e delege edilir; davranış değişmez.
 */
class AgyBackend(private val api: AntigravityApiService = AntigravityApiService()) : ChatBackend {

    override fun observeEvents(): Flow<StreamEvent> = api.observeEvents()

    override suspend fun getConversations(): Result<ConversationsResponse> = api.getConversations()
    override suspend fun loadConversation(id: String): Result<SessionResponse> = api.loadConversation(id)
    override suspend fun deleteConversation(id: String): Result<Unit> = api.deleteConversation(id)
    override suspend fun getModelsConfig(): Result<ModelsConfigResponse> = api.getModelsConfig()
    override suspend fun getSkills(): Result<SkillsResponse> = api.getSkills()
    override suspend fun getUsage(): Result<UsageResponse> = api.getUsage()

    override suspend fun sendPrompt(
        prompt: String,
        continueChat: Boolean,
        settings: ChatSettings,
        attachments: List<Attachment>
    ): Result<Unit> = api.sendPrompt(prompt, continueChat, settings, attachments)

    override suspend fun newChat(): Result<SessionResponse> = api.newChat()
    override suspend fun stopGeneration(): Result<Unit> = api.stopGeneration()

    override suspend fun uploadFile(name: String, base64: String, type: String): Result<UploadResponse> =
        api.uploadFile(name, base64, type)

    override suspend fun fetchVaultFiles(): Result<VaultResponse> = api.getVaultFiles()
    override suspend fun fetchVaultFileContent(relPath: String): Result<VaultFileContent> =
        api.getVaultFileContent(relPath)

    override suspend fun saveVaultNote(relPath: String?, title: String?, content: String): Result<Unit> =
        api.saveVaultNote(relPath, title, content)

    override suspend fun createVaultFolder(folderPath: String): Result<Unit> =
        api.createVaultFolder(folderPath)

    override suspend fun deleteVaultFile(relPath: String): Result<Unit> = api.deleteVaultFile(relPath)
    override suspend fun fetchSession(): Result<SessionResponse> = api.getSession()
    override suspend fun fetchAuthStatus(): Result<AuthStatusResponse> = api.getAuthStatus()
    override suspend fun submitAuthToken(token: String): Result<AuthTokenResponse> = api.submitAuthToken(token)
    override suspend fun startAgLogin(): Result<AgLoginResponse> = api.startAgLogin()
    override suspend fun submitAuthCode(code: String): Result<AgLoginCodeResponse> = api.submitAuthCode(code)
}
