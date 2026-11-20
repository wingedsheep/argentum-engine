package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Soldier of the Pantheon (THS #32) — {W} Creature — Human Soldier 2/1.
 *
 *   Protection from multicolored
 *   Whenever an opponent casts a multicolored spell, you gain 1 life.
 */
class SoldierOfThePantheonScenarioTest : ScenarioTestBase() {

    private fun opponentHolding(spell: String): TestGame =
        scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Soldier of the Pantheon")
            .withCardInHand(2, spell)
            .withLandsOnBattlefield(2, "Mountain", 1)
            .withLandsOnBattlefield(2, "Plains", 1)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(2)
            .withPriorityPlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

    init {
        test("an opponent casting a multicolored spell gains you 1 life") {
            val game = opponentHolding("Lightning Helix")

            game.castSpellTargetingPlayer(2, "Lightning Helix", 1).error shouldBe null
            game.resolveStack()

            // 20 - 3 from the Helix + 1 from the Soldier's trigger
            game.getLifeTotal(1) shouldBe 18
        }

        test("an opponent casting a monocolored spell gains nothing") {
            val game = opponentHolding("Shock")

            game.castSpellTargetingPlayer(2, "Shock", 1).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 18
        }

        test("protection from multicolored: a multicolored spell can't target it") {
            val game = opponentHolding("Lightning Helix")
            val soldier = game.findPermanent("Soldier of the Pantheon")!!

            game.castSpell(2, "Lightning Helix", soldier).error shouldNotBe null
            game.findPermanent("Soldier of the Pantheon") shouldBe soldier
        }
    }
}
