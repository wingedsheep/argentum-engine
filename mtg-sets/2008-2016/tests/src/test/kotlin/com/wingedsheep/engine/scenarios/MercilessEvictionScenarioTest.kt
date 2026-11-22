package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Merciless Eviction (GTC) — Choose one — Exile all artifacts / creatures / enchantments /
 * planeswalkers.
 */
class MercilessEvictionScenarioTest : ScenarioTestBase() {
    private fun setup() = scenario()
        .withPlayers("Player", "Opponent")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardOnBattlefield(2, "Ornithopter")
        .withCardInHand(1, "Merciless Eviction")
        .withActivePlayer(1)
        .withPriorityPlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("creature mode exiles every creature on both sides, leaving lands") {
            val game = setup()

            game.castSpellWithMode(1, "Merciless Eviction", modeIndex = 1).error shouldBe null
            game.resolveStack()

            withClue("all creatures, including the artifact creature, are exiled") {
                game.isInExile(1, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Hill Giant") shouldBe true
                game.isInExile(2, "Ornithopter") shouldBe true
                game.findPermanents("Plains").size shouldBe 3
            }
        }

        test("artifact mode exiles only artifacts") {
            val game = setup()

            game.castSpellWithMode(1, "Merciless Eviction", modeIndex = 0).error shouldBe null
            game.resolveStack()

            withClue("Ornithopter is exiled; non-artifact creatures stay") {
                game.isInExile(2, "Ornithopter") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
