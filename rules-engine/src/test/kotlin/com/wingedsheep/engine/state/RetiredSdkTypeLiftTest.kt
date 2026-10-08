package com.wingedsheep.engine.state

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantHexproofFromToGroup
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.SkipStepOrPhase
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.TriggeringEntityWas
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SkipDuration
import com.wingedsheep.sdk.scripting.effects.SkipStepOrPhaseEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Games persisted before the SDK consolidation hold retired discriminators inside card scripts.
 * [RetiredSdkTypeLift] must rewrite each one to the shape its cards author today — decoded, the
 * lifted JSON is exactly what the migrated card builds — and leave current JSON untouched.
 */
class RetiredSdkTypeLiftTest : FunSpec({

    val json = Json {
        encodeDefaults = true
        classDiscriminator = "type"
    }

    fun old(type: String, vararg fields: Pair<String, JsonElement>) =
        JsonObject(mapOf("type" to JsonPrimitive(type)) + fields)

    fun <T> lifted(serializer: KSerializer<T>, element: JsonElement): T =
        json.decodeFromJsonElement(serializer, RetiredSdkTypeLift.lift(element))

    fun target(value: EffectTarget) = json.encodeToJsonElement(EffectTarget.serializer(), value)

    test("the X comparisons become CompareNumericProperty over XValue") {
        listOf(
            "PowerEqualsX" to CardPredicate.CompareNumericProperty(CardNumericProperty.POWER, ComparisonOperator.EQ, DynamicAmount.XValue),
            "PowerAtLeastX" to CardPredicate.CompareNumericProperty(CardNumericProperty.POWER, ComparisonOperator.GTE, DynamicAmount.XValue),
            "ToughnessAtMostX" to CardPredicate.CompareNumericProperty(CardNumericProperty.TOUGHNESS, ComparisonOperator.LTE, DynamicAmount.XValue),
            "ManaValueEqualsX" to CardPredicate.CompareNumericProperty(CardNumericProperty.MANA_VALUE, ComparisonOperator.EQ, DynamicAmount.XValue),
            "ManaValueAtMostX" to CardPredicate.CompareNumericProperty(CardNumericProperty.MANA_VALUE, ComparisonOperator.LTE, DynamicAmount.XValue),
        ).forEach { (type, expected) ->
            lifted(CardPredicate.serializer(), old(type)) shouldBe expected
        }
    }

    test("the name-not-shared predicates become a negated SharesNameWithPermanentYouControl") {
        lifted(CardPredicate.serializer(), old("NameNotSharedWithControlledToken")) shouldBe
            CardPredicate.Not(CardPredicate.SharesNameWithPermanentYouControl(GameObjectFilter.Token, excludeSelf = false))
        lifted(CardPredicate.serializer(), old("NameNotSharedWithAnotherControlledPermanent")) shouldBe
            CardPredicate.Not(CardPredicate.SharesNameWithPermanentYouControl(GameObjectFilter.Permanent, excludeSelf = true))
    }

    test("the life conditions keep their threshold") {
        val threshold = "threshold" to JsonPrimitive(10)
        lifted(Condition.serializer(), old("AnOpponentLifeAtMost", threshold)) shouldBe Conditions.AnOpponentLifeAtMost(10)
        lifted(Condition.serializer(), old("EachPlayerLifeAtMost", threshold)) shouldBe Conditions.EachPlayerLifeAtMost(10)
        lifted(Condition.serializer(), old("APlayerLifeAtMost", threshold)) shouldBe Conditions.APlayerLifeAtMost(10)
    }

    test("the last-known type checks become TriggeringEntityWas") {
        lifted(Condition.serializer(), old("TriggeringEntityHadSubtype", "subtype" to JsonPrimitive("Demon"))) shouldBe
            TriggeringEntityWas(GameObjectFilter.Any.withSubtype(Subtype("Demon")))
        lifted(Condition.serializer(), old("TriggeringEntityHadCardType", "cardType" to JsonPrimitive("CREATURE"))) shouldBe
            TriggeringEntityWas(GameObjectFilter.Creature)
    }

    test("the skip effects keep their target and gain a duration") {
        val triggering = target(EffectTarget.PlayerRef(Player.TriggeringPlayer))
        lifted(Effect.serializer(), old("SkipNextDrawStep", "target" to target(EffectTarget.Controller))) shouldBe
            SkipStepOrPhaseEffect(TurnPart.DRAW_STEP, SkipDuration.NEXT, EffectTarget.Controller)
        lifted(Effect.serializer(), old("SkipNextUntapStep", "target" to triggering)) shouldBe
            SkipStepOrPhaseEffect(TurnPart.UNTAP_STEP, SkipDuration.NEXT, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        lifted(
            Effect.serializer(),
            old("SkipStepOrPhaseThisTurn", "part" to JsonPrimitive("COMBAT_PHASE"), "target" to triggering)
        ) shouldBe SkipStepOrPhaseEffect(TurnPart.COMBAT_PHASE, SkipDuration.THIS_TURN, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    test("the standing skips and hexproof grants keep their player and filter") {
        val each = json.encodeToJsonElement(Player.serializer(), Player.Each)
        lifted(StaticAbility.serializer(), old("SkipUntapStep", "player" to each)) shouldBe
            SkipStepOrPhase(TurnPart.UNTAP_STEP, Player.Each)
        lifted(StaticAbility.serializer(), old("SkipDrawStep")) shouldBe SkipStepOrPhase(TurnPart.DRAW_STEP)
        val source = json.encodeToJsonElement(GroupFilter.serializer(), GroupFilter.source())
        lifted(StaticAbility.serializer(), old("GrantHexproofFromMulticoloredToGroup", "filter" to source)) shouldBe
            GrantHexproofFromToGroup(ProtectionScope.Multicolored, GroupFilter.source())
    }

    test("a retired object nested deep in a document is rewritten in place") {
        val doc = JsonObject(mapOf("cards" to JsonArray(listOf(JsonObject(mapOf("filter" to old("PowerAtLeastX")))))))
        val expected = json.encodeToJsonElement(
            CardPredicate.serializer(),
            CardPredicate.CompareNumericProperty(CardNumericProperty.POWER, ComparisonOperator.GTE, DynamicAmount.XValue)
        )
        RetiredSdkTypeLift.lift(doc) shouldBe
            JsonObject(mapOf("cards" to JsonArray(listOf(JsonObject(mapOf("filter" to expected))))))
    }

    test("a document with no retired type passes through as the same instance") {
        val current = json.encodeToJsonElement(
            Effect.serializer(),
            SkipStepOrPhaseEffect(TurnPart.DRAW_STEP, SkipDuration.NEXT)
        )
        RetiredSdkTypeLift.lift(current) shouldBeSameInstanceAs current
    }
})
