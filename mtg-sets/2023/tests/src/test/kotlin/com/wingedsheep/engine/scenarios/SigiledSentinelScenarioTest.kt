package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sigiled Sentinel (MOM #37) — {2}{W} 2/2. Backup 1; vigilance.
 *
 * The backup trigger always puts the counter on its target; only *another* creature also gains
 * vigilance until end of turn.
 */
class SigiledSentinelScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun castSentinel(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Sigiled Sentinel")
            .withLandsOnBattlefield(1, "Plains", 3)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Sigiled Sentinel").error shouldBe null
        game.resolveStack()
        withClue("the backup trigger asks for its target") { game.hasPendingDecision() shouldBe true }
        return game
    }

    init {
        context("Sigiled Sentinel") {

            test("backup on another creature: a +1/+1 counter and vigilance until end of turn") {
                val game = castSentinel()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                plusOnes(game, bears) shouldBe 1
                game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                game.state.projectedState.getPower(bears) shouldBe 3

                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("the grant ends with the turn; the counter stays") {
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
                    plusOnes(game, bears) shouldBe 1
                }
            }

            test("backup on itself: only the counter") {
                val game = castSentinel()
                val sentinel = game.findPermanent("Sigiled Sentinel")!!
                game.selectTargets(listOf(sentinel)).error shouldBe null
                game.resolveStack()

                plusOnes(game, sentinel) shouldBe 1
                game.state.projectedState.getPower(sentinel) shouldBe 3
                game.state.projectedState.hasKeyword(sentinel, Keyword.VIGILANCE) shouldBe true
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe false
            }
        }
    }
}
