package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Invasion of Segovia // Caetus, Sea Tyrant of Segovia.
 *
 * Front: two 1/1 blue Kraken tokens with trample. Back: noncreature spells have convoke, and an
 * end-step untap of up to four target creatures.
 */
class InvasionOfSegoviaScenarioTest : ScenarioTestBase() {

    init {
        context("front face — Invasion of Segovia") {
            test("enters and creates two Kraken tokens") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Invasion of Segovia")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Invasion of Segovia").error shouldBe null
                game.resolveStack()
                game.resolveStack()

                val krakens = game.findPermanents("Kraken Token")
                withClue("two Kraken tokens") { krakens.size shouldBe 2 }
            }
        }
    }
}
