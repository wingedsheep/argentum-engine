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
 * Saiba Cryptomancer (MOM #76) — {1}{U} 0/1. Flash; backup 1; hexproof.
 *
 * Backup always puts the counter on its target; only *another* creature also gains hexproof until
 * end of turn. Flash is printed above backup, so it is never granted.
 */
class SaibaCryptomancerScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun castCryptomancer(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Saiba Cryptomancer")
            .withLandsOnBattlefield(1, "Island", 2)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Saiba Cryptomancer").error shouldBe null
        game.resolveStack()
        withClue("the backup trigger asks for its target") { game.hasPendingDecision() shouldBe true }
        return game
    }

    init {
        context("Saiba Cryptomancer") {

            test("backup on another creature: a +1/+1 counter and hexproof (not flash) until end of turn") {
                val game = castCryptomancer()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                plusOnes(game, bears) shouldBe 1
                val projected = game.state.projectedState
                projected.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
                projected.hasKeyword(bears, Keyword.FLASH) shouldBe false
                projected.getPower(bears) shouldBe 3

                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("the grant ends with the turn; the counter stays") {
                    game.state.projectedState.hasKeyword(bears, Keyword.HEXPROOF) shouldBe false
                    plusOnes(game, bears) shouldBe 1
                }
            }

            test("backup on itself: only the counter") {
                val game = castCryptomancer()
                val saiba = game.findPermanent("Saiba Cryptomancer")!!
                game.selectTargets(listOf(saiba)).error shouldBe null
                game.resolveStack()

                plusOnes(game, saiba) shouldBe 1
                game.state.projectedState.getPower(saiba) shouldBe 1
                game.state.projectedState.hasKeyword(saiba, Keyword.HEXPROOF) shouldBe true
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.HEXPROOF) shouldBe false
            }
        }
    }
}
