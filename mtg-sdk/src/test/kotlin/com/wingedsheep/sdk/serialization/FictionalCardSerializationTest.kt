package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.*
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests JSON round-trip serialization for a fictional card that exercises
 * multiple sealed interface types: triggered ability (ETB), activated ability,
 * keyword ability, and composite costs.
 *
 * Argentum Sentinel
 * {2}{W}
 * Creature — Human Soldier
 * 3/2
 * First strike
 * When Argentum Sentinel enters the battlefield, you gain 2 life.
 * {T}, Pay 1 life: Argentum Sentinel deals 1 damage to target creature.
 */
class FictionalCardSerializationTest : DescribeSpec({

    val json = CardSerialization.json

    val ArgentumSentinel = card("Argentum Sentinel") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human Soldier"
        power = 3
        toughness = 2
        oracleText = "First strike\nWhen Argentum Sentinel enters the battlefield, you gain 2 life.\n{T}, Pay 1 life: Argentum Sentinel deals 1 damage to target creature."

        keywordAbility(KeywordAbility.Simple(Keyword.FIRST_STRIKE))

        triggeredAbility {
            trigger = OnEnterBattlefield()
            effect = GainLifeEffect(2)
        }

        activatedAbility {
            cost = AbilityCost.Composite(listOf(AbilityCost.Tap, AbilityCost.PayLife(1)))
            target = Targets.Creature
            effect = DealDamageEffect(1, EffectTarget.ContextTarget(0))
        }
    }

    describe("Argentum Sentinel round-trip serialization") {

        it("should serialize to JSON with correct type discriminators") {
            val serialized = CardLoader.toJson(ArgentumSentinel)

            // Verify type discriminators are present
            serialized shouldContain "\"type\""

            // Verify effect types
            serialized shouldContain "GainLife"
            serialized shouldContain "DealDamage"

            // Verify trigger type
            serialized shouldContain "EntersBattlefield"

            // Verify keyword
            serialized shouldContain "FIRST_STRIKE"

            // Verify cost types
            serialized shouldContain "CostComposite"
            serialized shouldContain "CostTap"
            serialized shouldContain "CostPayLife"
        }

        it("should round-trip the full card definition") {
            val serialized = CardLoader.toJson(ArgentumSentinel)
            val deserialized = CardLoader.fromJson(serialized)

            // Basic properties
            deserialized.name shouldBe "Argentum Sentinel"
            deserialized.manaCost shouldBe ManaCost.parse("{2}{W}")
            deserialized.creatureStats!!.basePower shouldBe 3
            deserialized.creatureStats!!.baseToughness shouldBe 2
        }

        it("should round-trip keyword abilities") {
            val serialized = CardLoader.toJson(ArgentumSentinel)
            val deserialized = CardLoader.fromJson(serialized)

            deserialized.keywordAbilities.size shouldBe 1
            val keyword = deserialized.keywordAbilities[0]
            keyword.shouldBeInstanceOf<KeywordAbility.Simple>()
            (keyword as KeywordAbility.Simple).keyword shouldBe Keyword.FIRST_STRIKE
        }

        it("should round-trip triggered ability with ETB trigger and gain life effect") {
            val serialized = CardLoader.toJson(ArgentumSentinel)
            val deserialized = CardLoader.fromJson(serialized)

            deserialized.script.triggeredAbilities.size shouldBe 1
            val trigger = deserialized.script.triggeredAbilities[0]
            trigger.trigger.shouldBeInstanceOf<OnEnterBattlefield>()
            trigger.effect.shouldBeInstanceOf<GainLifeEffect>()

            val effect = trigger.effect as GainLifeEffect
            effect.amount shouldBe DynamicAmount.Fixed(2)
        }

        it("should round-trip activated ability with composite cost") {
            val serialized = CardLoader.toJson(ArgentumSentinel)
            val deserialized = CardLoader.fromJson(serialized)

            deserialized.script.activatedAbilities.size shouldBe 1
            val ability = deserialized.script.activatedAbilities[0]

            // Verify composite cost
            ability.cost.shouldBeInstanceOf<AbilityCost.Composite>()
            val compositeCost = ability.cost as AbilityCost.Composite
            compositeCost.costs.size shouldBe 2
            compositeCost.costs[0].shouldBeInstanceOf<AbilityCost.Tap>()
            compositeCost.costs[1].shouldBeInstanceOf<AbilityCost.PayLife>()
            (compositeCost.costs[1] as AbilityCost.PayLife).amount shouldBe 1

            // Verify effect
            ability.effect.shouldBeInstanceOf<DealDamageEffect>()
            val dealDamage = ability.effect as DealDamageEffect
            dealDamage.amount shouldBe DynamicAmount.Fixed(1)
        }

        it("should produce valid JSON that can be parsed independently") {
            val serialized = CardLoader.toJson(ArgentumSentinel)

            // Parse as raw JSON to verify structure
            val jsonElement = json.parseToJsonElement(serialized)
            jsonElement.shouldBeInstanceOf<kotlinx.serialization.json.JsonObject>()
        }
    }
})
