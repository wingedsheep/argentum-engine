package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseReplacementDecision
import com.wingedsheep.engine.core.ReplacementChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for New Blood (C17 #19).
 *
 * {2}{B}{B} Sorcery
 * As an additional cost to cast this spell, tap an untapped Vampire you control.
 * Gain control of target creature. Change the text of that creature by replacing all instances of
 * one creature type with Vampire.
 *
 * Exercises the fixed `replacementType` of ChangeCreatureTypeText: the player picks only the type
 * being replaced, and the stolen creature becomes a Vampire that the Vampire lord now pumps.
 */
class NewBloodScenarioTest : ScenarioTestBase() {

    init {
        context("tap a Vampire, steal a creature, and make it a Vampire") {

            test("steals the creature and replaces the chosen type with Vampire") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "New Blood")
                    .withCardOnBattlefield(1, "Stromkirk Captain")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("New Blood is castable with an untapped Vampire") {
                    game.getLegalActions(1).any { it.description.startsWith("Cast New Blood") } shouldBe true
                }

                val newBlood = game.findCardsInHand(1, "New Blood").first()
                val captain = game.findPermanent("Stromkirk Captain")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                val cast = game.execute(
                    CastSpell(
                        game.player1Id,
                        newBlood,
                        listOf(ChosenTarget.Permanent(bears)),
                        additionalCostPayment = AdditionalCostPayment(tappedPermanents = listOf(captain)),
                    )
                )
                withClue("New Blood casts by tapping Stromkirk Captain: ${cast.error}") { cast.error shouldBe null }
                withClue("the Vampire is tapped to pay the additional cost") {
                    game.state.getEntity(captain)?.has<TappedComponent>() shouldBe true
                }

                game.resolveStack()

                val decision = game.getPendingDecision() as ChooseReplacementDecision
                withClue("the replacement type is fixed to Vampire") {
                    decision.toOptions shouldBe listOf("Vampire")
                }
                withClue("Vampire isn't offered as the type to replace") {
                    decision.fromOptions shouldNotContain "Vampire"
                }
                withClue("the creature's own type sorts first") { decision.fromOptions.first() shouldBe "Bear" }

                game.submitDecision(
                    ReplacementChosenResponse(decision.id, decision.fromOptions.indexOf("Bear"), 0)
                ).error shouldBe null

                val projected = game.state.projectedState
                withClue("Player1 controls the stolen creature") {
                    projected.getController(bears) shouldBe game.player1Id
                }
                withClue("Bear became Vampire") {
                    projected.hasSubtype(bears, "Vampire") shouldBe true
                    projected.hasSubtype(bears, "Bear") shouldBe false
                }
                withClue("Stromkirk Captain now pumps it: 3/3 first strike") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 3
                }
            }

            test("can't be cast without an untapped Vampire") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "New Blood")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.getLegalActions(1).none { it.description.startsWith("Cast New Blood") } shouldBe true
            }
        }
    }
}
