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
 * Somberwald Spider (ISD #202) — {4}{G} 2/4 Spider.
 *
 *   Reach
 *   Morbid — This creature enters with two +1/+1 counters on it if a creature died this turn.
 */
class SomberwaldSpiderScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun buildGame() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Somberwald Spider")
        .withCardInHand(1, "Lightning Bolt")
        .withLandsOnBattlefield(1, "Forest", 5)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("enters with two +1/+1 counters when a creature died this turn") {
            val game = buildGame()
            game.castSpell(1, "Lightning Bolt", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()

            val cast = game.castSpell(1, "Somberwald Spider")
            withClue("cast should succeed: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            val spider = game.findPermanent("Somberwald Spider")!!
            game.plusOnes(spider) shouldBe 2
            game.state.projectedState.getPower(spider) shouldBe 4
            game.state.projectedState.getToughness(spider) shouldBe 6
            game.state.projectedState.hasKeyword(spider, Keyword.REACH) shouldBe true
        }

        test("enters without counters when no creature died this turn") {
            val game = buildGame()
            game.castSpell(1, "Somberwald Spider").error shouldBe null
            game.resolveStack()

            val spider = game.findPermanent("Somberwald Spider")!!
            game.plusOnes(spider) shouldBe 0
            game.state.projectedState.getPower(spider) shouldBe 2
            game.state.projectedState.getToughness(spider) shouldBe 4
        }
    }
}
