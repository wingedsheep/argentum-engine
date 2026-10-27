package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Replicating Ring (KHM): the upkeep night-counter countdown — one counter per upkeep, and at
 * eight the counters all come off and eight snow Replicated Ring mana tokens are made.
 */
class ReplicatingRingScenarioTest : ScenarioTestBase() {

    private fun TestGame.nightCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.NIGHT) ?: 0

    private fun TestGame.withNightCounters(id: EntityId, count: Int) {
        state = state.updateEntity(id) { it.with(CountersComponent().withAdded(CounterType.NIGHT, count)) }
    }

    private fun game() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Replicating Ring")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(2)
        .inPhase(Phase.ENDING, Step.END)
        .build()

    private fun TestGame.toMyUpkeep() {
        passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        resolveStack()
    }

    init {
        context("Replicating Ring") {

            test("puts a night counter on itself at the beginning of your upkeep") {
                val game = game()
                val ring = game.findPermanent("Replicating Ring")!!

                game.toMyUpkeep()

                game.nightCounters(ring) shouldBe 1
                game.findPermanents("Replicated Ring").size shouldBe 0
            }

            test("at eight night counters removes them all and creates eight snow Replicated Rings") {
                val game = game()
                val ring = game.findPermanent("Replicating Ring")!!
                game.withNightCounters(ring, 7)

                game.toMyUpkeep()

                game.nightCounters(ring) shouldBe 0
                val tokens = game.findPermanents("Replicated Ring")
                tokens.size shouldBe 8
                val token = game.state.getEntity(tokens.first())!!.get<CardComponent>()!!
                token.typeLine.isSnow shouldBe true
                token.typeLine.isArtifact shouldBe true
                game.state.projectedState.getColors(tokens.first()).isEmpty() shouldBe true
            }

            test("seven counters after the upkeep make no tokens yet") {
                val game = game()
                val ring = game.findPermanent("Replicating Ring")!!
                game.withNightCounters(ring, 6)

                game.toMyUpkeep()

                game.nightCounters(ring) shouldBe 7
                game.findPermanents("Replicated Ring").size shouldBe 0
            }
        }
    }
}
