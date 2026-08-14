package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Mask of Memory (MRD) — "Whenever equipped creature deals combat damage to a player, you may draw
 * two cards. If you do, discard a card." Equip {1}.
 *
 * The "if you do" is not a second decision: the discard is the price of the draw, so both sit inside
 * one may. These tests pin that accepting costs exactly one card from hand and that declining costs
 * nothing — the failure mode being a discard that fires even when the draw was declined.
 */
class MaskOfMemoryScenarioTest : ScenarioTestBase() {

    init {
        context("Mask of Memory — draw two, discard one, on combat damage") {
            test("accepting draws two and then discards one") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Mask of Memory", "Grizzly Bears")
                    .withCardsInHand(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libBefore = game.state.getLibrary(game.player1Id).size
                val handBefore = game.state.getHand(game.player1Id).size
                val gyBefore = game.state.getGraveyard(game.player1Id).size

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.passPriority()
                game.resolveStack()

                game.answerYesNo(true).error shouldBe null

                val discard = game.getPendingDecision()
                withClue("the accepted branch always reaches the discard") {
                    (discard is SelectCardsDecision) shouldBe true
                }
                game.selectCards(listOf((discard as SelectCardsDecision).options.first()))
                    .error shouldBe null
                game.resolveStack()

                withClue("two drawn off the top") {
                    game.state.getLibrary(game.player1Id).size shouldBe libBefore - 2
                }
                withClue("net +1 in hand: drew two, discarded one") {
                    game.state.getHand(game.player1Id).size shouldBe handBefore + 1
                }
                withClue("the discarded card is in the graveyard") {
                    game.state.getGraveyard(game.player1Id).size shouldBe gyBefore + 1
                }
            }

            test("declining draws nothing and discards nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Mask of Memory", "Grizzly Bears")
                    .withCardsInHand(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libBefore = game.state.getLibrary(game.player1Id).size
                val handBefore = game.state.getHand(game.player1Id).size
                val gyBefore = game.state.getGraveyard(game.player1Id).size

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.passPriority()
                game.resolveStack()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("declining the draw also skips the 'if you do' discard") {
                    game.state.getLibrary(game.player1Id).size shouldBe libBefore
                    game.state.getHand(game.player1Id).size shouldBe handBefore
                    game.state.getGraveyard(game.player1Id).size shouldBe gyBefore
                }
            }
        }
    }
}
