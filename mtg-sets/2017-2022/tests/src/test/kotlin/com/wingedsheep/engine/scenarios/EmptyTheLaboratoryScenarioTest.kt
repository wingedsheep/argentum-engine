package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Empty the Laboratory (MIC #14) — "Sacrifice X Zombies, then reveal cards from the top of your
 * library until you reveal a number of Zombie creature cards equal to the number of Zombies
 * sacrificed this way. Put those cards onto the battlefield and the rest on the bottom of your
 * library in a random order."
 *
 * Pins that the reveal count is the number of Zombies actually sacrificed, not X: with X = 3 and
 * only two Zombies to give, exactly two Zombie creature cards come off the library.
 */
class EmptyTheLaboratoryScenarioTest : ScenarioTestBase() {

    init {
        test("reveals as many Zombie creature cards as Zombies were actually sacrificed") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Empty the Laboratory")
                .withCardOnBattlefield(1, "Walking Corpse")
                .withCardOnBattlefield(1, "Walking Corpse")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Scathe Zombies")
                .withCardInLibrary(1, "Scathe Zombies")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Scathe Zombies")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Empty the Laboratory", 3).error shouldBe null
            game.resolveStack()

            // Both Zombies were sacrificed; the non-Zombie creature was not.
            game.findAllPermanents("Walking Corpse").size shouldBe 0
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.graveyardSize(1) shouldBe 3 // two Walking Corpses + the sorcery

            // Two sacrificed → exactly two Zombie creature cards put onto the battlefield.
            game.findAllPermanents("Scathe Zombies").size shouldBe 2
            game.librarySize(1) shouldBe 3
        }

        test("an Aura on the Zombie is not offered for sacrifice") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Empty the Laboratory")
                .withCardOnBattlefield(1, "Walking Corpse")
                .withCardAttachedTo(2, "Pacifism", "Walking Corpse")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Scathe Zombies")
                .withCardInLibrary(1, "Scathe Zombies")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Empty the Laboratory", 1).error shouldBe null
            game.resolveStack()

            // The only choice is the Zombie itself; one sacrificed → one Zombie card revealed.
            game.state.pendingDecision shouldBe null
            game.isOnBattlefield("Walking Corpse") shouldBe false
            game.findAllPermanents("Scathe Zombies").size shouldBe 1
            game.librarySize(1) shouldBe 1
        }

        test("with no Zombies to sacrifice, nothing is revealed") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Empty the Laboratory")
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Scathe Zombies")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(2, "Island")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castXSpell(1, "Empty the Laboratory", 1).error shouldBe null
            game.resolveStack()

            game.findAllPermanents("Scathe Zombies").size shouldBe 0
            game.librarySize(1) shouldBe 2
        }
    }
}
