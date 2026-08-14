package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Ebon Dragon (POR #91) — {5}{B}{B} Creature — Dragon, 5/4.
 *
 * "Flying
 *  When this creature enters, you may have target opponent discard a card."
 *
 * The card is modelled as `optional = true` on the triggered ability plus a `TargetOpponent`
 * requirement, which makes it the regression case for the fail-open bug in
 * `TriggerProcessor.processTargetedTrigger`: with a single legal opponent the processor used to
 * auto-select that opponent and put the trigger straight on the stack, skipping the
 * target-selection decision whose `minTargets = 0` is what carries the "you may" decline. The
 * opponent discarded whether or not the controller wanted them to.
 *
 * Both branches are covered — accepting discards, declining does not.
 */
class EbonDragonScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Ally Bear",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype("Bear")),
                power = 2,
                toughness = 2
            )
        )

        context("Ebon Dragon's optional ETB discard") {

            test("accepting the may makes the sole opponent discard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ebon Dragon")
                    .withCardInHand(2, "Ally Bear")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ebon Dragon").error shouldBe null
                game.resolveStack()

                val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the may/target; got ${game.state.pendingDecision}")

                withClue("the optional trigger must offer the decline (minTargets = 0)") {
                    targetDecision.targetRequirements.single().minTargets shouldBe 0
                }

                game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to listOf(game.player2Id))))
                game.resolveStack()

                withClue("Player 2 should have discarded their only card") {
                    game.handSize(2) shouldBe 0
                    game.isInGraveyard(2, "Ally Bear") shouldBe true
                }
            }

            test("declining the may leaves the opponent's hand alone") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Ebon Dragon")
                    .withCardInHand(2, "Ally Bear")
                    .withLandsOnBattlefield(1, "Swamp", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Ebon Dragon").error shouldBe null
                game.resolveStack()

                val targetDecision = game.state.pendingDecision as? ChooseTargetsDecision
                    ?: error("expected a ChooseTargetsDecision for the may/target; got ${game.state.pendingDecision}")

                game.submitDecision(TargetsResponse(targetDecision.id, mapOf(0 to emptyList())))
                game.resolveStack()

                withClue("declining must not make Player 2 discard") {
                    game.handSize(2) shouldBe 1
                    game.isInGraveyard(2, "Ally Bear") shouldBe false
                }

                withClue("Ebon Dragon should still be on the battlefield") {
                    game.isOnBattlefield("Ebon Dragon") shouldBe true
                }
            }
        }
    }
}
