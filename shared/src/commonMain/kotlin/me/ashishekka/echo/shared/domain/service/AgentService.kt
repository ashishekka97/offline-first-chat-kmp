package me.ashishekka.echo.shared.domain.service

import me.ashishekka.echo.shared.data.entity.FileDetails
import me.ashishekka.echo.shared.data.entity.MessageType
import me.ashishekka.echo.shared.domain.Constants
import me.ashishekka.echo.shared.domain.repository.MessageRepository
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlin.random.Random
import me.ashishekka.echo.shared.di.DispatcherProvider
import me.ashishekka.echo.shared.domain.Result
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Service responsible for simulating AI agent interactions.
 */
interface AgentService {
    /**
     * Triggers a simulated reply for the given [chatId].
     * The service internally handles debouncing and message counting.
     */
    fun triggerReply(chatId: String)
}

/**
 * Default implementation of [AgentService].
 */
class DefaultAgentService(
    private val messageRepository: MessageRepository,
    private val dispatcherProvider: DispatcherProvider,
    private val scope: CoroutineScope = CoroutineScope(dispatcherProvider.default + SupervisorJob()),
    private val clock: Clock = Clock.System
) : AgentService {

    // Mutex to guard state mutations for thread safety
    private val mutex = Mutex()

    // Tracks user message count per chat to trigger reply every 4-5 messages
    private val messageCounters = mutableMapOf<String, Int>()
    
    // Tracks active simulation jobs to handle debouncing
    private val simulationJobs = mutableMapOf<String, Job>()

    override fun triggerReply(chatId: String) {
        scope.launch {
            mutex.withLock {
                val currentCount = (messageCounters[chatId] ?: 0) + 1
                messageCounters[chatId] = currentCount

                // Trigger reply every 4th or 5th message (randomized for variety)
                val triggerThreshold = if (Random.nextBoolean()) 4 else 5
                
                if (currentCount >= triggerThreshold) {
                    messageCounters[chatId] = 0 // Reset counter
                    startSimulationLocked(chatId)
                }
            }
        }
    }

    /**
     * Starts the simulation. MUST be called within a [mutex] lock to ensure
     * [simulationJobs] is updated safely and existing jobs are cancelled.
     */
    private fun startSimulationLocked(chatId: String) {
        // Debounce: Cancel any existing simulation for this chat
        simulationJobs[chatId]?.cancel()
        
        simulationJobs[chatId] = scope.launch {
            // 1. Thinking delay (1-2 seconds)
            delay(Random.nextLong(1000, 2000))
            
            // 2. Generate randomized reply
            val isImageReply = Random.nextFloat() < 0.3 // 30% chance for image
            
            val messageId = "agent_msg_${clock.now().toEpochMilliseconds()}"
            val timestamp = clock.now().toEpochMilliseconds()

            if (isImageReply) {
                sendImageReply(messageId, chatId, timestamp)
            } else {
                sendTextReply(messageId, chatId, timestamp)
            }
        }
    }

    private suspend fun sendTextReply(id: String, chatId: String, timestamp: Long) {
        val reply = TEXT_REPLIES.random()
        messageRepository.sendMessage(
            id = id,
            chatId = chatId,
            senderId = Constants.DEFAULT_AGENT_ID,
            message = reply,
            type = MessageType.TEXT,
            timestamp = timestamp
        )
    }

    private suspend fun sendImageReply(id: String, chatId: String, timestamp: Long) {
        val (caption, assetName) = IMAGE_REPLIES.random()
        messageRepository.sendMessage(
            id = id,
            chatId = chatId,
            senderId = Constants.DEFAULT_AGENT_ID,
            message = caption,
            type = MessageType.FILE,
            file = FileDetails(
                path = assetName,
                fileSize = 0, // Not critical for simulation
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

        private val IMAGE_REPLIES = listOf(
            "Check this out!" to "seed_flight_screenshot.jpg",
            "I found these options for you." to "seed_flight_options.jpg",
            "Take a look at this data." to "seed_flight_screenshot.jpg"
        )
    }
}
