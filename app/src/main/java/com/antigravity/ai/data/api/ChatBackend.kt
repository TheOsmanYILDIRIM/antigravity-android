package com.antigravity.ai.data.api

import com.antigravity.ai.data.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Ortak backend arayüzü. UI/ViewModel bu arayüze bağımlıdır; hangi arka uç
 * (agy CLI veya opencode) olduğundan haberdar değildir. Her backend, kendi
 * protokolünü buradaki ortak model ve [StreamEvent] tiplerine çevirir.
 */
interface ChatBackend {
    fun observeEvents(): Flow<StreamEvent>

    suspend fun getConversations(): Result<ConversationsResponse>
    suspend fun loadConversation(id: String): Result<SessionResponse>
    suspend fun deleteConversation(id: String): Result<Unit>
    suspend fun getModelsConfig(): Result<ModelsConfigResponse>
    suspend fun getSkills(): Result<SkillsResponse>
    suspend fun getUsage(): Result<UsageResponse>

    suspend fun sendPrompt(
        prompt: String,
        continueChat: Boolean = true,
        settings: ChatSettings = ChatSettings(),
        attachments: List<Attachment> = emptyList()
    ): Result<Unit>

    suspend fun newChat(): Result<SessionResponse>
    suspend fun stopGeneration(): Result<Unit>

    // --- opencode'a özgü, agy backend'inde no-op ---

    suspend fun replyPermission(
        sessionID: String,
        requestID: String,
        allow: Boolean,
        always: Boolean
    ): Result<Unit> = Result.success(Unit)

    suspend fun replyQuestion(
        sessionID: String,
        requestID: String,
        answers: List<String>
    ): Result<Unit> = Result.success(Unit)

    // --- agy'ye özgü (opencode backend'inde varsayılan olarak desteklenmez) ---

    suspend fun uploadFile(name: String, base64: String, type: String): Result<UploadResponse> =
        Result.failure(UnsupportedOperationException("upload desteklenmiyor"))

    suspend fun fetchVaultFiles(): Result<VaultResponse> =
        Result.failure(UnsupportedOperationException("vault desteklenmiyor"))

    suspend fun fetchVaultFileContent(relPath: String): Result<VaultFileContent> =
        Result.failure(UnsupportedOperationException("vault desteklenmiyor"))

    suspend fun saveVaultNote(relPath: String?, title: String?, content: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("vault desteklenmiyor"))

    suspend fun createVaultFolder(folderPath: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("vault desteklenmiyor"))

    suspend fun deleteVaultFile(relPath: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("vault desteklenmiyor"))

    suspend fun fetchSession(): Result<SessionResponse> = Result.success(SessionResponse("ok", null, false))

    suspend fun fetchAuthStatus(): Result<AuthStatusResponse> =
        Result.success(AuthStatusResponse("ok", true, "none"))
}

/** opencode tarafından sorulan izin isteği (StreamEvent.PermissionRequested ile taşınır). */
data class PermissionRequestData(
    val id: String,
    val sessionID: String,
    val action: String,
    val resources: List<String> = emptyList(),
    val messageID: String? = null,
    val callID: String? = null
)

/** opencode tarafından sorulan kullanıcı sorusu (StreamEvent.QuestionRequested ile taşınır). */
data class QuestionRequestData(
    val id: String,
    val sessionID: String,
    val questions: List<String> = emptyList(),
    val tool: String? = null
)
