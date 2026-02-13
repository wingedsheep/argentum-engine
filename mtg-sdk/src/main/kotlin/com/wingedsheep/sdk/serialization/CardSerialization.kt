package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

/**
 * Central serialization configuration for card JSON loading.
 *
 * Uses custom string serializers for ManaCost and TypeLine so card authors
 * can write concise JSON like:
 * ```json
 * {
 *   "manaCost": "{2}{R}{R}",
 *   "typeLine": "Creature — Goblin Warrior"
 * }
 * ```
 *
 * Instead of the verbose structured form:
 * ```json
 * {
 *   "manaCost": {"symbols": [{"type": "Generic", "amount": 2}, ...]},
 *   "typeLine": {"supertypes": [], "cardTypes": ["CREATURE"], "subtypes": [...]}
 * }
 * ```
 */
object CardSerialization {

    /**
     * SerializersModule that registers contextual serializers for
     * human-friendly JSON representation of core types.
     */
    val module = SerializersModule {
        contextual(ManaCost::class, ManaCostStringSerializer)
        contextual(TypeLine::class, TypeLineStringSerializer)
    }

    /**
     * Json instance configured for card loading.
     *
     * Features:
     * - String shorthands for ManaCost and TypeLine via contextual serializers
     * - "type" class discriminator for sealed interface polymorphism
     * - Ignores unknown keys for forward compatibility
     * - Doesn't encode defaults to keep JSON minimal
     * - Lenient parsing for easier authoring (comments, trailing commas)
     */
    val json = Json {
        serializersModule = module
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = false
        isLenient = true
    }

    /**
     * Compact Json instance for export/storage (no pretty printing).
     */
    val compactJson = Json {
        serializersModule = module
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = false
    }
}
