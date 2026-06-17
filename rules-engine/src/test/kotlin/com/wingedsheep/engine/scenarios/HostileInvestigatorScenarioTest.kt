package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Hostile Investigator (BIG #10, {3}{B}, Creature — Ogre Rogue Detective, 4/3).
 *
 *   When this creature enters, target opponent discards a card.
 *   Whenever one or more players discard one or more cards, investigate. This ability
 *   triggers only once each turn. (Create a Clue token...)
 *
 * Casting the creature exercises both abilities at once: the ETB discard fires a discard
 * event that the second ability sees, producing one Clue.
 */
class HostileInvestigatorScenarioTest : ScenarioTestBase() {

    init {
        context("Hostile Investigator") {

            test("ETB makes the opponent discard, which investigates once") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hostile Investigator")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInHand(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Hostile Investigator")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                // ETB targets the opponent; discard the opponent's only card.
                if (game.hasPendingDecision()) {
                    val bears = game.findCardsInHand(2, "Grizzly Bears")
                    if (bears.isNotEmpty()) game.selectCards(bears) else game.resolveStack()
                }
                game.resolveStack()

                withClue("Opponent discarded their only card") {
                    game.handSize(2) shouldBe 0
                }
                withClue("The discard triggers investigate → exactly one Clue token") {
                    clueTokens(game) shouldBe 1
                }
            }

            test("investigate only fires once per turn even on multiple discards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hostile Investigator", summoningSickness = false)
                    .withCardInHand(1, "Mind Rot")
                    .withCardsInHand(2, "Grizzly Bears", 3)
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Mind Rot: target player discards two cards (one discard event of >1 card).
                val cast = game.castSpellTargetingPlayer(1, "Mind Rot", 2)
                withClue("Mind Rot cast should succeed: ${cast.error}") { cast.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    val bears = game.findCardsInHand(2, "Grizzly Bears").take(2)
                    if (bears.isNotEmpty()) game.selectCards(bears)
                }
                game.resolveStack()

                withClue("Two cards discarded in one batch → still only one Clue (once each turn)") {
                    clueTokens(game) shouldBe 1
                }
            }
        }
    }

    private fun clueTokens(game: TestGame): Int =
        game.state.getBattlefield().count { id ->
            game.state.getEntity(id)?.get<CardComponent>()?.name == "Clue"
        }
}
