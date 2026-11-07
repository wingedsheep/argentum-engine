package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Laboratory Brute (EMN #67) — {3}{U} Creature — Zombie Horror 3/3.
 *
 *   When this creature enters, mill four cards.
 *
 * Exactly four cards leave the top of your library for your graveyard; the fifth stays.
 */
class LaboratoryBruteScenarioTest : ScenarioTestBase() {

    init {
        test("entering mills exactly four cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Laboratory Brute")
                .withLandsOnBattlefield(1, "Island", 4)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Savannah Lions")
                .withCardInLibrary(1, "Elite Vanguard")
                .withCardInLibrary(1, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val libraryBefore = game.librarySize(1)
            val graveyardBefore = game.graveyardSize(1)

            game.castSpell(1, "Laboratory Brute").error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Laboratory Brute") shouldBe true
            game.librarySize(1) shouldBe libraryBefore - 4
            game.graveyardSize(1) shouldBe graveyardBefore + 4
        }
    }
}
