package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AiRepository
import com.example.data.AssistantMessageEntity
import com.example.data.AssistantStreamEvent
import com.example.data.EcoRepository
import com.example.data.EcoState
import com.example.domain.ProfileDraft
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class AssistantChatUiState(
    val isStreaming: Boolean = false,
    val streamingReply: String = "",
    val error: String? = null
)

class AssistantViewModel(
    private val aiRepository: AiRepository,
    private val ecoRepository: EcoRepository
) : ViewModel() {

    val messages: StateFlow<List<AssistantMessageEntity>> = ecoRepository.assistantMessages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(AssistantChatUiState())
    val uiState: StateFlow<AssistantChatUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    fun sendMessage(
        text: String,
        ecoState: EcoState,
        profileDraft: ProfileDraft?
    ) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _uiState.value.isStreaming) return

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            val historySnapshot = messages.value.map { it.role to it.content }
            val userMsgId = UUID.randomUUID().toString()
            ecoRepository.saveAssistantMessage(userMsgId, "user", trimmed)

            _uiState.value = AssistantChatUiState(isStreaming = true, streamingReply = "", error = null)

            val latestRev = ecoRepository.billRevisions.firstOrNull()?.lastOrNull()
            aiRepository.streamAssistantReply(
                history = historySnapshot,
                userMessage = trimmed,
                ecoState = ecoState,
                latestRevision = latestRev,
                profileDraft = profileDraft
            ).collect { event ->
                when (event) {
                    is AssistantStreamEvent.Chunk -> {
                        _uiState.value = AssistantChatUiState(
                            isStreaming = true,
                            streamingReply = event.accumulatedText,
                            error = null
                        )
                    }
                    is AssistantStreamEvent.Completed -> {
                        ecoRepository.saveAssistantMessage(
                            id = UUID.randomUUID().toString(),
                            role = "assistant",
                            content = event.finalText
                        )
                        _uiState.value = AssistantChatUiState(
                            isStreaming = false,
                            streamingReply = "",
                            error = null
                        )
                    }
                    is AssistantStreamEvent.Error -> {
                        _uiState.value = AssistantChatUiState(
                            isStreaming = false,
                            streamingReply = "",
                            error = event.message
                        )
                    }
                }
            }
        }
    }

    fun clearChat() {
        streamJob?.cancel()
        viewModelScope.launch {
            ecoRepository.clearAssistantMessages()
            _uiState.value = AssistantChatUiState()
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    companion object {
        fun factory(
            aiRepository: AiRepository,
            ecoRepository: EcoRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AssistantViewModel(aiRepository, ecoRepository) as T
            }
    }
}
