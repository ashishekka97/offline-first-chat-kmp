package me.ashishekka.echo.shared.domain.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import me.ashishekka.echo.shared.data.file.LocalAssetManager
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.AppError
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.Result
import me.ashishekka.echo.shared.domain.model.*
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import me.ashishekka.echo.shared.util.Log
import kotlin.random.Random

/**
 * Service responsible for simulating AI agent interactions.
 */
interface AgentService {
    /**
     * A reactive map of chatId to its typing status.
     */
    val typingStates: StateFlow<Map<ChatId, Boolean>>

    /**
     * Triggers a simulated reply for the given [chatId].
     * The service internally handles debouncing and message counting.
     */
    fun triggerReply(chatId: ChatId)

    /**
     * Cancels any active simulations and stops background processing.
     */
    fun cancel()
}

/**
 * Default implementation of [AgentService].
 */
@OptIn(FlowPreview::class)
class DefaultAgentService(
    private val messageRepository: MessageRepository,
    private val localAssetManager: LocalAssetManager,
    private val idGenerator: IdGenerator,
    private val dispatcherProvider: DispatcherProvider,
    private val scope: CoroutineScope = CoroutineScope(dispatcherProvider.default + SupervisorJob()),
    private val clock: Clock = Clock.System
) : AgentService {

    private val _typingStates = MutableStateFlow<Map<ChatId, Boolean>>(emptyMap())
    override val typingStates: StateFlow<Map<ChatId, Boolean>> = _typingStates.asStateFlow()

    // Tracks if the user is currently typing in a chat
    private val userTypingStates = MutableStateFlow<Map<ChatId, Boolean>>(emptyMap())

    private val triggerFlow = MutableSharedFlow<ChatId>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // Mutex to guard state mutations for thread safety
    private val mutex = Mutex()

    // Tracks user message count per chat to trigger reply every 4-5 messages
    private val messageCounters = mutableMapOf<ChatId, Int>()
    
    // Tracks active simulation jobs to handle debouncing
    private val simulationJobs = mutableMapOf<ChatId, Job>()

    init {
        // Requirement: Don't trigger if user rapidly sends multiple messages (debounce)
        triggerFlow
            .debounce(500) // Wait for 500ms of inactivity before starting simulation
            .onEach { chatId ->
                mutex.withLock {
                    startSimulationLocked(chatId)
                }
            }
            .launchIn(scope)
    }

    override fun triggerReply(chatId: ChatId) {
        Log.d("AgentService", "triggerReply called for chat $chatId")
        scope.launch {
            mutex.withLock {
                val currentCount = (messageCounters[chatId] ?: 0) + 1
                messageCounters[chatId] = currentCount
                Log.d("AgentService", "Message count for $chatId: $currentCount")

                // Requirement: Trigger reply every 4th or 5th message (randomized for variety)
                val triggerThreshold = if (Random.nextBoolean()) 4 else 5
                Log.d("AgentService", "Trigger threshold: $triggerThreshold")
                
                if (currentCount >= triggerThreshold) {
                    Log.d("AgentService", "Threshold reached, emitting trigger to debounced flow")
                    messageCounters[chatId] = 0 // Reset counter
                    triggerFlow.tryEmit(chatId)
                }
            }
        }
    }

    override fun cancel() {
        scope.cancel()
    }

    /**
     * Starts the simulation. MUST be called within a [mutex] lock to ensure
     * [simulationJobs] is updated safely and existing jobs are cancelled.
     */
    private fun startSimulationLocked(chatId: ChatId) {
        // Cancel any existing simulation for this chat
        simulationJobs[chatId]?.cancel()
        
        simulationJobs[chatId] = scope.launch {
            try {
                yield() 
                
                _typingStates.update { it + (chatId to true) }
                
                // 1. Thinking delay (1-2 seconds)
                delay(Random.nextLong(1000, 2000))
                
                if (!isActive) return@launch // Double check if cancelled during delay

                // 2. Generate randomized reply
                val isImageReply = Random.nextFloat() < 0.3 // 30% chance for image
                
                val messageId = MessageId(idGenerator.generateUuid())
                val timestamp = clock.now().toEpochMilliseconds()

                val result = if (isImageReply) {
                    sendImageReply(messageId, chatId, timestamp)
                } else {
                    sendTextReply(messageId, chatId, timestamp)
                }
                
                // Hardening: Handle repository failures gracefully in simulation
                if (result is Result.Failure) {
                    // Log error or handle as needed for simulation resilience
                    Log.e("AgentService", "Agent simulation failed for chat $chatId: ${result.error}")
                }
            } finally {
                // Ensure typing state is always cleared even on cancellation/error
                _typingStates.update { it + (chatId to false) }
            }
        }
    }

    private suspend fun sendTextReply(id: MessageId, chatId: ChatId, timestamp: Long): Result<Unit, AppError> {
        val reply = TEXT_REPLIES.random()
        return messageRepository.sendMessage(
            id = id,
            chatId = chatId,
            senderId = Constants.DEFAULT_AGENT_ID,
            message = reply,
            type = MessageType.TEXT,
            timestamp = timestamp
        )
    }

    private suspend fun sendImageReply(id: MessageId, chatId: ChatId, timestamp: Long): Result<Unit, AppError> {
        // Requirement: Offline-First. We only reuse existing local media.
        // To maintain the 70/30 ratio, we first try to find media in the current chat,
        // then fall back to ANY available media across the entire app (e.g., from seed data or other chats).
        val mediaResult = messageRepository.getFilePathsForChat(chatId)
        var existingMedia = if (mediaResult is Result.Success) {
            mediaResult.data.filter { path ->
                localAssetManager.exists(path)
            }
        } else {
            emptyList()
        }

        // If current chat is empty, look globally to fulfill the 30% image requirement
        if (existingMedia.isEmpty()) {
            val globalMediaResult = messageRepository.getAllLocalMediaPaths()
            if (globalMediaResult is Result.Success) {
                existingMedia = globalMediaResult.data.filter { path ->
                    localAssetManager.exists(path)
                }
            }
        }

        if (existingMedia.isEmpty()) {
            // Ultimate fallback: if there is NO media anywhere, we use one of the bundled assets.
            // We randomly select one of 5 bundled fallback images for variety.
            val fallbackIndex = Random.nextInt(1, 6)
            val fallbackFileName = "agent_fallback_$fallbackIndex.jpg"
            
            if (!localAssetManager.exists(fallbackFileName)) {
                val copyResult = localAssetManager.copyBundledAssetToLocal(fallbackFileName)
                if (copyResult is Result.Failure) {
                    // If copying fails (e.g., asset missing), fall back to text
                    return sendTextReply(id, chatId, timestamp)
                }
            }
            
            return messageRepository.sendMessage(
                id = id,
                chatId = chatId,
                senderId = Constants.DEFAULT_AGENT_ID,
                message = "I found this for you!",
                type = MessageType.FILE,
                file = FileDetails(
                    path = fallbackFileName,
                    fileSize = 0,
                    thumbnail = null
                ),
                timestamp = timestamp
            )
        }

        val assetPath = existingMedia.random()
        val caption = "Check this out again!"

        return messageRepository.sendMessage(
            id = id,
            chatId = chatId,
            senderId = Constants.DEFAULT_AGENT_ID,
            message = caption,
            type = MessageType.FILE,
            file = FileDetails(
                path = assetPath,
                fileSize = 0,
                thumbnail = null
            ),
            timestamp = timestamp
        )
    }

    companion object {
        private val TEXT_REPLIES = listOf(
            "That's a great point! I hadn't thought of it that way.",
            "I'm echoing your thoughts on this.",
            "Can you tell me more about that?",
            "Interesting. Let me process that for a moment...",
            "I agree with you completely.",
            "That sounds like a solid plan.",
            "How does that make you feel?",
            "I'm here to listen. Go on.",
            "Processing... Done! I think I understand now.",
            "Echoing back: acknowledged."
        )
    }
}
