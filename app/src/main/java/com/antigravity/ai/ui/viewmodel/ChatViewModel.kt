package com.antigravity.ai.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.ai.data.api.*
import com.antigravity.ai.data.model.*
import com.antigravity.ai.data.repository.ChatRepository
import com.antigravity.ai.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val conversations: List<ConversationMeta> = emptyList(),
    val currentSessionId: String? = null,
    val currentConversationId: String? = null,
    val inputText: String = "",
    val pastedBlocks: List<PastedBlock> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val settings: ChatSettings = ChatSettings(),
    val availableModels: List<ModelItem> = emptyList(),
    val availableEfforts: List<EffortItem> = emptyList(),
    val vaultFiles: List<VaultItem> = emptyList(),
    val installedSkills: List<SkillItem> = emptyList(),
    val usage: UsageData? = null,
    val activeVaultFileContent: String? = null,
    val activeVaultFilePath: String? = null,
    val isUploadingAttachment: Boolean = false,
    val isGenerating: Boolean = false,
    val isListening: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showAuthDialog: Boolean = false,
    val isAuthenticated: Boolean = true,
    val authMethod: String = "oauth",
    val showVaultManager: Boolean = false,
    val showUsageDetail: Boolean = false,
    val showSlashCommands: Boolean = false,
    val slashQuery: String = "",
    val showMentions: Boolean = false,
    val mentionQuery: String = "",
    val errorMessage: String? = null,
    val notice: String? = null,
    val activeBackend: String = "agy",
    val pendingPermission: PermissionRequestData? = null,
    val pendingQuestion: QuestionRequestData? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("agy_settings", android.content.Context.MODE_PRIVATE)
    private lateinit var repository: ChatRepository
    private var eventsJob: Job? = null
    private val _uiState = MutableStateFlow(ChatUiState(settings = loadSavedSettings()))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        initSpeechRecognizer()
        viewModelScope.launch(Dispatchers.IO) {
            val name = resolveBackendName()
            repository = ChatRepository(buildBackend(name))
            _uiState.update { it.copy(activeBackend = name) }
            startEventCollection()
            refreshAll()
        }
    }

    private fun loadBackendPref(): String = prefs.getString("backend", "auto") ?: "auto"

    private fun resolveBackendName(): String {
        val pref = loadBackendPref()
        return if (pref == "auto") detectBackend() else pref
    }

    private fun buildBackend(name: String): ChatBackend = when (name) {
        "opencode" -> OpenCodeBackend()
        else -> AgyBackend()
    }

    /** Açık olan sunucuyu bulur: :8080 (agy) veya :4096 (opencode). İkisi de açıksa agy tercih edilir. */
    private fun detectBackend(): String {
        val agy = isPortOpen("127.0.0.1", 8080)
        val oc = isPortOpen("127.0.0.1", 4096)
        return when {
            agy -> "agy"
            oc -> "opencode"
            else -> "agy"
        }
    }

    private fun isPortOpen(host: String, port: Int): Boolean = runCatching {
        java.net.Socket().use { s -> s.connect(java.net.InetSocketAddress(host, port), 600) }
    }.isSuccess

    /** Backend'i değiştirir (agy <-> opencode) ve olay akışını yeniden başlatır. */
    fun setBackend(name: String) {
        prefs.edit().putString("backend", name).apply()
        viewModelScope.launch(Dispatchers.IO) {
            val resolved = if (name == "auto") detectBackend() else name
            repository = ChatRepository(buildBackend(resolved))
            _uiState.update { it.copy(activeBackend = resolved, messages = emptyList(), isGenerating = false) }
            startEventCollection()
            refreshAll()
        }
    }

    private fun loadSavedSettings(): ChatSettings {
        val model = prefs.getString("model", "gemini-3.7-flash-medium") ?: "gemini-3.7-flash-medium"
        val effort = prefs.getString("effort", "default") ?: "default"
        val mode = prefs.getString("mode", "default") ?: "default"
        val useVault = prefs.getBoolean("useVault", true)
        val fontSizeSp = prefs.getFloat("fontSizeSp", 13.5f)
        val thermalMode = prefs.getString("thermalMode", "eco") ?: "eco"
        val notificationsEnabled = prefs.getBoolean("notificationsEnabled", true)
        return ChatSettings(model, effort, mode, useVault, fontSizeSp, thermalMode, notificationsEnabled)
    }

    private fun saveSettings(settings: ChatSettings) {
        prefs.edit()
            .putString("model", settings.model)
            .putString("effort", settings.effort)
            .putString("mode", settings.mode)
            .putBoolean("useVault", settings.useVault)
            .putFloat("fontSizeSp", settings.fontSizeSp)
            .putString("thermalMode", settings.thermalMode)
            .putBoolean("notificationsEnabled", settings.notificationsEnabled)
            .apply()
    }

    fun refreshAll() {
        fetchConversations()
        syncWithServer()
        fetchVaultFiles()
        fetchSkills()
        fetchUsage()
        fetchModelsConfig()
        fetchAuthStatus()
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication()).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _uiState.update { it.copy(isListening = true) }
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        _uiState.update { it.copy(isListening = false) }
                    }
                    override fun onError(error: Int) {
                        _uiState.update { it.copy(isListening = false) }
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0]
                            _uiState.update {
                                val current = it.inputText
                                val updated = if (current.isEmpty()) recognized else "$current $recognized"
                                it.copy(inputText = updated, isListening = false)
                            }
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _uiState.update { it.copy(isListening = false) }
    }

    private fun startEventCollection() {
        eventsJob?.cancel()
        eventsJob = viewModelScope.launch {
            repository.observeStreamEvents().collect { event ->
                when (event) {
                    is StreamEvent.Init -> {
                        _uiState.update {
                            it.copy(
                                currentSessionId = event.conversationId,
                                currentConversationId = event.conversationId
                            )
                        }
                        fetchConversations()
                    }
                    is StreamEvent.Chunk -> {
                        _uiState.update { state ->
                            val list = state.messages.toMutableList()
                            if (list.isNotEmpty() && list.last().role == "bot") {
                                val last = list.last().copy(
                                    content = event.fullContent,
                                    state = MessageState.GENERATING
                                )
                                list[list.size - 1] = last
                            }
                            state.copy(messages = list, isGenerating = true)
                        }
                    }
                    is StreamEvent.ToolUpdate -> {
                        _uiState.update { state ->
                            val list = state.messages.toMutableList()
                            if (list.isNotEmpty() && list.last().role == "bot") {
                                val botMsg = list.last()
                                val tools = botMsg.tools.toMutableList()
                                val idx = tools.indexOfFirst { it.stepIndex == event.tool.stepIndex }
                                if (idx >= 0) {
                                    tools[idx] = event.tool
                                } else {
                                    tools.add(event.tool)
                                }
                                list[list.size - 1] = botMsg.copy(tools = tools)
                            }
                            state.copy(messages = list)
                        }
                    }
                    is StreamEvent.Done -> {
                        _uiState.update { state ->
                            val list = state.messages.toMutableList()
                            if (list.isNotEmpty() && list.last().role == "bot") {
                                val last = list.last()
                                val updated = last.copy(
                                    content = event.botMessage?.content ?: last.content,
                                    tools = (event.botMessage?.tools ?: last.tools).toMutableList(),
                                    usage = event.botMessage?.usage ?: last.usage,
                                    state = MessageState.DONE
                                )
                                list[list.size - 1] = updated
                            }
                            state.copy(messages = list, isGenerating = false)
                        }
                        fireNotification("Antigravity AI", "Yanıt hazır — sıra sende")
                        fetchConversations()
                        fetchUsage()
                    }
                    is StreamEvent.Stopped -> {
                        _uiState.update { state ->
                            val list = state.messages.toMutableList()
                            if (list.isNotEmpty() && list.last().role == "bot") {
                                val last = list.last()
                                list[list.size - 1] = last.copy(state = MessageState.DONE)
                            }
                            state.copy(messages = list, isGenerating = false)
                        }
                        fireNotification("Antigravity AI", "Üretim durduruldu")
                    }
                    is StreamEvent.SessionLoaded -> {
                        val serverMessages = event.session.messages?.map { mapSessionMessage(it) } ?: emptyList()
                        _uiState.update {
                            it.copy(
                                messages = serverMessages,
                                currentSessionId = event.session.id,
                                currentConversationId = event.session.conversationId,
                                isGenerating = event.session.isGenerating
                            )
                        }
                    }
                    is StreamEvent.SessionReset -> {
                        _uiState.update { it.copy(messages = emptyList(), isGenerating = false) }
                        fetchConversations()
                    }
                    is StreamEvent.PermissionRequested -> {
                        _uiState.update { it.copy(pendingPermission = event.request) }
                    }
                    is StreamEvent.QuestionRequested -> {
                        _uiState.update { it.copy(pendingQuestion = event.request) }
                    }
                    is StreamEvent.Error -> {
                        _uiState.update { state ->
                            val list = state.messages.toMutableList()
                            if (list.isNotEmpty() && list.last().role == "bot") {
                                val last = list.last()
                                val updated = last.copy(
                                    content = last.content + "\n\n⚠️ Hata: ${event.message}*",
                                    state = MessageState.ERROR
                                )
                                list[list.size - 1] = updated
                            }
                            state.copy(messages = list, isGenerating = false, errorMessage = event.message, notice = null)
                        }
                        fireNotification("Antigravity AI", "Üretim hatası: ${event.message.take(140)}")
                    }
                    is StreamEvent.Stderr -> {
                        _uiState.update { it.copy(notice = event.text) }
                    }
                }
            }
        }
    }

    fun fetchConversations() {
        viewModelScope.launch {
            repository.fetchConversations().onSuccess { res ->
                _uiState.update {
                    it.copy(
                        conversations = res.conversations ?: emptyList(),
                        currentSessionId = res.currentSessionId ?: it.currentSessionId
                    )
                }
            }
        }
    }

    fun fetchModelsConfig() {
        viewModelScope.launch {
            repository.fetchModelsConfig().onSuccess { res ->
                _uiState.update {
                    it.copy(
                        availableModels = res.models ?: emptyList(),
                        availableEfforts = res.efforts ?: emptyList()
                    )
                }
            }
        }
    }

    fun fetchSkills() {
        viewModelScope.launch {
            repository.fetchSkills().onSuccess { res ->
                _uiState.update { it.copy(installedSkills = res.skills ?: emptyList()) }
            }
        }
    }

    fun fetchUsage() {
        viewModelScope.launch {
            repository.fetchUsage().onSuccess { res ->
                _uiState.update { it.copy(usage = res.usage) }
            }
        }
    }

    fun selectConversation(id: String) {
        viewModelScope.launch {
            repository.loadConversation(id).onSuccess { res ->
                val serverMessages = res.session?.messages?.map { mapSessionMessage(it) } ?: emptyList()
                _uiState.update {
                    it.copy(
                        messages = serverMessages,
                        currentSessionId = res.session?.id,
                        currentConversationId = res.session?.conversationId,
                        isGenerating = res.isGenerating
                    )
                }
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id).onSuccess {
                fetchConversations()
                syncWithServer()
            }
        }
    }

    fun fetchVaultFiles() {
        viewModelScope.launch {
            repository.fetchVaultFiles().onSuccess { res ->
                _uiState.update { it.copy(vaultFiles = res.files ?: emptyList()) }
            }
        }
    }

    fun loadVaultFileContent(relPath: String) {
        if (relPath.isBlank()) {
            _uiState.update { it.copy(activeVaultFilePath = null, activeVaultFileContent = null) }
            return
        }
        viewModelScope.launch {
            repository.fetchVaultFileContent(relPath).onSuccess { res ->
                _uiState.update {
                    it.copy(
                        activeVaultFilePath = relPath,
                        activeVaultFileContent = res.content
                    )
                }
            }
        }
    }

    fun saveVaultNote(relPath: String?, title: String?, content: String) {
        viewModelScope.launch {
            repository.saveVaultNote(relPath, title, content).onSuccess {
                fetchVaultFiles()
                if (relPath != null) loadVaultFileContent(relPath)
            }
        }
    }

    fun createVaultFolder(folderPath: String) {
        viewModelScope.launch {
            repository.createVaultFolder(folderPath).onSuccess {
                fetchVaultFiles()
            }
        }
    }

    fun deleteVaultFile(relPath: String) {
        viewModelScope.launch {
            repository.deleteVaultFile(relPath).onSuccess {
                fetchVaultFiles()
                if (_uiState.value.activeVaultFilePath == relPath) {
                    _uiState.update { it.copy(activeVaultFilePath = null, activeVaultFileContent = null) }
                }
            }
        }
    }

    fun uploadAndAttachFile(fileName: String, uri: Uri, mimeType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAttachment = true) }
            try {
                val bytes = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }

                if (bytes != null) {
                    val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val type = if (mimeType.startsWith("image/")) "image" else "doc"

                    repository.uploadFile(fileName, base64, type).onSuccess { res ->
                        val attachment = Attachment(
                            name = res.fileName,
                            path = res.path,
                            localUri = uri.toString(),
                            relPath = res.relPath,
                            type = res.type,
                            size = res.size
                        )
                        _uiState.update {
                            it.copy(
                                attachments = it.attachments + attachment,
                                isUploadingAttachment = false
                            )
                        }
                    }.onFailure {
                        _uiState.update { it.copy(isUploadingAttachment = false) }
                    }
                } else {
                    _uiState.update { it.copy(isUploadingAttachment = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUploadingAttachment = false) }
            }
        }
    }

    fun syncWithServer() {
        viewModelScope.launch {
            repository.fetchSession().onSuccess { response ->
                val serverMessages = response.session?.messages?.map { mapSessionMessage(it) } ?: emptyList()
                _uiState.update {
                    it.copy(
                        messages = serverMessages,
                        isGenerating = response.isGenerating,
                        currentSessionId = response.session?.id,
                        currentConversationId = response.session?.conversationId
                    )
                }
            }
        }
    }

    private fun mapSessionMessage(sessionMsg: SessionMessage): Message {
        return Message(
            role = sessionMsg.role,
            content = sessionMsg.content ?: "",
            tools = sessionMsg.tools?.toMutableList() ?: mutableListOf(),
            usage = sessionMsg.usage,
            attachments = sessionMsg.attachments ?: emptyList(),
            state = if (sessionMsg.state == "generating") MessageState.GENERATING else MessageState.DONE
        )
    }

    // Smart Multi-Paste Detection & Slash/Mention Triggers
    fun onInputTextChange(newText: String) {
        val isHugePaste = (newText.length > 150 || newText.lines().size > 3) && _uiState.value.inputText.isEmpty()
        if (isHugePaste) {
            val newBlock = PastedBlock(content = newText)
            _uiState.update {
                it.copy(
                    inputText = "",
                    pastedBlocks = it.pastedBlocks + newBlock
                )
            }
            return
        }

        val isSlash = newText.startsWith("/")
        val lastWord = newText.substringAfterLast(" ")
        val isMention = lastWord.startsWith("@")

        _uiState.update {
            it.copy(
                inputText = newText,
                showSlashCommands = isSlash,
                slashQuery = if (isSlash) newText else "",
                showMentions = isMention,
                mentionQuery = if (isMention) lastWord else ""
            )
        }
    }

    fun onSelectSlashCommand(cmd: SlashCommand) {
        _uiState.update {
            it.copy(
                inputText = cmd.command + " ",
                showSlashCommands = false
            )
        }
    }

    fun onSelectMention(item: VaultItem) {
        _uiState.update { state ->
            val prefix = state.inputText.substringBeforeLast("@")
            val updated = "$prefix@${item.path} "
            state.copy(
                inputText = updated,
                showMentions = false
            )
        }
    }

    fun referenceFile(item: VaultItem) {
        _uiState.update { state ->
            val current = state.inputText
            val refText = "@agy-vault/${item.path}"
            val updated = if (current.isEmpty()) refText else "$current $refText"
            state.copy(inputText = updated)
        }
    }

    fun referenceParagraph(fileName: String, paragraph: String) {
        _uiState.update { state ->
            val current = state.inputText
            val quote = "> [Alıntı: $fileName]\n> ${paragraph.replace("\n", "\n> ")}\n\n"
            val updated = if (current.isEmpty()) quote else "$quote\n$current"
            state.copy(inputText = updated)
        }
    }

    fun removePastedBlock(block: PastedBlock) {
        _uiState.update { it.copy(pastedBlocks = it.pastedBlocks.filter { b -> b.id != block.id }) }
    }

    fun addAttachment(attachment: Attachment) {
        _uiState.update { it.copy(attachments = it.attachments + attachment) }
    }

    fun removeAttachment(attachment: Attachment) {
        _uiState.update { it.copy(attachments = it.attachments.filter { a -> a != attachment }) }
    }

    fun updateSettings(newSettings: ChatSettings) {
        saveSettings(newSettings)
        _uiState.update { it.copy(settings = newSettings) }
    }

    fun setSettingsDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = visible) }
    }

    fun setVaultManagerVisible(visible: Boolean) {
        _uiState.update { it.copy(showVaultManager = visible) }
    }

    fun setUsageDetailVisible(visible: Boolean) {
        _uiState.update { it.copy(showUsageDetail = visible) }
    }

    fun setAuthDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showAuthDialog = visible) }
    }

    fun fetchAuthStatus() {
        viewModelScope.launch {
            repository.fetchAuthStatus().onSuccess { res ->
                _uiState.update { it.copy(isAuthenticated = res.isAuthenticated, authMethod = res.authMethod) }
            }
        }
    }

    fun submitAuthToken(token: String) {
        viewModelScope.launch {
            repository.submitAuthToken(token).onSuccess { res ->
                if (res.status == "ok") {
                    _uiState.update { it.copy(isAuthenticated = true, showAuthDialog = false) }
                    refreshAll()
                } else {
                    _uiState.update { it.copy(errorMessage = res.error ?: "Token doğrulanamadı") }
                }
            }.onFailure { err ->
                _uiState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        var finalPrompt = state.inputText.trim()

        if (state.pastedBlocks.isNotEmpty()) {
            val blocksText = state.pastedBlocks.mapIndexed { idx, b ->
                "### Ek Metin / Kod Parçası #${idx + 1}:\n```\n${b.content.trim()}\n```"
            }.joinToString("\n\n")

            finalPrompt = if (finalPrompt.isEmpty()) blocksText else "$finalPrompt\n\n$blocksText"
        }

        if (finalPrompt.isEmpty() && state.attachments.isEmpty()) return
        if (state.isGenerating) return

        val userMessage = Message(
            role = "user",
            content = finalPrompt,
            attachments = state.attachments
        )
        val botPlaceholder = Message(
            role = "bot",
            content = "",
            state = MessageState.GENERATING
        )

        _uiState.update {
            it.copy(
                inputText = "",
                pastedBlocks = emptyList(),
                attachments = emptyList(),
                showSlashCommands = false,
                showMentions = false,
                messages = it.messages + userMessage + botPlaceholder,
                isGenerating = true,
                notice = null
            )
        }

        viewModelScope.launch {
            repository.sendMessage(
                prompt = finalPrompt,
                continueChat = true,
                settings = state.settings,
                attachments = userMessage.attachments
            ).onFailure { err ->
                _uiState.update { current ->
                    val cleanList = current.messages.filter { it.id != botPlaceholder.id }
                    current.copy(
                        messages = cleanList,
                        isGenerating = false,
                        errorMessage = err.message ?: "Mesaj gönderilemedi"
                    )
                }
            }
        }
    }

    fun startNewChat() {
        viewModelScope.launch {
            repository.startNewChat().onSuccess { res ->
                _uiState.update {
                    it.copy(
                        messages = emptyList(),
                        isGenerating = false,
                        currentSessionId = res.session?.id,
                        currentConversationId = null
                    )
                }
                fetchConversations()
            }
        }
    }

    fun stopExecution() {
        viewModelScope.launch {
            repository.stopExecution()
            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    // --- opencode izin & soru onayları ---

    fun replyPermission(allow: Boolean, always: Boolean) {
        val req = _uiState.value.pendingPermission ?: return
        viewModelScope.launch {
            repository.replyPermission(req.sessionID, req.id, allow, always)
            _uiState.update { it.copy(pendingPermission = null) }
        }
    }

    fun dismissPermission() {
        _uiState.update { it.copy(pendingPermission = null) }
    }

    fun replyQuestion(answers: List<String>) {
        val req = _uiState.value.pendingQuestion ?: return
        viewModelScope.launch {
            repository.replyQuestion(req.sessionID, req.id, answers)
            _uiState.update { it.copy(pendingQuestion = null) }
        }
    }

    fun dismissQuestion() {
        _uiState.update { it.copy(pendingQuestion = null) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    private fun fireNotification(title: String, message: String) {
        if (prefs.getBoolean("notificationsEnabled", true)) {
            NotificationHelper.notify(getApplication(), title, message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }
}
