package com.wingedsheep.gym.server.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.GlobalOpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Top-of-Swagger-page metadata + cleanup for Kotlin inline-class mangling.
 *
 * The per-endpoint docs are attached with `@Tag` / `@Operation`
 * annotations on the controller methods — see [EnvController][com.wingedsheep.gym.server.controller.EnvController]
 * and [MetaController][com.wingedsheep.gym.server.controller.MetaController].
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
                    HTTP façade over the JVM-side `MultiEnvService` from `:gym`.
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

    /**
     * Strips Kotlin's inline-class getter-name mangling from the generated
     * schemas.
     *
     * Kotlin's `@JvmInline value class EntityId(val value: String)` compiles
     * to a bytecode getter `getId-xxxxxxx()` so the JVM can distinguish it
     * from a regular `getId()` overload. Swagger's reflection-based schema
     * generator picks that mangled name up verbatim, producing OpenAPI
     * properties like `"id-v2tQoa0"` / `"ownerId-Z9UYGMk"` / `"agentToAct-Z9UYGMk"`.
     *
     * This customizer walks every component schema (including nested
     * `items`, `additionalProperties`, and composed `allOf`/`oneOf`/`anyOf`
     * schemas) and rewrites any property name matching
     * `"foo-<hash>"` back to `"foo"`, along with any corresponding entry in
     * the `required` list. Runtime JSON is unaffected — kotlinx.serialization
     * already emits the un-mangled names via its own `SerialDescriptor`;
     * this is only a docs concern.
     */
    @Bean
    fun stripInlineClassMangling(): GlobalOpenApiCustomizer = GlobalOpenApiCustomizer { openApi ->
        openApi.components?.schemas?.values?.forEach { unmangle(it) }
    }

    private fun unmangle(schema: Schema<*>?) {
        if (schema == null) return

        // Properties map + required list at this level
        val original = schema.properties
        if (original != null && original.isNotEmpty()) {
            val renamed = LinkedHashMap<String, Schema<Any>>(original.size)
            original.forEach { (name, value) ->
                renamed[stripHash(name)] = value
                unmangle(value)
            }
            schema.properties = renamed
            schema.required = schema.required?.map { stripHash(it) }
        }

        // Recurse into container schemas
        schema.items?.let { unmangle(it) }
        (schema.additionalProperties as? Schema<*>)?.let { unmangle(it) }
        schema.allOf?.forEach { unmangle(it) }
        schema.oneOf?.forEach { unmangle(it) }
        schema.anyOf?.forEach { unmangle(it) }
    }

    private fun stripHash(name: String): String =
        MANGLE_SUFFIX.replace(name, "")

    companion object {
        // Kotlin inline-class mangling: a short alphanumeric suffix preceded by `-`.
        // Conservative pattern — legitimate JSON property names in the contract are
        // camelCase without dashes.
        private val MANGLE_SUFFIX = Regex("-[A-Za-z0-9_]{5,}$")
    }
}
