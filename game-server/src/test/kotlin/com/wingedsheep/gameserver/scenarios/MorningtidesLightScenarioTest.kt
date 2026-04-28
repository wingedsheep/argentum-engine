package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Morningtide's Light.
 *
 * "Until your next turn, prevent all damage that would be dealt to you."
 */
class MorningtidesLightScenarioTest : ScenarioTestBase() {

    init {
        test("prevents damage to its controller until their next turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Morningtide's Light")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardsInHand(2, "Shock", 2)
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .withActivePlayer(1)
                .build()

            game.castSpell(1, "Morningtide's Light")
            game.resolveStack()
            game.getLifeTotal(1) shouldBe 20
            game.getClientState(1)
                .players
                .first { it.playerId == game.player1Id }
                .activeEffects
                .any { it.effectId == "prevent_all_damage" } shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            val firstShock = game.castSpellTargetingPlayer(2, "Shock", 1)
            firstShock.isSuccess shouldBe true
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 20

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

            game.passPriority()
            val secondShock = game.castSpellTargetingPlayer(2, "Shock", 1)
            secondShock.isSuccess shouldBe true
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 18
            game.getClientState(1)
                .players
                .first { it.playerId == game.player1Id }
                .activeEffects
                .any { it.effectId == "prevent_all_damage" } shouldBe false
        }
    }
}
