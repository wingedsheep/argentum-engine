package com.wingedsheep.engine.hygiene

import com.wingedsheep.engine.core.engineSerializersModule
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.full.createType

/**
 * Guards GameState persistence against the JSON class-discriminator collision: the game
 * server's `persistenceJson` (and kotlinx's default) uses `classDiscriminator = "type"`, and
 * kotlinx.serialization throws [kotlinx.serialization.json.JsonEncodingException] the moment a
 * polymorphic leaf whose own JSON field is named "type" is encoded.
 *
 * The bug this pins: `SerializableModification.AddType(val type: String)` crashed
 * `RedisGameRepository.save` the first time a permanent animation (Tendril of the Mycotyrant's
 * `BecomeCreature` → `AddType("CREATURE")` floating effect) was in the saved state. The
 * property is now `@SerialName("cardType")`; this test fails on any future leaf that
 * reintroduces a colliding field, at test time instead of mid-game.
 */
@OptIn(ExperimentalSerializationApi::class)
class FloatingEffectSerializationRoundTripTest : FunSpec({

    // Same discriminator the game server's persistenceJson uses (also kotlinx's default).
    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        allowStructuredMapKeys = true
        serializersModule = engineSerializersModule
    }

    fun sealedLeaves(klass: KClass<*>): List<KClass<*>> =
        klass.sealedSubclasses.flatMap { sub ->
            if (sub.sealedSubclasses.isEmpty()) listOf(sub) else sealedLeaves(sub)
        }

    test("no SerializableModification leaf has a JSON field colliding with the 'type' class discriminator") {
        val leaves = sealedLeaves(SerializableModification::class)
        leaves.isNotEmpty() shouldBe true
        leaves.forEach { leaf ->
            val descriptor = serializer(leaf.createType()).descriptor
            val fieldNames = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }
            withClue("${leaf.simpleName} would throw JsonEncodingException on save — rename the field with @SerialName") {
                fieldNames shouldNotContain "type"
            }
        }
    }

    test("an ActiveFloatingEffect carrying AddType round-trips through persistence-shaped JSON") {
        val effect = ActiveFloatingEffect(
            id = EntityId("floating-1"),
            effect = FloatingEffectData(
                layer = Layer.TYPE,
                modification = SerializableModification.AddType("CREATURE"),
                affectedEntities = setOf(EntityId("land-1"))
            ),
            duration = Duration.Permanent,
            sourceId = EntityId("tendril-1"),
            sourceName = "Tendril of the Mycotyrant",
            controllerId = EntityId("player-1"),
            timestamp = 42L
        )
        val encoded = json.encodeToString(ActiveFloatingEffect.serializer(), effect)
        json.decodeFromString(ActiveFloatingEffect.serializer(), encoded) shouldBe effect
    }

    test("an ActiveFloatingEffect carrying RemoveType round-trips through persistence-shaped JSON") {
        val effect = ActiveFloatingEffect(
            id = EntityId("floating-2"),
            effect = FloatingEffectData(
                layer = Layer.TYPE,
                modification = SerializableModification.RemoveType("CREATURE"),
                affectedEntities = setOf(EntityId("perm-1"))
            ),
            duration = Duration.Permanent,
            sourceId = null,
            controllerId = EntityId("player-1"),
            timestamp = 43L
        )
        val encoded = json.encodeToString(ActiveFloatingEffect.serializer(), effect)
        json.decodeFromString(ActiveFloatingEffect.serializer(), encoded) shouldBe effect
    }
    test("an ActiveFloatingEffect carrying RemoveSubtype round-trips through persistence-shaped JSON") {
        val effect = ActiveFloatingEffect(
            id = EntityId("floating-3"),
            effect = FloatingEffectData(
                layer = Layer.TYPE,
                modification = SerializableModification.RemoveSubtype("Equipment"),
                affectedEntities = setOf(EntityId("sword-1"))
            ),
            duration = Duration.Permanent,
            sourceId = EntityId("sword-1"),
            controllerId = EntityId("player-1"),
            timestamp = 44L
        )
        val encoded = json.encodeToString(ActiveFloatingEffect.serializer(), effect)
        json.decodeFromString(ActiveFloatingEffect.serializer(), encoded) shouldBe effect
    }
})
