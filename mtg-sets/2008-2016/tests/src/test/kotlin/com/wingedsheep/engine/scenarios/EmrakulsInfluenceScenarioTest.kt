package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Emrakul's Influence (EMN #157) — {2}{G}{G} Enchantment.
 *
 *   Whenever you cast an Eldrazi creature spell with mana value 7 or greater, draw two cards.
 *
 * Sire of Seven Deaths ({7} Eldrazi) sits exactly on the threshold; Kozilek's Channeler ({5}
 * Eldrazi) is under it.
 */
class EmrakulsInfluenceScenarioTest : ScenarioTestBase() {

    init {
        test("casting a mana value 7 Eldrazi creature spell draws two cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Emrakul's Influence")
                .withCardInHand(1, "Sire of Seven Deaths")
                .withLandsOnBattlefield(1, "Forest", 7)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)
            game.castSpell(1, "Sire of Seven Deaths").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Sire of Seven Deaths") shouldBe true
            // -1 for the cast Sire, +2 from the trigger.
            game.handSize(1) shouldBe handBefore + 1
        }

        test("an Eldrazi creature spell with mana value under 7 draws nothing") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Emrakul's Influence")
                .withCardInHand(1, "Kozilek's Channeler")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val handBefore = game.handSize(1)
            game.castSpell(1, "Kozilek's Channeler").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Kozilek's Channeler") shouldBe true
            game.handSize(1) shouldBe handBefore - 1
        }
    }
}
