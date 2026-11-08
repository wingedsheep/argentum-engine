package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Scythecat Cub (J25 #24):
 *   "Trample. Landfall — Whenever a land you control enters, put a +1/+1 counter on target creature
 *    you control. If this is the second time this ability has resolved this turn, double the number
 *    of +1/+1 counters on that creature instead."
 */
class ScythecatCubScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(entity: EntityId): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.counters?.get(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.playForestTargeting(forest: EntityId, target: EntityId) {
        execute(PlayLand(player1Id, forest)).error.shouldBeNull()
        val decision = state.pendingDecision as ChooseTargetsDecision
        withClue("only creatures you control are legal targets") {
            decision.legalTargets[0]!! shouldContainExactlyInAnyOrder listOf(
                findPermanent("Scythecat Cub")!!,
                findPermanent("Grizzly Bears")!!
            )
        }
        selectTargets(listOf(target)).error.shouldBeNull()
        resolveStack()
    }

    private fun boardWithThreeLandDrops() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Scythecat Cub")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(1, "Exploration")
        .withCardOnBattlefield(1, "Exploration")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInHand(1, "Forest")
        .withCardInHand(1, "Forest")
        .withCardInHand(1, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Scythecat Cub") {

            test("first resolution adds a counter, the second doubles, the third adds one again") {
                val game = boardWithThreeLandDrops()
                val cub = game.findPermanent("Scythecat Cub")!!
                val forests = game.findCardsInHand(1, "Forest")

                game.playForestTargeting(forests[0], cub)
                withClue("first resolution puts one +1/+1 counter") { game.plusOneCounters(cub) shouldBe 1 }

                game.playForestTargeting(forests[1], cub)
                withClue("second resolution doubles instead of adding") { game.plusOneCounters(cub) shouldBe 2 }

                game.playForestTargeting(forests[2], cub)
                withClue("third resolution is back to adding one") { game.plusOneCounters(cub) shouldBe 3 }
            }

            test("the second resolution on a creature with no counters doubles zero and adds nothing") {
                val game = boardWithThreeLandDrops()
                val cub = game.findPermanent("Scythecat Cub")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val forests = game.findCardsInHand(1, "Forest")

                game.playForestTargeting(forests[0], cub)
                game.playForestTargeting(forests[1], bears)

                withClue("doubling replaces the +1/+1 counter, so the Bears get none") {
                    game.plusOneCounters(bears) shouldBe 0
                }
                withClue("the Cub keeps its first counter") { game.plusOneCounters(cub) shouldBe 1 }
            }
        }
    }
}
