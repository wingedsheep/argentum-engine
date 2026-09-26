package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Jugan, the Rising Star (CHK #217) — "Flying. When Jugan dies, you may distribute five +1/+1
 * counters among any number of target creatures."
 */
class JuganTheRisingStarScenarioTest : ScenarioTestBase() {

    init {
        context("Jugan, the Rising Star") {

            fun board() = scenario()
                .withPlayers("Alice", "Bob")
                .withCardOnBattlefield(1, "Jugan, the Rising Star")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Terror")
                .withLandsOnBattlefield(1, "Swamp", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            fun TestGame.plusOnes(id: EntityId): Int =
                state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

            test("has flying") {
                val game = board()
                val jugan = game.findPermanent("Jugan, the Rising Star")!!
                game.state.projectedState.hasKeyword(jugan, Keyword.FLYING) shouldBe true
            }

            test("dying lets its controller split five counters unevenly among any creatures") {
                val game = board()
                val jugan = game.findPermanent("Jugan, the Rising Star")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Terror", jugan).error shouldBe null
                game.resolveStack()
                game.selectTargets(listOf(bears, giant)).error shouldBe null

                val decision = game.getPendingDecision().shouldBeInstanceOf<DistributeDecision>()
                withClue("five counters, at least one per target") {
                    decision.totalAmount shouldBe 5
                    decision.minPerTarget shouldBe 1
                }
                game.submitDecision(DistributionResponse(decision.id, mapOf(bears to 4, giant to 1))).error shouldBe null
                game.resolveStack()

                game.plusOnes(bears) shouldBe 4
                game.plusOnes(giant) shouldBe 1
                game.state.projectedState.getPower(bears) shouldBe 6
            }

            test("choosing no targets declines the distribution") {
                val game = board()
                val jugan = game.findPermanent("Jugan, the Rising Star")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Terror", jugan).error shouldBe null
                game.resolveStack()
                game.selectTargets(emptyList()).error shouldBe null
                game.resolveStack()

                game.plusOnes(bears) shouldBe 0
            }
        }
    }
}
