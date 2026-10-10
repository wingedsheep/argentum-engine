package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Field of the Dead (M20 #247) — Land. Enters tapped; {T}: Add {C}; whenever it or another land
 * you control enters, if you control seven or more lands with different names, create a 2/2 black
 * Zombie. The intervening "if" counts Field itself and the entering land.
 */
class FieldOfTheDeadScenarioTest : ScenarioTestBase() {
    init {
        fun main() = scenario().withPlayers("P1", "P2")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("Field entering as the seventh differently named land counts itself") {
            val game = main().withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(1, "Island")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Mountain")
                .withCardOnBattlefield(1, "Forest")
                .withCardOnBattlefield(1, "Terramorphic Expanse")
                .withCardInHand(1, "Field of the Dead").build()
            val field = game.findCardsInHand(1, "Field of the Dead").single()
            game.execute(PlayLand(game.player1Id, field)).error shouldBe null
            game.resolveStack()
            game.findPermanents("Zombie Token").size shouldBe 1
        }

        test("another land entering makes a Zombie once seven names are present") {
            val game = main().withCardOnBattlefield(1, "Field of the Dead")
                .withCardOnBattlefield(1, "Plains")
                .withCardOnBattlefield(1, "Island")
                .withCardOnBattlefield(1, "Swamp")
                .withCardOnBattlefield(1, "Mountain")
                .withCardOnBattlefield(1, "Forest")
                .withCardInHand(1, "Evolving Wilds").build()
            val wilds = game.findCardsInHand(1, "Evolving Wilds").single()
            game.execute(PlayLand(game.player1Id, wilds)).error shouldBe null
            game.resolveStack()
            game.findPermanents("Zombie Token").size shouldBe 1
        }

        test("duplicate land names do not count toward seven") {
            val game = main().withCardOnBattlefield(1, "Field of the Dead")
                .withLandsOnBattlefield(1, "Forest", 6)
                .withCardInHand(1, "Plains").build()
            val plains = game.findCardsInHand(1, "Plains").single()
            game.execute(PlayLand(game.player1Id, plains)).error shouldBe null
            game.resolveStack()
            game.findPermanents("Zombie Token").size shouldBe 0
        }
    }
}
