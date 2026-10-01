package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.HydraTrainer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hydra Trainer (MH3) — "You may exert this creature as it attacks. When you do, target creature
 * gets +X/+X until end of turn, where X is the number of counters on permanents you control."
 * and "{2}{G}: Adapt 2."
 */
class HydraTrainerScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + HydraTrainer)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.addCounters(entity: EntityId, type: CounterType, n: Int) = replaceState(
        state.updateEntity(entity) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(type, n))
        }
    )

    test("exerting pumps the target by every counter of any kind on permanents you control") {
        val d = driver()
        val active = d.activePlayer!!
        val opponent = d.getOpponent(active)
        val trainer = d.putCreatureOnBattlefield(active, "Hydra Trainer")
        val bears = d.putCreatureOnBattlefield(active, "Grizzly Bears")
        val forest = d.putLandOnBattlefield(active, "Forest")
        val theirBears = d.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        d.addCounters(trainer, CounterType.PLUS_ONE_PLUS_ONE, 2)
        d.addCounters(forest, CounterType.CHARGE, 1)
        d.addCounters(theirBears, CounterType.PLUS_ONE_PLUS_ONE, 4) // an opponent's counters don't count

        d.removeSummoningSickness(trainer)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(trainer), opponent)
        d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitCardSelection(active, listOf(trainer))
        d.state.getEntity(trainer)?.has<ExertedComponent>() shouldBe true

        d.submitTargetSelection(active, listOf(bears))
        d.bothPass()
        // X = 2 (+1/+1 on Trainer) + 1 (charge on Forest) = 3
        d.state.projectedState.getPower(bears) shouldBe 5
        d.state.projectedState.getToughness(bears) shouldBe 5
    }

    test("not exerting means no trigger") {
        val d = driver()
        val active = d.activePlayer!!
        val trainer = d.putCreatureOnBattlefield(active, "Hydra Trainer")
        d.removeSummoningSickness(trainer)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(trainer), d.getOpponent(active))
        d.submitCardSelection(active, emptyList())
        d.state.stack.isEmpty() shouldBe true
        d.state.getEntity(trainer)?.has<ExertedComponent>() shouldBe false
    }

    test("adapt 2 puts two +1/+1 counters only when it has none") {
        val d = driver()
        val active = d.activePlayer!!
        val trainer = d.putCreatureOnBattlefield(active, "Hydra Trainer")
        val adapt = HydraTrainer.activatedAbilities[0].id

        repeat(2) {
            d.giveMana(active, Color.GREEN, 3)
            d.submitSuccess(ActivateAbility(playerId = active, sourceId = trainer, abilityId = adapt))
            d.bothPass()
        }
        d.state.getEntity(trainer)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 2
        d.state.projectedState.getPower(trainer) shouldBe 3
    }
})
