package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Cinder Hellion — {4}{R} 4/4 Hellion. "Trample. When this creature enters, it deals 2 damage to
 * target opponent or planeswalker."
 */
class CinderHellionScenarioTest : ScenarioTestBase() {
    init {
        test("entering deals 2 damage to the target opponent") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Cinder Hellion")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Cinder Hellion").error shouldBe null

            var guard = 0
            while (guard++ < 30) {
                val pd = game.state.pendingDecision
                when {
                    pd is ChooseTargetsDecision ->
                        game.submitDecision(TargetsResponse(pd.id, mapOf(0 to listOf(game.player2Id)))).error shouldBe null
                    pd != null -> error("unexpected decision $pd")
                    game.state.stack.isNotEmpty() -> game.resolveStack()
                    else -> break
                }
            }

            val hellion = game.findPermanent("Cinder Hellion")!!
            game.state.projectedState.hasKeyword(hellion, Keyword.TRAMPLE) shouldBe true
            withClue("the opponent took 2 from the enters trigger; the caster took none") {
                game.getLifeTotal(2) shouldBe 18
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
