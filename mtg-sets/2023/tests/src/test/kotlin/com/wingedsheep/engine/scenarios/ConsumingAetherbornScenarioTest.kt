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
 * Consuming Aetherborn (MOM #97) — {3}{B} 2/2. Backup 1; lifelink.
 */
class ConsumingAetherbornScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun castAetherborn(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Consuming Aetherborn")
            .withLandsOnBattlefield(1, "Swamp", 4)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Consuming Aetherborn").error shouldBe null
        game.resolveStack()
        withClue("the backup trigger asks for its target") { game.hasPendingDecision() shouldBe true }
        return game
    }

    init {
        context("Consuming Aetherborn") {

            test("backup on another creature: counter and lifelink until end of turn") {
                val game = castAetherborn()
                val bears = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                plusOnes(game, bears) shouldBe 1
                game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe true
                game.state.projectedState.getPower(bears) shouldBe 3

                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe false
                plusOnes(game, bears) shouldBe 1
            }

            test("backup on itself: only the counter") {
                val game = castAetherborn()
                val aetherborn = game.findPermanent("Consuming Aetherborn")!!
                game.selectTargets(listOf(aetherborn)).error shouldBe null
                game.resolveStack()

                plusOnes(game, aetherborn) shouldBe 1
                game.state.projectedState.getPower(aetherborn) shouldBe 3
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.LIFELINK) shouldBe false
            }
        }
    }
}
