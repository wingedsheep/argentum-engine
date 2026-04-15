package com.wingedsheep.gymserver.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Top-of-Swagger-page metadata. Renders at `/swagger-ui.html`.
 *
 * The per-endpoint docs are attached with `@Tag` / `@Operation`
 * annotations on the controller methods — see [EnvController][com.wingedsheep.gymserver.controller.EnvController]
 * and [MetaController][com.wingedsheep.gymserver.controller.MetaController].
 */
@Configuration
class OpenApiConfig {

    @Bean
    fun gymOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Argentum Engine Gym — HTTP Transport")
                .version("0.1.0")
                .description(
                    """
                    HTTP façade over the JVM-side `MultiEnvService` from `:engine-gym`.
                    Drives many concurrent MTG environments for RL / MCTS training loops.

                    Action IDs inside `TrainingObservation.legalActions` are valid only
                    for the observation they come in — never cache them across steps.

                    The `/schema-hash` endpoint lets Python clients fail fast on
                    contract drift without having to compare the full observation type.
                    """.trimIndent()
                )
                .license(License().name("MIT"))
        )
        .addServersItem(Server().url("http://localhost:8081").description("Local development"))
}
