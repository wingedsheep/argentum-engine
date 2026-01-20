package com.wingedsheep.gameserver.config

import com.wingedsheep.gameserver.websocket.GameWebSocketHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val gameWebSocketHandler: GameWebSocketHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(gameWebSocketHandler, "/game")
            .setAllowedOrigins("*")
    }

    /**
     * Configure WebSocket container with larger buffer sizes and session timeouts.
     * The default text buffer is too small for game state updates.
     */
    @Bean
    fun createWebSocketContainer(): ServletServerContainerFactoryBean {
        val container = ServletServerContainerFactoryBean()
        // 5 MB buffer for text messages (game state can be large with many entities)
        container.setMaxTextMessageBufferSize(5 * 1024 * 1024)
        container.setMaxBinaryMessageBufferSize(5 * 1024 * 1024)
        // Longer session timeout to prevent disconnection during long game sequences
        container.setMaxSessionIdleTimeout(300_000L) // 5 minutes
        return container
    }
}
