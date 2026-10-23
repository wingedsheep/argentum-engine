package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mid.cards.GeistflameReservoir
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Geistflame Reservoir (MID) — "Whenever you cast an instant or sorcery spell, put a charge counter
 * on this artifact. {1}{R}, {T}, Remove any number of charge counters from this artifact: It deals
 * that much damage to any target."
 *
 * The damage is a cost-linked amount: X is chosen on activation, capped by the Reservoir's own charge
 * counters, and the resolving ability must read that same X back as its damage.
 */
class GeistflameReservoirScenarioTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(GeistflameReservoir)
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.charge(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.CHARGE) ?: 0

    fun GameTestDriver.addCharge(id: EntityId, count: Int) {
        replaceState(state.updateEntity(id) { container ->
            val existing = container.get<CountersComponent>() ?: CountersComponent()
            container.with(existing.withAdded(CounterType.CHARGE, count))
        })
    }

    fun GameTestDriver.resolveAll() {
        repeat(12) {
            if (state.pendingDecision != null) autoResolveDecision()
            else if (stackSize > 0) bothPass()
            else return@repeat
        }
    }

    test("casting an instant puts a charge counter on it") {
        val d = driver()
        val reservoir = d.putPermanentOnBattlefield(d.player1, "Geistflame Reservoir")
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)

        d.castSpell(d.player1, bolt, listOf(d.player2)).outcome shouldBe Outcome.Done
        d.resolveAll()

        d.charge(reservoir) shouldBe 1
        d.getLifeTotal(d.player2) shouldBe 17
    }

    test("removing two of three charge counters deals 2 damage to the chosen target") {
        val d = driver()
        val reservoir = d.putPermanentOnBattlefield(d.player1, "Geistflame Reservoir")
        d.addCharge(reservoir, 3)
        d.giveMana(d.player1, Color.RED, 2)

        val ability = d.legalActions(d.player1)
            .first { it.actionType == "ActivateAbility" && it.maxAffordableX != null }
        withClue("X is capped by the charge counters on it") { ability.maxAffordableX shouldBe 3 }

        val activate = ability.action as ActivateAbility
        val result = d.submit(
            activate.copy(xValue = 2, targets = listOf(ChosenTarget.Player(d.player2)))
        )
        withClue("activation failed: ${result.error}") { result.outcome shouldBe Outcome.Done }
        d.resolveAll()

        d.charge(reservoir) shouldBe 1
        d.getLifeTotal(d.player2) shouldBe 18
    }
})
