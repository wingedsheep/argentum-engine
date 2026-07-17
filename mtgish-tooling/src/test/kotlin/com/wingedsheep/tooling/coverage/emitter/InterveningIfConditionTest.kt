package com.wingedsheep.tooling.coverage.emitter

import com.wingedsheep.tooling.coverage.J
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonObject

/**
 * Per-handler table test for the intervening-if condition layer: pins a handful of mtgish `_Condition`
 * fragments to the exact `Conditions.*` DSL that [interveningIfDsl] renders. Fast and local (no IR
 * download / compile); the committed golden remains the exhaustive net.
 *
 * Focus: the "you control ANOTHER <filter>" shape. The IR encodes "another" as an `And` whose first arm
 * is `Other(<self ref>)`; [youControlConditionDsl] must peel that clause and lift it to
 * `excludeSelf = true`, not silently drop it (which would widen "another Spirit" to any Spirit,
 * self-triggering the payoff).
 *
 * [interveningIfDsl] returns the single-line `render(...)` of the condition Call; the multi-line
 * wrapping seen in an emitted `.kt` file is a downstream source-formatting pass, not this string.
 */
class InterveningIfConditionTest : StringSpec({

    val ctx = EmitCtx(emptySet())
    fun obj(json: String): JsonObject = J.parseToJsonElement(json) as JsonObject
    fun condition(json: String): String? = ctx.interveningIfDsl(obj(json))

    // "you control a <filter>" wrapped in PlayerPassesFilter(You, ControlsA(<filter>)).
    fun youControl(controlsFilter: String): String? =
        condition("""{"_Condition":"PlayerPassesFilter","args":[{"_Player":"You"},""" +
            """{"_Players":"ControlsA","args":$controlsFilter}]}""")

    "Packsong Pup: 'another Wolf or Werewolf' lifts the Other(ThisPermanent) clause to excludeSelf" {
        youControl(
            """{"_Permanents":"And","args":[""" +
                """{"_Permanents":"Other","args":{"_Permanent":"ThisPermanent"}},""" +
                """{"_Permanents":"Or","args":[{"_Permanents":"IsCreatureType","args":"Wolf"},""" +
                """{"_Permanents":"IsCreatureType","args":"Werewolf"}]}]}""",
        ) shouldBe "Conditions.YouControl(GameObjectFilter.Creature.withAnyOfSubtypes(" +
            "listOf(Subtype.WOLF, Subtype.WEREWOLF)), excludeSelf = true)"
    }

    "Resistance Squad: 'another Human' lifts the Other(ThatEnteringPermanent) clause to excludeSelf" {
        youControl(
            """{"_Permanents":"And","args":[""" +
                """{"_Permanents":"Other","args":{"_Permanent":"ThatEnteringPermanent"}},""" +
                """{"_Permanents":"IsCreatureType","args":"Human"}]}""",
        ) shouldBe "Conditions.YouControl(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN), " +
            "excludeSelf = true)"
    }

    "without an Other(self) clause, 'you control a Spirit' renders WITHOUT excludeSelf (single line)" {
        // The plain "you control a <filter>" gate (no "another") must NOT gain excludeSelf — it stays a
        // one-arg Conditions.YouControl. Guards against the fix over-applying.
        youControl("""{"_Permanents":"IsCreatureType","args":"Spirit"}""") shouldBe
            "Conditions.YouControl(GameObjectFilter.Creature.withSubtype(Subtype.SPIRIT))"
    }
})
