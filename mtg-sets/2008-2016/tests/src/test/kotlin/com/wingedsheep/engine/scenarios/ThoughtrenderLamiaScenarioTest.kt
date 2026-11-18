package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Thoughtrender Lamia (JOU #85) — {4}{B}{B} Enchantment Creature — Lamia 5/3.
 *
 *   Constellation — Whenever this creature or another enchantment you control enters, each opponent discards a card.
 */
class ThoughtrenderLamiaScenarioTest : ScenarioTestBase() {
    init {
        test("its own entry makes the opponent discard a card of their choice") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Thoughtrender Lamia")
                .withLandsOnBattlefield(1, "Swamp", 6)
                .withCardInHand(2, "Grizzly Bears")
                .withCardInHand(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Thoughtrender Lamia").error shouldBe null
            game.resolveStack()

            val discard = game.getPendingDecision() as SelectCardsDecision
            withClue("the opponent chooses the discard") {
                discard.playerId shouldBe game.player2Id
            }
            val giant = game.findCardsInHand(2, "Hill Giant").single()
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            withClue("the chosen card was discarded") {
                game.handSize(2) shouldBe 1
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }
    }
}
