package com.wingedsheep.engine.state

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.SkipStepOrPhase
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWas
import com.wingedsheep.sdk.scripting.effects.SkipDuration
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads games persisted before the SDK consolidation of 2026-10 folded narrow types into general
 * ones. A card's script travels inside the persisted state (its spell effect, granted statics,
 * abilities on the stack), so an in-flight game can still hold a retired discriminator, and an
 * unknown polymorphic discriminator fails the whole decode. This rewrites each retired SDK object
 * to the shape its cards author today, before decoding. Objects with a current discriminator pass
 * through untouched.
 */
internal object RetiredSdkTypeLift {
    private val json = Json {
        encodeDefaults = true
        classDiscriminator = "type"
    }

    private fun <T> encode(serializer: KSerializer<T>, value: T): JsonObject =
        json.encodeToJsonElement(serializer, value).jsonObject

    private fun predicate(value: CardPredicate) = encode(CardPredicate.serializer(), value)
    private fun condition(value: Condition) = encode(Condition.serializer(), value)
    private fun static(value: StaticAbility) = encode(StaticAbility.serializer(), value)

    private fun xComparison(property: CardNumericProperty, operator: ComparisonOperator) =
        predicate(CardPredicate.CompareNumericProperty(property, operator, DynamicAmount.XValue))

    private fun skipEffect(part: TurnPart, duration: SkipDuration, target: JsonElement?) = JsonObject(
        buildMap {
            put("type", JsonPrimitive("SkipStepOrPhaseEffect"))
            put("part", JsonPrimitive(part.name))
            put("duration", JsonPrimitive(duration.name))
            if (target != null) put("target", target)
        }
    )

    private fun hexproofGrant(scope: ProtectionScope, old: JsonObject) = JsonObject(
        buildMap {
            put("type", JsonPrimitive("GrantHexproofFromToGroup"))
            put("scope", encode(ProtectionScope.serializer(), scope))
            old["filter"]?.let { put("filter", it) }
        }
    )

    private fun nameNotShared(filter: GameObjectFilter, excludeSelf: Boolean) = predicate(
        CardPredicate.Not(CardPredicate.SharesNameWithPermanentYouControl(filter, excludeSelf))
    )

    private val cardTypeFilters = mapOf(
        "CREATURE" to GameObjectFilter.Creature,
        "ARTIFACT" to GameObjectFilter.Artifact,
        "ENCHANTMENT" to GameObjectFilter.Enchantment,
        "LAND" to GameObjectFilter.Land,
        "PLANESWALKER" to GameObjectFilter.Planeswalker,
    )

    private fun JsonObject.int(key: String): Int = getValue(key).jsonPrimitive.int
    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content

    /** Retired discriminator → the current object for it. */
    private val rewrites: Map<String, (JsonObject) -> JsonObject> = mapOf(
        // CardPredicate — the X comparisons and the name-not-shared pair
        "PowerEqualsX" to { _ -> xComparison(CardNumericProperty.POWER, ComparisonOperator.EQ) },
        "PowerAtLeastX" to { _ -> xComparison(CardNumericProperty.POWER, ComparisonOperator.GTE) },
        "ToughnessAtMostX" to { _ -> xComparison(CardNumericProperty.TOUGHNESS, ComparisonOperator.LTE) },
        "ManaValueEqualsX" to { _ -> xComparison(CardNumericProperty.MANA_VALUE, ComparisonOperator.EQ) },
        "ManaValueAtMostX" to { _ -> xComparison(CardNumericProperty.MANA_VALUE, ComparisonOperator.LTE) },
        "NameNotSharedWithControlledToken" to { _ -> nameNotShared(GameObjectFilter.Token, excludeSelf = false) },
        "NameNotSharedWithAnotherControlledPermanent" to { _ ->
            nameNotShared(GameObjectFilter.Permanent, excludeSelf = true)
        },
        // Condition — the life comparisons and the last-known type checks
        "AnOpponentLifeAtMost" to { old -> condition(Conditions.AnOpponentLifeAtMost(old.int("threshold"))) },
        "EachPlayerLifeAtMost" to { old -> condition(Conditions.EachPlayerLifeAtMost(old.int("threshold"))) },
        "APlayerLifeAtMost" to { old -> condition(Conditions.APlayerLifeAtMost(old.int("threshold"))) },
        "TriggeringEntityHadSubtype" to { old ->
            condition(TriggeringEntityWas(GameObjectFilter.Any.withSubtype(Subtype(old.string("subtype")))))
        },
        "TriggeringEntityHadCardType" to { old ->
            val cardType = old.string("cardType")
            val filter = cardTypeFilters[cardType.uppercase()]
                ?: throw UnsupportedGameStateFormatException("Unsupported GameState format: TriggeringEntityHadCardType($cardType)")
            condition(TriggeringEntityWas(filter))
        },
        // Condition — the cast-time flag reader, now a comparison over the numeric capture
        "CastTimeFlagSet" to { old -> condition(Conditions.CapturedAtCast(old.string("flag"))) },
        // Effect — the skip effects, now one type over a duration
        "SkipNextDrawStep" to { old -> skipEffect(TurnPart.DRAW_STEP, SkipDuration.NEXT, old["target"]) },
        "SkipNextUntapStep" to { old -> skipEffect(TurnPart.UNTAP_STEP, SkipDuration.NEXT, old["target"]) },
        "SkipStepOrPhaseThisTurn" to { old ->
            skipEffect(TurnPart.valueOf(old.string("part")), SkipDuration.THIS_TURN, old["target"])
        },
        // StaticAbility — the standing skips and the hexproof-from grants
        "SkipUntapStep" to { old ->
            JsonObject(
                buildMap {
                    put("type", JsonPrimitive("SkipStepOrPhase"))
                    put("part", JsonPrimitive(TurnPart.UNTAP_STEP.name))
                    old["player"]?.let { put("player", it) }
                }
            )
        },
        "SkipDrawStep" to { _ -> static(SkipStepOrPhase(TurnPart.DRAW_STEP)) },
        "GrantHexproofFromMonocoloredToGroup" to { old -> hexproofGrant(ProtectionScope.Monocolored, old) },
        "GrantHexproofFromMulticoloredToGroup" to { old -> hexproofGrant(ProtectionScope.Multicolored, old) },
    )

    /**
     * Retired *fields* on current types. The may-play `singleUse` flag became a play cap:
     * `GrantMayPlayFromExileEffect.maxPlays` in a card script, `MayPlayPermission.playsRemaining` on
     * a stored grant. A true flag is a cap of one; a false one is the uncapped default.
     */
    private fun withoutRetiredFields(element: JsonObject, type: String?): JsonObject =
        withoutCastTimeFlags(withoutSingleUse(element, type))

    /**
     * A spell on the stack recorded its "as you cast this spell" captures as the names whose
     * condition held (`castTimeFlags`); it now records every capture as a number (`castTimeValues`),
     * a condition capture being 1 when it held.
     */
    private fun withoutCastTimeFlags(element: JsonObject): JsonObject {
        val flags = element["castTimeFlags"] as? JsonArray ?: return element
        val values = JsonObject(flags.associate { it.jsonPrimitive.content to JsonPrimitive(1) })
        return JsonObject(element - "castTimeFlags" + ("castTimeValues" to values))
    }

    private fun withoutSingleUse(element: JsonObject, type: String?): JsonObject {
        val singleUse = element["singleUse"] as? JsonPrimitive ?: return element
        val capKey = if (type == "GrantMayPlayFromExile") "maxPlays" else "playsRemaining"
        val rest = element - "singleUse"
        return JsonObject(if (singleUse.content == "true") rest + (capKey to JsonPrimitive(1)) else rest)
    }

    /** Returns [element] itself (no copy) when nothing in it needs rewriting — the common case. */
    fun lift(element: JsonElement): JsonElement = when (element) {
        is JsonArray -> {
            val children = element.map(::lift)
            if (children.indices.all { children[it] === element[it] }) element else JsonArray(children)
        }
        is JsonObject -> {
            val type = (element["type"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            val rewritten = withoutRetiredFields(type?.let { rewrites[it] }?.invoke(element) ?: element, type)
            val children = rewritten.mapValues { (_, value) -> lift(value) }
            if (rewritten === element && children.all { (key, value) -> value === element[key] }) element
            else JsonObject(children)
        }
        else -> element
    }
}
