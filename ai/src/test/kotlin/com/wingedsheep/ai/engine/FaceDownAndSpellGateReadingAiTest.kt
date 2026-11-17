package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.dtk.cards.ObscuringAether
import com.wingedsheep.mtg.sets.definitions.dtk.cards.TapestryOfTheAges
import com.wingedsheep.mtg.sets.definitions.lgn.cards.WallOfDeceit
import com.wingedsheep.mtg.sets.definitions.ons.cards.Backslide
import com.wingedsheep.mtg.sets.definitions.ons.cards.DreamChisel
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Three Dragons of Tarkir readings:
 *  - `TurnFaceDownEffect` aimed at another creature (Backslide) leaves it a nameless 2/2 with no
 *    abilities — a neutralizing answer aimed at the opponent's side — while a permanent turning
 *    itself face down (Obscuring Aether, Wall of Deceit) is a self-animation that needs no intent.
 *  - "Face-down creature spells you cast cost {1} less" (Obscuring Aether, Dream Chisel) is a
 *    discount on our own spells, the cost reducer a "spells you cast" discount reads as.
 *  - "Activate only if you've cast a noncreature spell this turn" (Tapestry of the Ages) cashes in
 *    the deck's instants and sorceries: a spells payoff, unlike a gate any spell opens.
 */
class FaceDownAndSpellGateReadingAiTest : FunSpec({

    test("turning another creature face down is a harmful neutralize; turning itself face down needs no intent") {
        val spell = Backslide.script
        TargetPolarityAnalyzer.forRequirements(spell.spellEffect!!, spell.targetRequirements, source = Backslide) shouldBe
            listOf(TargetPolarity.HARMFUL)
        CardIntentAnalyzer.analyze(Backslide).tags shouldContain IntentTag.NEUTRALIZE
        withClue("Backslide reads fully") { AiReadingAudit.read(Backslide).readFully shouldBe true }
        listOf(ObscuringAether, WallOfDeceit).forEach { selfFlipper ->
            withClue("${selfFlipper.name} reads fully") { AiReadingAudit.read(selfFlipper).readFully shouldBe true }
            withClue("${selfFlipper.name} turning itself face down is no answer") {
                CardIntentAnalyzer.analyze(selfFlipper).tags shouldNotContain IntentTag.NEUTRALIZE
            }
        }
    }

    test("a discount on our face-down spells reads as a cost reducer") {
        listOf(ObscuringAether, DreamChisel).forEach { reducer ->
            withClue(reducer.name) { CardIntentAnalyzer.analyze(reducer).tags shouldContain IntentTag.COST_REDUCER }
        }
    }

    test("an ability gated on a noncreature spell this turn rewards the spells axis; one any spell opens does not") {
        SynergyAxes.of(TapestryOfTheAges).rewards shouldContain SynergyAxis.SPELLS
        val anySpell = card("Any-spell gate fixture") {
            manaCost = "{4}"
            typeLine = "Artifact"
            activatedAbility {
                cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
                restrictions = listOf(
                    ActivationRestriction.OnlyIfCondition(
                        Conditions.YouCastSpellsThisTurn(atLeast = 1, filter = GameObjectFilter.Any)
                    )
                )
                effect = Effects.DrawCards(1)
            }
        }
        SynergyAxes.of(anySpell).rewards shouldNotContain SynergyAxis.SPELLS
    }
})
