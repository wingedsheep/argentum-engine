package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Hornet Nest (M15): "Whenever this creature is dealt damage, create that many 1/1 green Insect
 * creature tokens with flying and deathtouch." Any damage triggers it — combat or not — "that many"
 * is the damage dealt, not the damage it could survive, and lethal damage still triggers (its ruling).
 */
class HornetNestScenarioTest : ScenarioTestBase() {

    private fun TestGame.insects() = findAllPermanents("Insect Token")

    private fun TestGame.assertFlyingDeathtouch() = insects().forEach {
        state.projectedState.hasKeyword(it, Keyword.FLYING) shouldBe true
        state.projectedState.hasKeyword(it, Keyword.DEATHTOUCH) shouldBe true
    }

    init {
        test("non-lethal combat damage creates that many flying deathtouch Insects") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Hornet Nest")
                .withCardOnBattlefield(2, "Raging Goblin")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Raging Goblin" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hornet Nest" to listOf("Raging Goblin"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.isOnBattlefield("Hornet Nest") shouldBe true
            game.insects().size shouldBe 1
            game.assertFlyingDeathtouch()
        }

        test("lethal noncombat damage still triggers, for the full amount dealt") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Hornet Nest")
                .withCardInHand(2, "Lightning Bolt")
                .withLandsOnBattlefield(2, "Mountain", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpell(2, "Lightning Bolt", game.findPermanent("Hornet Nest")!!).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Hornet Nest") shouldBe false
            game.insects().size shouldBe 3
            game.assertFlyingDeathtouch()
            game.insects().forEach { game.state.projectedState.getController(it) shouldBe game.player1Id }
        }
    }
}
