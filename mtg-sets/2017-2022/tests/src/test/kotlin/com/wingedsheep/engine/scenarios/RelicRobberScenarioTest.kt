package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Relic Robber — combat damage to a player makes *that player* create the Goblin Construct, so the
 * token's "At the beginning of your upkeep, this token deals 1 damage to you" fires on the damaged
 * player's upkeep and hits them, not Relic Robber's controller.
 */
class RelicRobberScenarioTest : ScenarioTestBase() {

    init {
        test("the damaged player gets the Construct and it pings them on their upkeep") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Relic Robber")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Relic Robber" to 2)).error shouldBe null
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 18
            val construct = game.findPermanents("Goblin Construct Token").single()
            game.state.projectedState.getController(construct) shouldBe game.player2Id
            game.state.projectedState.isCreature(construct) shouldBe true

            // Opponent's upkeep: the token's trigger damages its controller.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.resolveStack()

            game.state.activePlayerId shouldBe game.player2Id
            game.getLifeTotal(2) shouldBe 17
            game.getLifeTotal(1) shouldBe 20
        }
    }
}
