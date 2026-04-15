package com.wingedsheep.gymserver.config

import kotlinx.serialization.json.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.KotlinSerializationJsonHttpMessageConverter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * Registers kotlinx.serialization as the primary JSON converter so that
 * `@Serializable` types from `:engine-gym` (EnvConfig, TrainingObservation,
 * DecisionResponse, etc.) round-trip natively — in particular, sealed
 * hierarchies with `@SerialName` discriminators (DeckSpec, DecisionResponse)
 * deserialize without extra Jackson adapter code.
 */
@Configuration
class WebConfig : WebMvcConfigurer {

    @Bean
    fun gymJson(): Json = Json {
        ignoreUnknownKeys = true
        // encodeDefaults so callers see every field on the wire. Flip off
        // later if payload size becomes a concern.
        encodeDefaults = true
        explicitNulls = false
    }

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        // extendMessageConverters runs after the defaults are registered;
        // inserting at index 0 makes our converter win over the default
        // Jackson one. The default Jackson converter stays in the chain
        // so non-@Serializable exception responses still work.
        converters.add(0, KotlinSerializationJsonHttpMessageConverter(gymJson()))
    }
}
