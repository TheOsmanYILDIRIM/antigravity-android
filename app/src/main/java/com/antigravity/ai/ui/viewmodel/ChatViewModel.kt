package com.antigravity.ai.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.ai.data.api.StreamEvent
import com.antigravity.ai.data.model.*
import com.antigravity.ai.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val conversations: List<ConversationMeta> = emptyList(),
    val currentSessionId: String? = null,
    val currentConversationId: String? = null,
    val inputText: String = "",
    val pastedBlock: String? = null,
    val attachments: List<Attachment> = emptyList(),
    val settings: ChatSettings = ChatSettings(),
    val vaultFiles: List<VaultItem> = emptyList(),
    val isGenerating: Boolean = false,
    val isListening: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showVaultBrowser: Boolean = false,
    val showSlashCommands: Boolean = false,
    val slashQuery: String = "",
    val showMentions: Boolean = false,
    val mentionQuery: String = "",
    val errorMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository()
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        initSpeechRecognizer()
        observeEvents()
        refreshAll()
    }

    fun refreshAll() {
        fetchConversations()
        syncWithServer()
        fetchVaultFiles()
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

    private fun observeEvents() {
        viewModelScope.launch {
            repository.observeStreamEvents().collect { event ->
                when (event) {
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
                        fetchConversations()
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
                    is StreamEvent.Error -> {
                        _uiState.update { state ->
                            val list = state.messages.toMutableList()
                            if (list.isNotEmpty() && list.last().role == "bot") {
                                val last = list.last()
                                val updated = last.copy(
                                    content = last.content + "\n\n⚠️ *Hata: ${event.message}*",
                                    state = MessageState.ERROR
                                )
                                list[list.size - 1] = updated
                            }
                            state.copy(messages = list, isGenerating = false, errorMessage = event.message)
                        }
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

    // Smart Paste Collapsing & Slash/Mention Detection
    fun onInputTextChange(newText: String) {
        // Check if a huge block of text was pasted (> 200 chars or > 4 lines)
        val isHugePaste = newText.length > 200 || newText.lines().size > 4
        if (isHugePaste && _uiState.value.pastedBlock == null && _uiState.value.inputText.isEmpty()) {
            _uiState.update {
                it.copy(
                    inputText = "",
                    pastedBlock = newText
                )
            }
            return
        }

        // Slash command trigger
        val isSlash = newText.startsWith("/")
        // Mention trigger
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

    fun removePastedBlock() {
        _uiState.update { it.copy(pastedBlock = null) }
    }

    fun addAttachment(attachment: Attachment) {
        _uiState.update { it.copy(attachments = it.attachments + attachment) }
    }

    fun removeAttachment(attachment: Attachment) {
        _uiState.update { it.copy(attachments = it.attachments.filter { a -> a != attachment }) }
    }

    fun updateSettings(newSettings: ChatSettings) {
        _uiState.update { it.copy(settings = newSettings) }
    }

    fun setSettingsDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showSettingsDialog = visible) }
    }

    fun setVaultBrowserVisible(visible: Boolean) {
        _uiState.update { it.copy(showVaultBrowser = visible) }
    }

    fun sendMessage() {
        val state = _uiState.value
        var finalPrompt = state.inputText.trim()

        if (state.pastedBlock != null) {
            val block = state.pastedBlock.trim()
            finalPrompt = if (finalPrompt.isEmpty()) block else "$finalPrompt\n\n```\n$block\n```"
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
                pastedBlock = null,
                attachments = emptyList(),
                showSlashCommands = false,
                showMentions = false,
                messages = it.messages + userMessage + botPlaceholder,
                isGenerating = true
            )
        }

        viewModelScope.launch {
            repository.sendMessage(
                prompt = finalPrompt,
                continueChat = true,
                settings = state.settings,
                attachments = userMessage.attachments
            )
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

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }
}
