package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Bat Whisperer (MID #86) — {3}{B} 4/2 whose ETB reads "if an opponent lost life this turn,
 * create a 1/1 black Bat creature token with flying." Proves the intervening "if" gates the token.
 */
class BatWhispererScenarioTest : ScenarioTestBase() {

    init {
        context("Bat Whisperer") {

            test("creates a flying Bat when an opponent already lost life this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Bat Whisperer")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2).error shouldBe null
                game.resolveStack()

                game.castSpell(1, "Bat Whisperer").error shouldBe null
                game.resolveStack()

                val bats = game.findPermanents("Bat Token")
                bats.size shouldBe 1
                game.state.projectedState.hasKeyword(bats.single(), Keyword.FLYING) shouldBe true
            }

            test("creates no Bat when no opponent lost life this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Bat Whisperer")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Bat Whisperer").error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Bat Whisperer") shouldBe true
                game.findPermanents("Bat Token").size shouldBe 0
            }
        }
    }
}
