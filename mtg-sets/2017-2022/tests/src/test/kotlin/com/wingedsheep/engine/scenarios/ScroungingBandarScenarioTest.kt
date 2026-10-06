package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scrounging Bandar (AER #124) — "At the beginning of your upkeep, you may move any number of
 * +1/+1 counters from this creature onto another target creature."
 *
 * The target is locked in when the trigger goes on the stack; the number is chosen on resolution
 * (a pause that must carry the target through), capped at the Bandar's +1/+1 counters, and only
 * +1/+1 counters move.
 */
class ScroungingBandarScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.stamp(id: EntityId, counters: Map<CounterType, Int>) {
        state = state.updateEntity(id) { c -> c.with(CountersComponent(counters)) }
    }

    init {
        context("Scrounging Bandar") {

            test("enters with two +1/+1 counters") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Scrounging Bandar")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Scrounging Bandar").error shouldBe null
                game.resolveStack()

                val bandar = game.findPermanent("Scrounging Bandar")!!
                game.plusOnes(bandar) shouldBe 2
            }

            fun upkeepBoard(): Triple<TestGame, EntityId, EntityId> {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Scrounging Bandar")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()
                val bandar = game.findPermanent("Scrounging Bandar")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                game.stamp(bandar, mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2, CounterType.CHARGE to 1))
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                return Triple(game, bandar, bears)
            }

            test("moves the chosen number of +1/+1 counters onto the target, and only those") {
                val (game, bandar, bears) = upkeepBoard()

                game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseNumberDecision>()
                withClue("capped at the +1/+1 counters on the Bandar, not its other counters") {
                    decision.maxValue shouldBe 2
                }
                game.chooseNumber(1).error shouldBe null
                if (game.state.stack.isNotEmpty()) game.resolveStack()

                game.plusOnes(bandar) shouldBe 1
                game.plusOnes(bears) shouldBe 1
                game.state.getEntity(bandar)!!.get<CountersComponent>()!!.getCount(CounterType.CHARGE) shouldBe 1
            }

            test("choosing zero declines the move") {
                val (game, bandar, bears) = upkeepBoard()

                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                game.chooseNumber(0).error shouldBe null
                if (game.state.stack.isNotEmpty()) game.resolveStack()

                game.plusOnes(bandar) shouldBe 2
                game.plusOnes(bears) shouldBe 0
            }
        }
    }
}
