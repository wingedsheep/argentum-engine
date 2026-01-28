package com.wingedsheep.gameserver.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger/OpenAPI configuration for dev endpoints.
 *
 * Only enabled when game.dev-endpoints.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = ["game.dev-endpoints.enabled"], havingValue = "true")
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Argentum Engine - Dev API")
                    .description("""
                        Development endpoints for testing the Argentum MTG Engine.

                        **WARNING:** These endpoints are for development/testing only.
                        Never enable in production!

                        ## Quick Start

                        1. Use **POST /api/dev/scenarios** to create a test game
                        2. Copy the token from the response
                        3. Open the web client and connect using the token

                        ## Available Cards

                        Use **GET /api/dev/scenarios/cards** to see all available card names.
                    """.trimIndent())
                    .version("1.0.0")
            )
    }
}
