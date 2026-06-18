package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gwaihir the Windlord — "This spell costs {2} less to cast as long as you've drawn two or more
 * cards this turn." Exercises the new `PlayerDrewCardsThisTurn` condition gating a `ModifySpellCost`
 * reduction. Full cost {4}{W}{U} = 6 mana; reduced = {2}{W}{U} = 4 mana.
 */
class GwaihirTheWindlordScenarioTest : ScenarioTestBase() {

    init {
        test("costs {2} less with two cards drawn this turn") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Gwaihir the Windlord")
                .withLandsOnBattlefield(1, "Plains", 2) // W + 1 generic
                .withLandsOnBattlefield(1, "Island", 2) // U + 1 generic  → 4 mana total
                .withCardsDrawnThisTurn(1, 2)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Gwaihir the Windlord").error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Gwaihir the Windlord") shouldBe true
        }

        test("costs full price with fewer than two cards drawn") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Gwaihir the Windlord")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Island", 2) // only 4 mana — not enough for {4}{W}{U}
                .withCardsDrawnThisTurn(1, 1)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            // Without the reduction the spell needs 6 mana; only 4 are available.
            game.castSpell(1, "Gwaihir the Windlord").error shouldNotBe null
            game.isOnBattlefield("Gwaihir the Windlord") shouldBe false
        }
    }
}
