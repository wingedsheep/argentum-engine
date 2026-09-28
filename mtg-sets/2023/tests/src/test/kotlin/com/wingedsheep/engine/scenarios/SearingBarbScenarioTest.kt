package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Searing Barb — 2 damage to any target, creature can't block, incubate 1. */
class SearingBarbScenarioTest : ScenarioTestBase() {
    private fun TestGame.incubators() = state.getBattlefield(player1Id)
        .count { state.getEntity(it)?.get<CardComponent>()?.name == "Incubator" }

    init {
        test("damages a player and incubates") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Searing Barb")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.castSpellTargetingPlayer(1, "Searing Barb", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
            game.incubators() shouldBe 1
        }

        test("damages a creature, which then can't block, and incubates") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Searing Barb")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!
            game.castSpell(1, "Searing Barb", giant).error shouldBe null
            game.resolveStack()
            game.incubators() shouldBe 1
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldNotBe null
        }
    }
}
