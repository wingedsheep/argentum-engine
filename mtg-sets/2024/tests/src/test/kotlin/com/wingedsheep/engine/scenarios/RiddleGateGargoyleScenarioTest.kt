package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Riddle Gate Gargoyle (MH3): ETB energy, and the reflexive "may pay {E}{E}. When you do, target
 * creature you control gains lifelink until end of turn" attack trigger.
 */
class RiddleGateGargoyleScenarioTest : FunSpec({

    val projector = StateProjector()

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        return d
    }

    fun energyOf(d: GameTestDriver, playerId: EntityId): Int =
        d.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun seedEnergy(d: GameTestDriver, playerId: EntityId, amount: Int) {
        d.replaceState(
            d.state.updateEntity(playerId) { container ->
                val current = container.get<CountersComponent>() ?: CountersComponent()
                container.with(current.withAdded(CounterType.ENERGY, amount))
            }
        )
    }

    fun drainToYesNo(d: GameTestDriver, maxSteps: Int = 10): Boolean {
        repeat(maxSteps) {
            if (d.pendingDecision is YesNoDecision) return true
            if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
        }
        return d.pendingDecision is YesNoDecision
    }

    test("entering gives three energy") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.giveMana(active, Color.WHITE, 1)
        d.giveMana(active, Color.BLUE, 1)
        val gargoyle = d.putCardInHand(active, "Riddle Gate Gargoyle")
        d.castSpell(active, gargoyle)
        d.bothPass() // resolve the creature spell
        d.bothPass() // resolve the ETB trigger

        energyOf(d, active) shouldBe 3
        projector.project(d.state).hasKeyword(gargoyle, Keyword.FLYING) shouldBe true
    }

    test("paying {E}{E} on attack grants lifelink to the chosen creature until end of turn") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Riddle Gate Gargoyle")
        val wurm = d.putCreatureOnBattlefield(active, "Craw Wurm")
        d.removeSummoningSickness(wurm)
        seedEnergy(d, active, 3)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(wurm), opp)

        drainToYesNo(d) shouldBe true
        d.submitYesNo(active, true)
        d.submitTargetSelection(active, listOf(wurm))
        d.stackSize shouldBe 1
        d.bothPass()

        projector.project(d.state).hasKeyword(wurm, Keyword.LIFELINK) shouldBe true
        energyOf(d, active) shouldBe 1

        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.UPKEEP)
        projector.project(d.state).hasKeyword(wurm, Keyword.LIFELINK) shouldBe false
    }

    test("declining the payment grants nothing and keeps the energy") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Riddle Gate Gargoyle")
        val wurm = d.putCreatureOnBattlefield(active, "Craw Wurm")
        d.removeSummoningSickness(wurm)
        seedEnergy(d, active, 2)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(wurm), opp)

        drainToYesNo(d) shouldBe true
        d.submitYesNo(active, false)
        repeat(4) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        projector.project(d.state).hasKeyword(wurm, Keyword.LIFELINK) shouldBe false
        energyOf(d, active) shouldBe 2
    }

    test("with fewer than two energy the prompt never appears") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(active, "Riddle Gate Gargoyle")
        val wurm = d.putCreatureOnBattlefield(active, "Craw Wurm")
        d.removeSummoningSickness(wurm)
        seedEnergy(d, active, 1)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(wurm), opp)

        var sawPrompt = false
        repeat(6) {
            if (d.pendingDecision is YesNoDecision) sawPrompt = true
            if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass()
        }
        sawPrompt shouldBe false
        energyOf(d, active) shouldBe 1
    }
})
