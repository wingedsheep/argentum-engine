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
 * Golden-Scale Aeronaut (MOM #15) — {4}{W} 2/3. Backup 1; flying.
 *
 * The backup trigger always puts the counter on its target; only *another* creature also gains the
 * ability printed below backup until end of turn.
 */
class GoldenScaleAeronautScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun castAeronaut(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Golden-Scale Aeronaut")
            .withLandsOnBattlefield(1, "Plains", 5)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Golden-Scale Aeronaut").error shouldBe null
        game.resolveStack()
        withClue("the backup trigger asks for its target") { game.hasPendingDecision() shouldBe true }
        return game
    }

    init {
        context("Golden-Scale Aeronaut") {

            test("backup on another creature: a +1/+1 counter and flying until end of turn") {
                val game = castAeronaut()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                plusOnes(game, bears) shouldBe 1
                val projected = game.state.projectedState
                projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
                projected.getPower(bears) shouldBe 3

                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("the grant ends with the turn; the counter stays") {
                    game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
                    plusOnes(game, bears) shouldBe 1
                }
            }

            test("backup on itself: only the counter") {
                val game = castAeronaut()
                val aeronaut = game.findPermanent("Golden-Scale Aeronaut")!!
                game.selectTargets(listOf(aeronaut)).error shouldBe null
                game.resolveStack()

                plusOnes(game, aeronaut) shouldBe 1
                game.state.projectedState.getPower(aeronaut) shouldBe 3
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
            }
        }
    }
}
