package com.wingedsheep.engine.event

import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.effects.MayEffect
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * The build-time guard on the `effectOncePerTurn` lowering (CR 603.2h).
 *
 * `TriggerProcessor` places the *spending* budget gate inside the ability's consent gate, which is
 * what makes declining a "you may" free — but it only looks for that gate at the top of the effect
 * or at the tail of a `CompositeEffect` (the "do X, then you may Y" payoff shape, Planetarium of Wan
 * Shi Tong). A "you may" anywhere else would leave the budget gate *outside* it, so declining would
 * spend the turn's single use: the exact defect the flag exists to fix, and invisible at the table
 * because the prompt looks identical either way.
 *
 * `loweredEffectBudget` therefore throws on that combination. This sweep is what keeps that throw
 * theoretical: the condition depends only on the card definition, never on game state, so a card
 * pool that passes here can never trip it mid-game.
 */
class EffectOncePerTurnLoweringTest : DescribeSpec({

    /** Every script in the card pool, labelled — front faces, permanent backs, and spell faces. */
    fun allScripts(): List<Pair<String, CardScript>> = TestCards.all.flatMap { card ->
        buildList {
            add(card.name to card.script)
            card.backFace?.let { add(it.name to it.script) }
            card.cardFaces.forEach { add("${card.name} // ${it.name}" to it.script) }
        }
    }

    fun cappedAbilities() = allScripts().flatMap { (label, script) ->
        script.triggeredAbilities.filter { it.effectOncePerTurn }.map { label to it }
    }

    describe("every shipped 'Do this only once each turn' ability") {

        it("keeps its consent gate where the lowering looks for it") {
            val misplaced = cappedAbilities()
                .filter { (_, ability) -> TriggerProcessor.consentGateIsMisplaced(ability.effect) }
                .map { (label, ability) -> "$label — ${ability.effect}" }

            withClue(
                "the budget gate would land outside these abilities' 'you may', so declining " +
                    "would spend the turn's use (CR 603.2h): $misplaced"
            ) {
                misplaced shouldBe emptyList()
            }
        }

        it("lowers without throwing") {
            cappedAbilities().forEach { (label, ability) ->
                withClue(label) {
                    shouldNotThrowAny { TriggerProcessor.loweredEffectBudget(ability.effect, ability.id) }
                }
            }
        }

        it("is actually present in the pool, so the sweep above isn't vacuous") {
            // Guards against the sweep passing silently because a rename or a filter bug made
            // `effectOncePerTurn` invisible. Nine abilities carry the rider today: the two MSH
            // cards and the seven migrated off the trigger cap.
            cappedAbilities().shouldNotBeEmpty()
        }
    }

    describe("the guard itself") {

        val payoff = MayEffect(Effects.DrawCards(1))

        it("accepts a consent gate at the top of the effect") {
            TriggerProcessor.consentGateIsMisplaced(payoff) shouldBe false
        }

        it("accepts one at the tail of a composite — the Planetarium 'do X, then you may Y' shape") {
            val tail = Effects.Composite(Effects.GainLife(1), payoff)
            TriggerProcessor.consentGateIsMisplaced(tail) shouldBe false
        }

        it("rejects one buried mid-composite, where the budget would sit outside it") {
            val buried = Effects.Composite(payoff, Effects.GainLife(1))
            TriggerProcessor.consentGateIsMisplaced(buried) shouldBe true
        }

        it("accepts a mandatory ability, which has no consent gate to be inside of") {
            TriggerProcessor.consentGateIsMisplaced(Effects.GainLife(1)) shouldBe false
        }

        it("makes the lowering throw with an actionable message rather than mis-placing the gate") {
            val buried = Effects.Composite(payoff, Effects.GainLife(1))
            val failure = shouldThrow<IllegalArgumentException> {
                TriggerProcessor.loweredEffectBudget(buried, AbilityId("test"))
            }
            failure.message!! shouldContain "top of the effect or the tail of a CompositeEffect"
        }
    }
})
