package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Thassa's Devourer (JOU #53) — {4}{U} Enchantment Creature — Elemental 2/6.
 *
 *   Constellation — Whenever this creature or another enchantment you control enters, target player mills two cards.
 */
class ThassasDevourerScenarioTest : ScenarioTestBase() {
    init {
        test("another enchantment entering makes the target player mill two") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Thassa's Devourer")
                .withCardInHand(1, "Thassa's Ire")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInLibrary(2, "Forest")
                .withCardInLibrary(2, "Island")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Thassa's Ire").error shouldBe null
            game.resolveStack()

            withClue("constellation asks for a target player") {
                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            }
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            withClue("two of the opponent's three library cards were milled") {
                game.librarySize(2) shouldBe 1
                game.graveyardSize(2) shouldBe 2
            }
        }
    }
}
