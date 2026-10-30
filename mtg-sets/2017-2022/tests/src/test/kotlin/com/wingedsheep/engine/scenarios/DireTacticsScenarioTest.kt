package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Dire Tactics (IKO #183) — "Exile target creature. If you don't control a Human, you lose life
 * equal to that creature's toughness."
 *
 * The life loss reads the exiled creature's toughness as it last existed on the battlefield, so an
 * Aura's bonus counts (Grizzly Bears + Holy Strength is 3/4: lose 4, not the printed 2).
 */
class DireTacticsScenarioTest : ScenarioTestBase() {

    private fun castWith(humanOnBoard: Boolean): ScenarioTestBase.TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Dire Tactics")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardAttachedTo(2, "Holy Strength", "Grizzly Bears")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (humanOnBoard) builder.withCardOnBattlefield(1, "Elite Vanguard")
        val game = builder.build()

        val bears = game.findPermanent("Grizzly Bears")!!
        game.castSpell(1, "Dire Tactics", bears).error shouldBe null
        game.resolveStack()
        game.isInExile(2, "Grizzly Bears") shouldBe true
        return game
    }

    init {
        context("Dire Tactics") {
            test("without a Human you lose life equal to the exiled creature's last-known toughness") {
                castWith(humanOnBoard = false).getLifeTotal(1) shouldBe 16
            }

            test("controlling a Human means no life is lost") {
                castWith(humanOnBoard = true).getLifeTotal(1) shouldBe 20
            }
        }
    }
}
