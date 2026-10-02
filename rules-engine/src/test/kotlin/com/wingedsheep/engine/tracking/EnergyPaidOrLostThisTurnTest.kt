package com.wingedsheep.engine.tracking

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.player.PlayerCountersRemovedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Engine coverage for `TurnTracker.ENERGY_PAID_OR_LOST` — "if you've paid or lost N or more {E} this
 * turn" (Izzet Generatorium). Every energy counter removed from the player counts, whether paid as a
 * cost or removed by an effect; gaining energy never nets against it; the tally is per player and
 * resets at end of turn.
 */
class EnergyPaidOrLostThisTurnTest : FunSpec({

    val spender = card("Energy Spender Probe") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.PayPlayerCounters(CounterType.ENERGY, 2)
            effect = Effects.GainLife(1)
        }
    }

    val drainer = card("Energy Drainer Probe") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Free
            effect = Effects.RemoveCounters(CounterType.ENERGY, 1, EffectTarget.Controller)
        }
    }

    val gate = card("Spent Energy Gate Probe") {
        manaCost = "{0}"
        typeLine = "Artifact"
        activatedAbility {
            cost = Costs.Free
            restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.YouPaidOrLostEnergyThisTurn(4)))
            effect = Effects.DrawCards(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(spender, drainer, gate))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.giveEnergy(player: EntityId, amount: Int) =
        addComponent(player, CountersComponent().withAdded(CounterType.ENERGY, amount))

    fun GameTestDriver.energy(player: EntityId) =
        state.getEntity(player)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

    fun GameTestDriver.removed(player: EntityId) =
        state.getEntity(player)?.get<PlayerCountersRemovedThisTurnComponent>()?.count(CounterType.ENERGY) ?: 0

    fun GameTestDriver.activate(player: EntityId, permanent: EntityId, name: String) =
        submit(ActivateAbility(player, permanent, cardRegistry.requireCard(name).activatedAbilities[0].id))

    fun GameTestDriver.gateIsLegal(player: EntityId, gateId: EntityId) =
        legalActions(player).any { it.affordable && (it.action as? ActivateAbility)?.sourceId == gateId }

    test("energy paid as a cost and energy lost to an effect both count, and the gate opens at four") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.giveEnergy(me, 5)
        val spenderId = driver.putPermanentOnBattlefield(me, "Energy Spender Probe")
        val drainerId = driver.putPermanentOnBattlefield(me, "Energy Drainer Probe")
        val gateId = driver.putPermanentOnBattlefield(me, "Spent Energy Gate Probe")

        withClue("holding energy is not paying it") { driver.gateIsLegal(me, gateId) shouldBe false }

        driver.activate(me, spenderId, "Energy Spender Probe").error shouldBe null
        withClue("the cost is recorded as soon as it is paid, before the ability resolves") {
            driver.removed(me) shouldBe 2
        }
        driver.bothPass()

        driver.activate(me, drainerId, "Energy Drainer Probe").error shouldBe null
        driver.bothPass()
        withClue("three paid or lost — still short") {
            driver.removed(me) shouldBe 3
            driver.gateIsLegal(me, gateId) shouldBe false
        }

        driver.activate(me, drainerId, "Energy Drainer Probe").error shouldBe null
        driver.bothPass()
        driver.energy(me) shouldBe 1
        driver.removed(me) shouldBe 4
        driver.gateIsLegal(me, gateId) shouldBe true
    }

    test("gaining energy back does not net against what was paid") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        driver.giveEnergy(me, 4)
        val spenderId = driver.putPermanentOnBattlefield(me, "Energy Spender Probe")
        val gateId = driver.putPermanentOnBattlefield(me, "Spent Energy Gate Probe")

        repeat(2) {
            driver.activate(me, spenderId, "Energy Spender Probe").error shouldBe null
            driver.bothPass()
        }
        driver.giveEnergy(me, 6)

        driver.removed(me) shouldBe 4
        driver.gateIsLegal(me, gateId) shouldBe true
    }

    test("the tally belongs to the player who lost the energy and resets at end of turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.giveEnergy(me, 4)
        val spenderId = driver.putPermanentOnBattlefield(me, "Energy Spender Probe")
        val opponentGate = driver.putPermanentOnBattlefield(opponent, "Spent Energy Gate Probe")

        repeat(2) {
            driver.activate(me, spenderId, "Energy Spender Probe").error shouldBe null
            driver.bothPass()
        }
        driver.removed(me) shouldBe 4
        withClue("the opponent paid nothing") { driver.removed(opponent) shouldBe 0 }

        driver.passPriorityUntil(Step.UPKEEP)
        withClue("a new turn starts from zero") {
            driver.removed(me) shouldBe 0
            driver.gateIsLegal(opponent, opponentGate) shouldBe false
        }
    }
})
