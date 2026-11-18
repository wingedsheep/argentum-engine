package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Wall of Essence (STH): "Whenever this creature is dealt combat damage, you gain that much life."
 * Combat damage only — a burn spell doesn't trigger it — and lethal damage still does (its ruling).
 */
class WallOfEssenceScenarioTest : ScenarioTestBase() {

    private fun blockWithWall(attacker: String): TestGame {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Wall of Essence")
            .withCardOnBattlefield(2, attacker)
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf(attacker to 1)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(mapOf("Wall of Essence" to listOf(attacker))).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
        return game
    }

    init {
        test("being dealt combat damage gains that much life") {
            val game = blockWithWall("Hill Giant")

            game.isOnBattlefield("Wall of Essence") shouldBe true
            game.getLifeTotal(1) shouldBe 23
        }

        test("lethal combat damage still triggers, for the full amount") {
            val game = blockWithWall("Craw Wurm")

            game.isOnBattlefield("Wall of Essence") shouldBe false
            game.getLifeTotal(1) shouldBe 26
        }

        test("noncombat damage doesn't trigger it") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Wall of Essence")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(2, "Lightning Bolt", game.findPermanent("Wall of Essence")!!).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Wall of Essence") shouldBe true
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
