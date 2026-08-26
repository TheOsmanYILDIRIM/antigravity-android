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
    val conversations: List<Conversation> = emptyList(),
    val currentConversationId: String? = null,
    val inputText: String = "",
    val isGenerating: Boolean = false,
    val isListening: Boolean = false,
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
        syncWithServer()
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
                    }
                    is StreamEvent.SessionReset -> {
                        _uiState.update { it.copy(messages = emptyList(), isGenerating = false) }
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

    fun syncWithServer() {
        viewModelScope.launch {
            repository.fetchSession().onSuccess { response ->
                val serverMessages = response.session?.messages?.map { sessionMsg ->
                    Message(
                        role = sessionMsg.role,
                        content = sessionMsg.content ?: "",
                        tools = sessionMsg.tools?.toMutableList() ?: mutableListOf(),
                        usage = sessionMsg.usage,
                        state = if (sessionMsg.state == "generating") MessageState.GENERATING else MessageState.DONE
                    )
                } ?: emptyList()

                _uiState.update {
                    it.copy(
                        messages = serverMessages,
                        isGenerating = response.isGenerating,
                        currentConversationId = response.session?.conversationId
                    )
                }
            }
        }
    }

    fun onInputTextChange(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty() || _uiState.value.isGenerating) return

        val userMessage = Message(role = "user", content = text)
        val botPlaceholder = Message(role = "bot", content = "", state = MessageState.GENERATING)

        _uiState.update {
            it.copy(
                inputText = "",
                messages = it.messages + userMessage + botPlaceholder,
                isGenerating = true
            )
        }

        viewModelScope.launch {
            repository.sendMessage(text, continueChat = true)
        }
    }

    fun startNewChat() {
        viewModelScope.launch {
            repository.startNewChat()
            _uiState.update {
                it.copy(messages = emptyList(), isGenerating = false, currentConversationId = null)
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
