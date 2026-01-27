package com.wingedsheep.gameserver.handler

import com.wingedsheep.gameserver.protocol.ErrorCode
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.gameserver.session.SessionRegistry
import com.wingedsheep.engine.core.engineSerializersModule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession

@Component
class MessageSender(
    private val sessionRegistry: SessionRegistry
) {
    private val logger = LoggerFactory.getLogger(MessageSender::class.java)

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
        serializersModule = engineSerializersModule
    }

    fun send(session: WebSocketSession, message: ServerMessage) {
        val lock = sessionRegistry.getSessionLock(session.id)
        synchronized(lock) {
            try {
                if (session.isOpen) {
                    val jsonText = json.encodeToString(message)
                    session.sendMessage(TextMessage(jsonText))
                } else {
                    logger.warn("Cannot send message - session ${session.id} is closed")
                }
            } catch (e: Exception) {
                logger.error("Failed to send message to ${session.id}", e)
            }
        }
    }

    fun sendError(session: WebSocketSession, code: ErrorCode, message: String) {
        send(session, ServerMessage.Error(code, message))
    }
}
