package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class ChaosWarpScenarioTest : ScenarioTestBase() {

    init {
        context("Chaos Warp") {
            test("opponent's permanent: shuffled into opponent's library, revealed permanent enters under opponent's control") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Chaos Warp")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(2, "Goblin Grappler") // target — owned by player 2
                    .withCardInLibrary(2, "Goblin Goon") // permanent in p2's library
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val targetId = game.findPermanent("Goblin Grappler")!!
                val p2LibBefore = game.librarySize(2)

                game.castSpell(1, "Chaos Warp", targetId)
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                // p2's library started with [Goon]. After shuffling Grappler in, library has
                // [Goon, Grappler] in random order. Whichever card is on top is revealed and
                // (since both are permanents) enters the battlefield under p2's control.
                val gooBfd = game.isOnBattlefield("Goblin Goon")
                val grapplerBfd = game.isOnBattlefield("Goblin Grappler")
                (gooBfd xor grapplerBfd) shouldBe true

                // Net library delta: +1 (Grappler shuffled in) - 1 (revealed permanent leaves)
                game.librarySize(2) shouldBe p2LibBefore
            }
        }
    }
}
