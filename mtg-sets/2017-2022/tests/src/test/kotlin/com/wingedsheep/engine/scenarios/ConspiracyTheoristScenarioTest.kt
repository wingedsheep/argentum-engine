package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Conspiracy Theorist (STX #94) — {1}{R} Creature — Human Shaman 2/2.
 *
 *   Whenever this creature attacks, you may pay {1} and discard a card. If you do, draw a card.
 *   Whenever you discard one or more nonland cards, you may exile one of them from your
 *   graveyard. If you do, you may cast it this turn.
 *
 * Covers the two-part optional cost (pay + discard → draw), the empty-hand case (no offer, no
 * mana spent, no draw), the batch discard payoff exiling the discarded nonland card with a cast
 * permission, and a land discard not firing the nonland payoff.
 */
class ConspiracyTheoristScenarioTest : ScenarioTestBase() {

    private fun TestGame.nameOf(id: com.wingedsheep.sdk.model.EntityId): String? =
        state.getEntity(id)?.get<CardComponent>()?.name

    /** Answer every prompt until the stack is empty: [yes] to the offer, [pick] in every card choice. */
    private fun TestGame.drive(yes: Boolean, pick: String?) {
        var guard = 0
        while (guard++ < 40) {
            when (val decision = getPendingDecision()) {
                is YesNoDecision -> answerYesNo(yes)
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                is SelectCardsDecision -> {
                    val chosen = decision.options.filter { nameOf(it) == pick }.take(1)
                    if (chosen.isEmpty() && decision.minSelections == 0) skipSelection() else selectCards(chosen)
                }
                null -> if (state.stack.isNotEmpty()) resolveStack() else return
                else -> error("Unexpected decision: $decision")
            }
        }
    }

    private fun TestGame.mountainTapped(): Boolean =
        state.getEntity(findPermanent("Mountain")!!)?.has<TappedComponent>() == true

    init {
        context("Conspiracy Theorist") {

            test("paying {1} and discarding a spell draws a card, and the spell can be exiled to cast this turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Conspiracy Theorist", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Conspiracy Theorist" to 2))
                game.drive(yes = true, pick = "Lightning Bolt")

                withClue("the {1} was paid") { game.mountainTapped() shouldBe true }
                withClue("the discarded Bolt was replaced by a drawn card") {
                    game.isInHand(1, "Lightning Bolt") shouldBe false
                    game.isInHand(1, "Island") shouldBe true
                }
                withClue("the discard payoff exiled the Bolt from the graveyard") {
                    game.isInGraveyard(1, "Lightning Bolt") shouldBe false
                    game.isInExile(1, "Lightning Bolt") shouldBe true
                }
                withClue("the exiled Bolt may be cast this turn") {
                    val boltId = game.state.getExile(game.player1Id).first { game.nameOf(it) == "Lightning Bolt" }
                    game.state.mayPlayPermissions.any { boltId in it.cardIds } shouldBe true
                }
            }

            test("declining the exile leaves the discarded spell in the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Conspiracy Theorist", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Conspiracy Theorist" to 2))
                // Discard the Bolt for the cost, then pick nothing for the exile.
                var guard = 0
                var discarded = false
                while (guard++ < 40) {
                    when (val decision = game.getPendingDecision()) {
                        is YesNoDecision -> game.answerYesNo(true)
                        is SelectManaSourcesDecision -> game.submitManaSourcesAutoPay()
                        is SelectCardsDecision ->
                            if (!discarded && decision.minSelections > 0) {
                                game.selectCards(decision.options.filter { game.nameOf(it) == "Lightning Bolt" })
                                discarded = true
                            } else {
                                game.skipSelection()
                            }
                        null -> if (game.state.stack.isNotEmpty()) game.resolveStack() else break
                        else -> error("Unexpected decision: $decision")
                    }
                }

                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.isInExile(1, "Lightning Bolt") shouldBe false
            }

            test("with an empty hand the offer is skipped: no mana spent, no card drawn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Conspiracy Theorist", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Conspiracy Theorist" to 2))
                game.resolveStack()

                (game.getPendingDecision() is YesNoDecision) shouldBe false
                game.mountainTapped() shouldBe false
                game.handSize(1) shouldBe 0
            }

            test("discarding a land draws a card but offers no exile") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Conspiracy Theorist", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInHand(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Conspiracy Theorist" to 2))
                game.drive(yes = true, pick = "Forest")

                game.isInGraveyard(1, "Forest") shouldBe true
                game.isInExile(1, "Forest") shouldBe false
                game.isInHand(1, "Island") shouldBe true
            }
        }
    }
}
