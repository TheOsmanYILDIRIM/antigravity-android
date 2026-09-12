package com.antigravity.ai.ui.viewmodel

import android.app.Application
import android.content.Context
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
import com.antigravity.ai.service.FloatingKeepAliveService
import com.antigravity.ai.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.Locale

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val conversations: List<ConversationMeta> = emptyList(),
    val pinnedConversationIds: Set<String> = emptySet(),
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
    val generatingConversationIds: Set<String> = emptySet(),
    val isListening: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showAuthDialog: Boolean = false,
    val isAuthenticated: Boolean = true,
    val authMethod: String = "oauth",
    val isAgyAuthLoading: Boolean = false,
    val agyAuthError: String? = null,
    val isAgyWaitingCode: Boolean = false,
    val agyAuthUrl: String? = null,
    val showVaultManager: Boolean = false,
    val showFileManager: Boolean = false,
    val fsCurrentDir: String = "/data/data/com.termux/files/home",
    val fsParentDir: String? = null,
    val fsHomeDir: String = "/data/data/com.termux/files/home",
    val fsItems: List<FsItem> = emptyList(),
    val fsProjects: List<ProjectItem> = emptyList(),
    val isFsLoading: Boolean = false,
    val activeViewerFilePath: String? = null,
    val activeViewerFileContent: FsContentResponse? = null,
    val isViewerLoading: Boolean = false,
    val activeImageViewerUrl: String? = null,
    val activeImageViewerTitle: String = "",
    val serverHealth: ServerHealth? = null,
    val isCheckingHealth: Boolean = false,
    val isKeepAliveRunning: Boolean = false,
    val keepAliveMode: String = "invisible",
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
    private val _uiState = MutableStateFlow(ChatUiState(
        settings = loadSavedSettings(),
        pinnedConversationIds = prefs.getStringSet("pinned_conversations", emptySet()) ?: emptySet()
    ))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        initSpeechRecognizer()
        val floatPrefs = application.getSharedPreferences(FloatingKeepAliveService.PREFS_NAME, Context.MODE_PRIVATE)
        val keepAliveEnabled = floatPrefs.getBoolean(FloatingKeepAliveService.KEY_ENABLED, false)
        val keepAliveMode = floatPrefs.getString(FloatingKeepAliveService.KEY_MODE, "invisible") ?: "invisible"
        _uiState.update { it.copy(isKeepAliveRunning = keepAliveEnabled, keepAliveMode = keepAliveMode) }
        if (keepAliveEnabled && FloatingKeepAliveService.canDrawOverlays(application)) {
            FloatingKeepAliveService.startKeepAlive(application)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val name = resolveBackendName()
            repository = ChatRepository(buildBackend(name))
            _uiState.update { it.copy(activeBackend = name) }
            startEventCollection()
            refreshAll()
            val health = AgyServerManager.checkHealth()
            _uiState.update { it.copy(serverHealth = health) }
            if (!health.isOnline) {
                withContext(Dispatchers.Main) {
                    startAgyServer()
                }
            }
            startPeriodicHealthCheck()
        }
    }

    private fun startPeriodicHealthCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            var wasOnline = _uiState.value.serverHealth?.isOnline == true
            while (true) {
                delay(2500)
                val health = AgyServerManager.checkHealth()
                val becameOnline = !wasOnline && health.isOnline
                val shouldReload = becameOnline || (health.isOnline && _uiState.value.conversations.isEmpty())
                wasOnline = health.isOnline
                _uiState.update { it.copy(serverHealth = health) }
                if (shouldReload) {
                    startEventCollection()
                    refreshAll()
                }
            }
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
        val currentConvId = _uiState.value.currentConversationId
        if (!currentConvId.isNullOrBlank()) {
            selectConversation(currentConvId)
        }
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
                        _uiState.update { state ->
                            val updatedSet = state.generatingConversationIds.toMutableSet()
                            updatedSet.add(event.conversationId)
                            val isCurrentlyNew = state.currentConversationId == null
                            state.copy(
                                currentSessionId = if (isCurrentlyNew) event.conversationId else state.currentSessionId,
                                currentConversationId = if (isCurrentlyNew) event.conversationId else state.currentConversationId,
                                generatingConversationIds = updatedSet,
                                isGenerating = if (isCurrentlyNew) true else state.isGenerating
                            )
                        }
                        fetchConversations()
                    }
                    is StreamEvent.GeneratingStatus -> {
                        _uiState.update { state ->
                            val currentActiveId = state.currentConversationId
                            val updatedSet = state.generatingConversationIds.toMutableSet()
                            if (event.isGenerating) {
                                event.conversationId?.let { updatedSet.add(it) }
                                val isCurrentGenerating = event.conversationId == null || event.conversationId == currentActiveId
                                state.copy(
                                    generatingConversationIds = updatedSet,
                                    isGenerating = if (isCurrentGenerating) true else state.isGenerating
                                )
                            } else {
                                event.conversationId?.let { updatedSet.remove(it) }
                                val isCurrentDone = event.conversationId == null || event.conversationId == currentActiveId
                                state.copy(
                                    generatingConversationIds = updatedSet,
                                    isGenerating = if (isCurrentDone) false else state.isGenerating
                                )
                            }
                        }
                    }
                    is StreamEvent.Chunk -> {
                        _uiState.update { state ->
                            val currentActiveId = state.currentConversationId
                            val isMatching = event.conversationId == null || currentActiveId == null || event.conversationId == currentActiveId
                            if (!isMatching) {
                                state
                            } else {
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
                    }
                    is StreamEvent.ToolUpdate -> {
                        _uiState.update { state ->
                            val currentActiveId = state.currentConversationId
                            val isMatching = event.conversationId == null || currentActiveId == null || event.conversationId == currentActiveId
                            if (!isMatching) {
                                state
                            } else {
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
                    }
                    is StreamEvent.Done -> {
                        _uiState.update { state ->
                            val currentActiveId = state.currentConversationId
                            val isMatching = event.conversationId == null || currentActiveId == null || event.conversationId == currentActiveId
                            val updatedSet = state.generatingConversationIds.toMutableSet()
                            event.conversationId?.let { updatedSet.remove(it) } ?: currentActiveId?.let { updatedSet.remove(it) }

                            val list = if (isMatching) {
                                val mList = state.messages.toMutableList()
                                if (mList.isNotEmpty() && mList.last().role == "bot") {
                                    val last = mList.last()
                                    val updated = last.copy(
                                        content = event.botMessage?.content ?: last.content,
                                        tools = (event.botMessage?.tools ?: last.tools).toMutableList(),
                                        usage = event.botMessage?.usage ?: last.usage,
                                        state = MessageState.DONE
                                    )
                                    mList[mList.size - 1] = updated
                                }
                                mList
                            } else {
                                state.messages
                            }

                            val isStillGenerating = currentActiveId != null && updatedSet.contains(currentActiveId)
                            state.copy(
                                messages = list,
                                isGenerating = isStillGenerating,
                                generatingConversationIds = updatedSet
                            )
                        }
                        fireNotification("Antigravity AI", "Yanıt hazır — sıra sende")
                        fetchConversations()
                        fetchUsage()
                    }
                    is StreamEvent.Stopped -> {
                        _uiState.update { state ->
                            val currentActiveId = state.currentConversationId
                            val isMatching = event.conversationId == null || currentActiveId == null || event.conversationId == currentActiveId
                            val updatedSet = state.generatingConversationIds.toMutableSet()
                            event.conversationId?.let { updatedSet.remove(it) } ?: currentActiveId?.let { updatedSet.remove(it) }

                            val list = if (isMatching) {
                                val mList = state.messages.toMutableList()
                                if (mList.isNotEmpty() && mList.last().role == "bot") {
                                    val last = mList.last()
                                    mList[mList.size - 1] = last.copy(state = MessageState.DONE)
                                }
                                mList
                            } else {
                                state.messages
                            }

                            val isStillGenerating = currentActiveId != null && updatedSet.contains(currentActiveId)
                            state.copy(
                                messages = list,
                                isGenerating = isStillGenerating,
                                generatingConversationIds = updatedSet
                            )
                        }
                        fireNotification("Antigravity AI", "Üretim durduruldu")
                    }
                    is StreamEvent.SessionLoaded -> {
                        val currentActiveId = _uiState.value.currentConversationId
                        val loadedConvId = event.session.conversationId ?: event.session.id
                        if (!currentActiveId.isNullOrBlank() && currentActiveId == loadedConvId) {
                            val serverMessages = event.session.messages?.map { mapSessionMessage(it) } ?: emptyList()
                            _uiState.update {
                                it.copy(
                                    messages = serverMessages,
                                    currentSessionId = loadedConvId,
                                    currentConversationId = loadedConvId,
                                    isGenerating = event.session.isGenerating
                                )
                            }
                        }
                    }
                    is StreamEvent.SessionReset -> {
                        if (_uiState.value.currentConversationId == null) {
                            _uiState.update { it.copy(messages = emptyList(), isGenerating = false, currentSessionId = null) }
                        }
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
                            val currentActiveId = state.currentConversationId
                            val isMatching = event.conversationId == null || currentActiveId == null || event.conversationId == currentActiveId
                            val updatedSet = state.generatingConversationIds.toMutableSet()
                            event.conversationId?.let { updatedSet.remove(it) } ?: currentActiveId?.let { updatedSet.remove(it) }

                            val list = if (isMatching) {
                                val mList = state.messages.toMutableList()
                                if (mList.isNotEmpty() && mList.last().role == "bot") {
                                    val last = mList.last()
                                    val updated = last.copy(
                                        content = last.content + "\n\n⚠️ *Hata: ${event.message}*",
                                        state = MessageState.ERROR
                                    )
                                    mList[mList.size - 1] = updated
                                }
                                mList
                            } else {
                                state.messages
                            }

                            val isStillGenerating = currentActiveId != null && updatedSet.contains(currentActiveId)
                            state.copy(
                                messages = list,
                                isGenerating = isStillGenerating,
                                generatingConversationIds = updatedSet,
                                errorMessage = if (isMatching) event.message else state.errorMessage,
                                notice = null
                            )
                        }
                        fireNotification("Antigravity AI", "Üretim hatası: ${event.message.take(140)}")
                    }
                    is StreamEvent.AuthRequired -> {
                        _uiState.update {
                            it.copy(
                                isAuthenticated = false,
                                isGenerating = false,
                                errorMessage = event.message,
                                showAuthDialog = true,
                                agyAuthUrl = event.authUrl,
                                isAgyWaitingCode = !event.authUrl.isNullOrBlank(),
                                agyAuthError = null
                            )
                        }
                        fireNotification("Antigravity AI", "Oturum yenileme gerekli: ${event.message.take(140)}")
                    }
                    is StreamEvent.Stderr -> {
                        val currentActiveId = _uiState.value.currentConversationId
                        val isMatching = event.conversationId == null || currentActiveId == null || event.conversationId == currentActiveId
                        if (isMatching) {
                            _uiState.update { it.copy(notice = event.text) }
                        }
                    }
                }
            }
        }
    }

    fun fetchConversations() {
        viewModelScope.launch {
            repository.fetchConversations().onSuccess { res ->
                val serverGeneratingIds = (res.conversations ?: emptyList())
                    .filter { it.isGenerating }
                    .map { it.id }
                    .toSet()

                _uiState.update { state ->
                    val combinedGenerating = (state.generatingConversationIds + serverGeneratingIds).toSet()
                    state.copy(
                        conversations = res.conversations ?: emptyList(),
                        generatingConversationIds = combinedGenerating
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
        if (id.isBlank()) return
        viewModelScope.launch {
            repository.loadConversation(id).onSuccess { res ->
                val serverMessages = res.session?.messages?.map { mapSessionMessage(it) } ?: emptyList()
                val convId = res.session?.conversationId ?: res.session?.id ?: id
                val isConvGenerating = _uiState.value.generatingConversationIds.contains(convId) || res.isGenerating
                _uiState.update {
                    it.copy(
                        messages = serverMessages,
                        currentSessionId = convId,
                        currentConversationId = convId,
                        isGenerating = isConvGenerating,
                        inputText = "",
                        attachments = emptyList(),
                        pastedBlocks = emptyList(),
                        notice = null
                    )
                }
            }
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id).onSuccess {
                fetchConversations()
                val wasActive = _uiState.value.currentConversationId == id || _uiState.value.currentSessionId == id
                if (wasActive) {
                    _uiState.update {
                        it.copy(
                            messages = emptyList(),
                            currentSessionId = null,
                            currentConversationId = null,
                            isGenerating = false,
                            notice = null
                        )
                    }
                }
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
                        val newAttachments = _uiState.value.attachments + attachment
                        val tag = getAttachmentTag(attachment, newAttachments)
                        _uiState.update {
                            val currentInput = it.inputText
                            val updatedInput = insertTagIntoText(currentInput, tag)
                            it.copy(
                                attachments = newAttachments,
                                inputText = updatedInput,
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

    fun setFileManagerVisible(visible: Boolean) {
        _uiState.update { it.copy(showFileManager = visible) }
        if (visible) {
            loadFsDirectory(_uiState.value.fsCurrentDir)
            loadFsProjects()
        }
    }

    fun loadFsDirectory(dir: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFsLoading = true) }
            repository.fetchFsList(dir).onSuccess { res ->
                _uiState.update {
                    it.copy(
                        fsCurrentDir = res.currentDir,
                        fsParentDir = res.parentDir,
                        fsHomeDir = res.homeDir,
                        fsItems = res.items ?: emptyList(),
                        fsProjects = if (!res.projects.isNullOrEmpty()) res.projects else it.fsProjects,
                        isFsLoading = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isFsLoading = false) }
            }
        }
    }

    fun loadFsProjects() {
        viewModelScope.launch {
            repository.fetchFsProjects().onSuccess { res ->
                _uiState.update { it.copy(fsProjects = res.projects ?: emptyList()) }
            }
        }
    }

    fun openFileInViewer(path: String) {
        if (path.isBlank()) return
        val ext = path.substringAfterLast(".").lowercase()
        val isImg = ext in listOf("png", "jpg", "jpeg", "webp", "gif", "svg")
        if (isImg) {
            openImageInViewer(path, path.substringAfterLast("/"))
            return
        }

        _uiState.update {
            it.copy(
                activeViewerFilePath = path,
                activeViewerFileContent = null,
                isViewerLoading = true
            )
        }
        viewModelScope.launch {
            repository.fetchFsContent(path).onSuccess { res ->
                _uiState.update { it.copy(activeViewerFileContent = res, isViewerLoading = false) }
            }.onFailure {
                _uiState.update { it.copy(isViewerLoading = false) }
            }
        }
    }

    fun openImageInViewer(url: String, title: String = "") {
        _uiState.update {
            it.copy(
                activeImageViewerUrl = url,
                activeImageViewerTitle = title.ifEmpty { url.substringAfterLast("/") }
            )
        }
    }

    fun closeFileViewer() {
        _uiState.update { it.copy(activeViewerFilePath = null, activeViewerFileContent = null) }
    }

    fun closeImageViewer() {
        _uiState.update { it.copy(activeImageViewerUrl = null, activeImageViewerTitle = "") }
    }

    fun saveFsFileContent(path: String, content: String) {
        viewModelScope.launch {
            repository.saveFsFile(path, content).onSuccess {
                loadFsDirectory(_uiState.value.fsCurrentDir)
                openFileInViewer(path)
            }
        }
    }

    fun attachFsPathToChat(path: String) {
        val fileName = path.substringAfterLast("/")
        val ext = path.substringAfterLast(".").lowercase()
        val isImg = ext in listOf("png", "jpg", "jpeg", "webp", "gif", "svg")
        val attachment = Attachment(
            name = fileName,
            path = path,
            type = if (isImg) "image" else "file"
        )
        val newAttachments = _uiState.value.attachments + attachment
        val tag = getAttachmentTag(attachment, newAttachments)
        _uiState.update {
            val currentInput = it.inputText
            val updatedInput = insertTagIntoText(currentInput, tag)
            it.copy(
                attachments = newAttachments,
                inputText = updatedInput
            )
        }
    }

    fun mentionFsPathInChat(path: String) {
        _uiState.update { state ->
            val current = state.inputText
            val refText = "@$path"
            val updated = if (current.isEmpty()) refText else "$current $refText"
            state.copy(inputText = updated)
        }
    }

    fun checkServerHealth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingHealth = true) }
            val health = AgyServerManager.checkHealth()
            val wasOnline = _uiState.value.serverHealth?.isOnline == true
            val shouldReload = (!wasOnline && health.isOnline) || (health.isOnline && _uiState.value.conversations.isEmpty())
            _uiState.update { it.copy(serverHealth = health, isCheckingHealth = false) }
            if (shouldReload) {
                startEventCollection()
                refreshAll()
            }
        }
    }

    fun startAgyServer() {
        try {
            FloatingKeepAliveService.startKeepAlive(getApplication())
        } catch (e: Exception) {}

        AgyServerManager.startServer(getApplication()) { success, msg ->
            _uiState.update { it.copy(notice = if (success) "Termux agy-web arka planda başlatılıyor..." else msg) }
            viewModelScope.launch {
                for (step in listOf(800L, 1200L, 1500L, 2000L, 2500L)) {
                    delay(step)
                    val health = AgyServerManager.checkHealth()
                    _uiState.update { it.copy(serverHealth = health) }
                    if (health.isOnline) {
                        startEventCollection()
                        refreshAll()
                        if (_uiState.value.showFileManager) {
                            loadFsDirectory(_uiState.value.fsCurrentDir)
                            loadFsProjects()
                        }
                        break
                    }
                }
            }
        }
    }

    fun stopAgyServer() {
        viewModelScope.launch {
            AgyServerManager.stopServer(getApplication()) { success, msg ->
                _uiState.update { it.copy(notice = msg) }
                checkServerHealth()
            }
        }
    }

    fun restartAgyServer() {
        AgyServerManager.restartServer(getApplication()) { success, msg ->
            _uiState.update { it.copy(notice = msg) }
            viewModelScope.launch {
                delay(1500)
                checkServerHealth()
            }
        }
    }

    fun toggleKeepAlive(enabled: Boolean, mode: String = "invisible") {
        val app = getApplication<Application>()
        val floatPrefs = app.getSharedPreferences(FloatingKeepAliveService.PREFS_NAME, Context.MODE_PRIVATE)
        floatPrefs.edit()
            .putBoolean(FloatingKeepAliveService.KEY_ENABLED, enabled)
            .putString(FloatingKeepAliveService.KEY_MODE, mode)
            .apply()

        if (enabled) {
            FloatingKeepAliveService.startKeepAlive(app)
        } else {
            FloatingKeepAliveService.stopKeepAlive(app)
        }

        _uiState.update {
            it.copy(
                isKeepAliveRunning = enabled,
                keepAliveMode = mode
            )
        }
    }

    fun syncWithServer() {
        val currentConvId = _uiState.value.currentConversationId
        if (!currentConvId.isNullOrBlank()) {
            selectConversation(currentConvId)
            return
        }
        viewModelScope.launch {
            repository.fetchSession().onSuccess { response ->
                if (_uiState.value.currentConversationId == null && _uiState.value.messages.isEmpty()) {
                    val serverMessages = response.session?.messages?.map { mapSessionMessage(it) } ?: emptyList()
                    if (serverMessages.isNotEmpty()) {
                        val convId = response.session?.conversationId ?: response.session?.id
                        val isGenerating = (convId != null && _uiState.value.generatingConversationIds.contains(convId)) || response.isGenerating
                        _uiState.update {
                            it.copy(
                                messages = serverMessages,
                                isGenerating = isGenerating,
                                currentSessionId = convId,
                                currentConversationId = convId
                            )
                        }
                    }
                }
            }
        }
    }

    private fun mapSessionMessage(sessionMsg: SessionMessage): Message {
        val raw = sessionMsg.content ?: ""
        var displayContent = raw
        val extractedBlocks = mutableListOf<PastedBlock>()

        if (sessionMsg.role == "user" && raw.contains("### Ek Metin / Kod Parçası")) {
            val regex = Regex("### Ek Metin / Kod Parçası #\\d+:\\s*```[a-zA-Z]*\\n([\\s\\S]*?)\\n```")
            val matches = regex.findAll(raw).toList()
            if (matches.isNotEmpty()) {
                matches.forEach { m ->
                    val blockContent = m.groupValues[1]
                    extractedBlocks.add(PastedBlock(content = blockContent))
                }
                displayContent = regex.replace(raw, "").trim()
            }
        }

        return Message(
            role = sessionMsg.role,
            content = displayContent,
            tools = sessionMsg.tools?.toMutableList() ?: mutableListOf(),
            usage = sessionMsg.usage,
            attachments = sessionMsg.attachments ?: emptyList(),
            pastedBlocks = extractedBlocks,
            state = if (sessionMsg.state == "generating") MessageState.GENERATING else MessageState.DONE
        )
    }

    fun togglePinConversation(id: String) {
        val current = _uiState.value.pinnedConversationIds.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        prefs.edit().putStringSet("pinned_conversations", current).apply()
        _uiState.update { it.copy(pinnedConversationIds = current) }
    }

    fun exportSingleConversation(context: android.content.Context, id: String) {
        viewModelScope.launch {
            repository.loadConversation(id).onSuccess { res ->
                val session = res.session ?: return@onSuccess
                val sb = StringBuilder()
                sb.append("# ").append(session.title ?: "Antigravity AI Sohbeti").append("\n\n")
                sb.append("**Sohbet ID:** `").append(session.id ?: id).append("`  \n")
                sb.append("**Tarih:** ").append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(java.util.Date())).append("\n\n---\n\n")

                session.messages?.forEach { msg ->
                    val roleName = if (msg.role == "user") "👤 Kullanıcı" else "✨ Antigravity AI"
                    sb.append("### ").append(roleName).append("\n\n")
                    if (!msg.content.isNullOrBlank()) {
                        sb.append(msg.content).append("\n\n")
                    }
                    msg.tools?.forEach { tool ->
                        sb.append("> ⚙️ **Araç Çağrısı:** `").append(tool.name).append("` (Durum: ").append(tool.state).append(")\n")
                    }
                    sb.append("\n---\n\n")
                }

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, session.title ?: "Sohbet Dışa Aktarımı")
                    putExtra(Intent.EXTRA_TEXT, sb.toString())
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(shareIntent, "Sohbeti Paylaş / Dışa Aktar").apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }
        }
    }

    fun exportAllConversations(context: android.content.Context) {
        viewModelScope.launch {
            val convs = _uiState.value.conversations
            if (convs.isEmpty()) {
                android.widget.Toast.makeText(context, "Dışa aktarılacak sohbet bulunamadı", android.widget.Toast.LENGTH_SHORT).show()
                return@launch
            }

            val sb = StringBuilder()
            sb.append("# Antigravity AI - Tüm Sohbetler Arşivi\n\n")
            sb.append("**Toplam Sohbet Sayısı:** ").append(convs.size).append("  \n")
            sb.append("**Arşiv Tarihi:** ").append(java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(java.util.Date())).append("\n\n")
            sb.append("## İçindekiler\n\n")
            convs.forEachIndexed { index, meta ->
                sb.append("${index + 1}. [${meta.title}](#sohbet-${meta.id})\n")
            }
            sb.append("\n---\n\n")

            for (meta in convs) {
                repository.loadConversation(meta.id).onSuccess { res ->
                    val session = res.session ?: return@onSuccess
                    sb.append("<a name=\"sohbet-${session.id}\"></a>\n\n")
                    sb.append("## ").append(session.title ?: meta.title).append("\n\n")
                    sb.append("**Tarih:** ").append(meta.lastMessageTime ?: "").append("  \n\n")

                    session.messages?.forEach { msg ->
                        val roleName = if (msg.role == "user") "👤 Kullanıcı" else "✨ Antigravity AI"
                        sb.append("### ").append(roleName).append("\n\n")
                        if (!msg.content.isNullOrBlank()) {
                            sb.append(msg.content).append("\n\n")
                        }
                        sb.append("\n")
                    }
                    sb.append("\n---\n\n")
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Antigravity AI - Tüm Sohbetler Arşivi")
                putExtra(Intent.EXTRA_TEXT, sb.toString())
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(shareIntent, "Tüm Sohbetleri Paylaş / Dışa Aktar").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    }

    fun addPastedBlock(content: String) {
        val trimmed = content.trim()
        if (trimmed.isNotBlank()) {
            val newBlock = PastedBlock(content = trimmed)
            val newBlocks = _uiState.value.pastedBlocks + newBlock
            val tag = "[metin-${newBlocks.size}]"
            _uiState.update {
                val currentInput = it.inputText
                val updatedInput = insertTagIntoText(currentInput, tag)
                it.copy(
                    pastedBlocks = newBlocks,
                    inputText = updatedInput
                )
            }
        }
    }

    // Smart Multi-Paste Detection & Universal Slash/Mention Triggers (Anywhere in text)
    fun onInputTextChange(newText: String) {
        val currentText = _uiState.value.inputText
        val addedLength = newText.length - currentText.length
        val addedLines = newText.lines().size - currentText.lines().size

        // Otomatik yapıştırma algılama (80 karakterden uzun veya 2'den fazla satır eklenirse)
        if (addedLength > 80 || addedLines >= 2) {
            var commonPrefixLen = 0
            while (commonPrefixLen < currentText.length &&
                commonPrefixLen < newText.length &&
                currentText[commonPrefixLen] == newText[commonPrefixLen]
            ) {
                commonPrefixLen++
            }

            var commonSuffixLen = 0
            while (commonSuffixLen < (currentText.length - commonPrefixLen) &&
                commonSuffixLen < (newText.length - commonPrefixLen) &&
                currentText[currentText.length - 1 - commonSuffixLen] == newText[newText.length - 1 - commonSuffixLen]
            ) {
                commonSuffixLen++
            }

            val prefix = currentText.substring(0, commonPrefixLen)
            val suffix = currentText.substring(currentText.length - commonSuffixLen)
            val pastedChunk = newText.substring(commonPrefixLen, newText.length - commonSuffixLen)

            if (pastedChunk.trim().length > 70 || pastedChunk.lines().size > 2) {
                val newBlock = PastedBlock(content = pastedChunk.trim())
                val newBlocks = _uiState.value.pastedBlocks + newBlock
                val tag = "[metin-${newBlocks.size}]"
                val cleanPrefix = if (prefix.isNotEmpty() && !prefix.endsWith(" ")) "$prefix " else prefix
                val cleanSuffix = if (suffix.isNotEmpty() && !suffix.startsWith(" ")) " $suffix" else suffix
                val updatedInput = "$cleanPrefix$tag$cleanSuffix".trim()

                _uiState.update {
                    it.copy(
                        inputText = if (updatedInput.endsWith(tag)) "$updatedInput " else updatedInput,
                        pastedBlocks = newBlocks
                    )
                }
                return
            }
        }

        val lastWord = newText.split(Regex("[\\s\n]+")).lastOrNull() ?: ""
        val isSlash = lastWord.startsWith("/") && lastWord.length >= 1
        val isMention = lastWord.startsWith("@")

        _uiState.update {
            it.copy(
                inputText = newText,
                showSlashCommands = isSlash,
                slashQuery = if (isSlash) lastWord else "",
                showMentions = isMention,
                mentionQuery = if (isMention) lastWord else ""
            )
        }
    }

    fun onSelectSlashCommand(cmd: SlashCommand) {
        _uiState.update { state ->
            val text = state.inputText
            val lastSlashIdx = text.lastIndexOf('/')
            val updated = if (lastSlashIdx >= 0) {
                text.substring(0, lastSlashIdx) + cmd.command + " "
            } else {
                cmd.command + " "
            }
            state.copy(
                inputText = updated,
                showSlashCommands = false
            )
        }
    }

    fun onSelectMention(item: VaultItem) {
        _uiState.update { state ->
            val text = state.inputText
            val lastAtIdx = text.lastIndexOf('@')
            val updated = if (lastAtIdx >= 0) {
                text.substring(0, lastAtIdx) + "@${item.path} "
            } else {
                "$text @${item.path} "
            }
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
        _uiState.update { state ->
            val idx = state.pastedBlocks.indexOfFirst { it.id == block.id }
            val updatedBlocks = state.pastedBlocks.filter { it.id != block.id }
            var updatedText = state.inputText
            if (idx >= 0) {
                val num = idx + 1
                val tagRegex = Regex("\\[(metin|text|kod|ek|snippet|block)[-_]?$num\\]\\s*", RegexOption.IGNORE_CASE)
                updatedText = tagRegex.replace(updatedText, "").trim()
            }
            state.copy(
                pastedBlocks = updatedBlocks,
                inputText = updatedText
            )
        }
    }

    fun addAttachment(attachment: Attachment) {
        val newAttachments = _uiState.value.attachments + attachment
        val tag = getAttachmentTag(attachment, newAttachments)
        _uiState.update {
            val currentInput = it.inputText
            val updatedInput = insertTagIntoText(currentInput, tag)
            it.copy(
                attachments = newAttachments,
                inputText = updatedInput
            )
        }
    }

    fun removeAttachment(attachment: Attachment) {
        _uiState.update { state ->
            val idx = state.attachments.indexOf(attachment)
            val updatedAttachments = state.attachments.filter { it != attachment }
            var updatedText = state.inputText
            if (idx >= 0) {
                val num = idx + 1
                val tagRegex = Regex("\\[(image|resim|görsel|dosya|file|doc|ek)[-_]?$num\\]\\s*", RegexOption.IGNORE_CASE)
                updatedText = tagRegex.replace(updatedText, "").trim()
            }
            state.copy(
                attachments = updatedAttachments,
                inputText = updatedText
            )
        }
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
        _uiState.update {
            it.copy(
                showAuthDialog = visible,
                agyAuthError = if (visible) null else it.agyAuthError,
                isAgyAuthLoading = if (visible) false else it.isAgyAuthLoading,
                isAgyWaitingCode = if (visible) it.isAgyWaitingCode else false,
                agyAuthUrl = if (visible) it.agyAuthUrl else null
            )
        }
    }

    fun startAgyLogin(openUri: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAgyAuthLoading = true, agyAuthError = null) }
            repository.startAgLogin()
                .onSuccess { res ->
                    if (res.status == "ok" && !res.authUrl.isNullOrBlank()) {
                        _uiState.update {
                            it.copy(
                                isAgyAuthLoading = false,
                                agyAuthUrl = res.authUrl,
                                isAgyWaitingCode = true
                            )
                        }
                        openUri(res.authUrl)
                    } else {
                        _uiState.update {
                            it.copy(
                                isAgyAuthLoading = false,
                                agyAuthError = res.error ?: "Giriş başlatılamadı. agy-web sunucusu güncel mi?"
                            )
                        }
                    }
                }
                .onFailure { err ->
                    val raw = err.message ?: "Bilinmeyen hata"
                    val friendly = if (raw.contains("timed out", true) || raw.contains("timeout", true) || raw.contains("failed to connect", true)) {
                        "Sunucuya ulaşılamadı (127.0.0.1:8080). agy-web sunucusu çalışıyor mu?"
                    } else raw
                    _uiState.update { it.copy(isAgyAuthLoading = false, agyAuthError = friendly) }
                }
        }
    }

    fun submitAgyCode(code: String) {
        val trimmed = code.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAgyAuthLoading = true, agyAuthError = null) }
            repository.submitAuthCode(trimmed)
                .onSuccess { res ->
                    if (res.status == "ok") {
                        _uiState.update {
                            it.copy(
                                isAuthenticated = true,
                                showAuthDialog = false,
                                isAgyAuthLoading = false,
                                isAgyWaitingCode = false,
                                agyAuthError = null,
                                agyAuthUrl = null
                            )
                        }
                        refreshAll()
                    } else {
                        _uiState.update {
                            it.copy(
                                isAgyAuthLoading = false,
                                agyAuthError = res.error ?: "Kod doğrulanamadı."
                            )
                        }
                    }
                }
                .onFailure { err ->
                    val raw = err.message ?: "Bilinmeyen hata"
                    val friendly = if (raw.contains("timed out", true) || raw.contains("timeout", true) || raw.contains("failed to connect", true)) {
                        "Sunucuya ulaşılamadı (127.0.0.1:8080). agy-web sunucusu çalışıyor mu?"
                    } else raw
                    _uiState.update { it.copy(isAgyAuthLoading = false, agyAuthError = friendly) }
                }
        }
    }

    fun fetchAuthStatus() {
        viewModelScope.launch {
            repository.fetchAuthStatus().onSuccess { res ->
                _uiState.update { it.copy(isAuthenticated = res.isAuthenticated, authMethod = res.authMethod) }
            }
        }
    }

    private fun getAttachmentTag(att: Attachment, allAttachments: List<Attachment>): String {
        val isImg = att.type == "image" || att.name.endsWith(".png", true) || att.name.endsWith(".jpg", true) || att.name.endsWith(".jpeg", true) || att.name.endsWith(".webp", true) || att.name.endsWith(".gif", true)
        val num = allAttachments.indexOf(att).let { if (it >= 0) it + 1 else allAttachments.size }
        return if (isImg) "[image-$num]" else "[dosya-$num]"
    }

    private fun insertTagIntoText(text: String, tag: String): String {
        return if (text.isBlank()) {
            "$tag "
        } else if (text.endsWith(" ")) {
            "$text$tag "
        } else {
            "$text $tag "
        }
    }

    fun sendMessage() {
        val state = _uiState.value
        val userTypedText = state.inputText.trim()
        val blocks = state.pastedBlocks
        val attachments = state.attachments

        if (userTypedText.isEmpty() && blocks.isEmpty() && attachments.isEmpty()) return
        if (state.isGenerating) return

        // 1. Metin bloklarını inline olarak yerleştir veya ekle
        var processedPrompt = userTypedText
        val unplacedBlocks = mutableListOf<Pair<Int, PastedBlock>>()

        blocks.forEachIndexed { idx, block ->
            val num = idx + 1
            val blockTagRegex = Regex("\\[(metin|text|kod|ek|snippet|block)[-_]?$num\\]", RegexOption.IGNORE_CASE)
            val inlineReplacement = "\n\n[Ek Metin #$num:\n```\n${block.content.trim()}\n```]\n\n"

            if (blockTagRegex.containsMatchIn(processedPrompt)) {
                processedPrompt = blockTagRegex.replace(processedPrompt) { inlineReplacement }
            } else {
                unplacedBlocks.add(Pair(num, block))
            }
        }

        // Metin içinde bahsedilmemiş bloklar varsa en sona ekle
        if (unplacedBlocks.isNotEmpty()) {
            val appendedBlocks = unplacedBlocks.joinToString("\n\n") { (num, b) ->
                "### Ek Metin / Kod Parçası #$num:\n```\n${b.content.trim()}\n```"
            }
            processedPrompt = if (processedPrompt.isBlank()) appendedBlocks else "$processedPrompt\n\n$appendedBlocks"
        }

        // 2. Attachments (Görsel ve dosyaları) inline olarak yerleştir veya ekle
        val unplacedAttachments = mutableListOf<Pair<Int, Attachment>>()

        attachments.forEachIndexed { idx, att ->
            val num = idx + 1
            val safePath = att.path ?: "/data/data/com.termux/files/home/uploads/${att.name}"
            val attTagRegex = Regex("\\[(image|resim|görsel|dosya|file|doc|ek)[-_]?$num\\]", RegexOption.IGNORE_CASE)
            val isImg = att.type == "image" || att.name.endsWith(".png", true) || att.name.endsWith(".jpg", true) || att.name.endsWith(".jpeg", true) || att.name.endsWith(".webp", true) || att.name.endsWith(".gif", true)
            val label = if (isImg) "Ek Görsel #$num" else "Ek Dosya #$num"
            val inlineRef = "[$label (${att.name}): $safePath]"

            if (attTagRegex.containsMatchIn(processedPrompt)) {
                processedPrompt = attTagRegex.replace(processedPrompt) { inlineRef }
            } else {
                unplacedAttachments.add(Pair(num, att))
            }
        }

        // Metin içinde bahsedilmemiş diğer ekler varsa en sona ekle
        if (unplacedAttachments.isNotEmpty()) {
            val fileRefs = unplacedAttachments.joinToString("\n") { (num, a) ->
                val safePath = a.path ?: "/data/data/com.termux/files/home/uploads/${a.name}"
                "[Eklenen Dosya/Resim: $safePath] (Adı: ${a.name})"
            }
            val attachmentNotice = "\n\nKullanıcının mesaja eklediği diğer dosya ve görseller:\n$fileRefs\nLütfen ekteki bu dosya/görselleri de inceleyerek yanıt verin."
            processedPrompt = if (processedPrompt.isBlank()) fileRefs else "$processedPrompt$attachmentNotice"
        }

        val finalPrompt = processedPrompt.trim()

        val userMessage = Message(
            role = "user",
            content = userTypedText.ifBlank { finalPrompt },
            pastedBlocks = blocks,
            attachments = attachments
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

        val activeConvId = state.currentConversationId
        val isContinue = !activeConvId.isNullOrBlank()
        viewModelScope.launch {
            repository.sendMessage(
                prompt = finalPrompt,
                conversationId = if (isContinue) activeConvId else null,
                continueChat = isContinue,
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
                        currentSessionId = null,
                        currentConversationId = null,
                        inputText = "",
                        pastedBlocks = emptyList(),
                        attachments = emptyList(),
                        notice = null
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
