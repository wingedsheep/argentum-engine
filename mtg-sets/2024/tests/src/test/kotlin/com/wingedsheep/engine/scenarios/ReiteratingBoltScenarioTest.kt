package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Reiterating Bolt (MH3) — "Replicate—Pay {E}{E}{E}. Reiterating Bolt deals 3 damage to target
 * creature or planeswalker."
 */
class ReiteratingBoltScenarioTest : ScenarioTestBase() {

    private fun TestGame.energy(): Int =
        state.getEntity(player1Id)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    private fun TestGame.giveEnergy(amount: Int) {
        state = state.updateEntity(player1Id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.ENERGY, amount))
        }
    }

    private fun TestGame.castBolt(target: EntityId, times: Int?) = execute(
        CastSpell(
            player1Id,
            state.getHand(player1Id).first { state.getEntity(it)?.get<CardComponent>()?.name == "Reiterating Bolt" },
            targets = listOf(ChosenTarget.Permanent(target)),
            declaredCostSlot = times?.let { ChoiceSlot.REPLICATED },
            declaredCostTimes = times ?: 1,
        )
    )

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Reiterating Bolt")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardOnBattlefield(2, "Craw Wurm") // 6/4
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Reiterating Bolt") {
            test("without replicate, deals 3 damage once") {
                val game = board()
                game.castBolt(game.findPermanent("Grizzly Bears")!!, times = null).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("replicated twice for six energy: the original and two copies each deal 3") {
                val game = board()
                game.giveEnergy(7)
                val wurm = game.findPermanent("Craw Wurm")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.castBolt(wurm, times = 2).error shouldBe null
                game.energy() shouldBe 1

                // First copy stays on the Wurm (3 + 3 = 6 ≥ toughness 4), second goes to the Bears.
                val picks = mutableListOf(wurm, bears)
                var guard = 0
                while ((game.state.stack.isNotEmpty() || game.getPendingDecision() != null) && guard++ < 20) {
                    if (game.getPendingDecision() is ChooseTargetsDecision) {
                        game.selectTargets(listOf(picks.removeAt(0))).error shouldBe null
                    } else {
                        game.resolveStack()
                    }
                }
                picks.isEmpty() shouldBe true
                game.isInGraveyard(2, "Craw Wurm") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("can't replicate more times than the energy pays for") {
                val game = board()
                game.giveEnergy(5)
                game.castBolt(game.findPermanent("Craw Wurm")!!, times = 2).error shouldNotBe null
                game.energy() shouldBe 5
            }
        }
    }
}
