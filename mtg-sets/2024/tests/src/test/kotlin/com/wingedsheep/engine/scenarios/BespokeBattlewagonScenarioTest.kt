package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.BespokeBattlewagon
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Bespoke Battlewagon (MH3) — {T} for two energy, {T}+{E}{E} to tap a creature, {T}+{E}{E}{E} to
 * draw, {E}{E}{E}{E} to animate itself until end of turn, and crew 4.
 */
class BespokeBattlewagonScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + BespokeBattlewagon)
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })
    fun GameTestDriver.newlyArrived(id: EntityId) = replaceState(state.updateEntity(id) {
        it.with(SummoningSicknessComponent)
    })

    val abilities = BespokeBattlewagon.activatedAbilities
    val getEnergy = abilities[0].id
    val tapCreature = abilities[1].id
    val draw = abilities[2].id
    val animate = abilities[3].id

    test("{T}: get two energy, even the turn it arrives while not a creature") {
        val d = driver()
        val wagon = d.putPermanentOnBattlefield(d.player1, "Bespoke Battlewagon")
        d.newlyArrived(wagon)

        d.submit(ActivateAbility(d.player1, wagon, getEnergy)).error shouldBe null
        d.bothPass()
        d.energy() shouldBe 2
        d.isTapped(wagon) shouldBe true
    }

    test("{T}, pay {E}{E}: tap target creature") {
        val d = driver()
        val wagon = d.putPermanentOnBattlefield(d.player1, "Bespoke Battlewagon")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")

        d.giveEnergy(1)
        withClue("one energy can't pay {E}{E}") {
            d.submit(ActivateAbility(d.player1, wagon, tapCreature, targets = listOf(ChosenTarget.Permanent(theirs))))
                .error shouldNotBe null
        }

        d.giveEnergy(3)
        d.submit(ActivateAbility(d.player1, wagon, tapCreature, targets = listOf(ChosenTarget.Permanent(theirs))))
            .error shouldBe null
        d.bothPass()
        d.isTapped(theirs) shouldBe true
        d.isTapped(wagon) shouldBe true
        d.energy() shouldBe 1
    }

    test("{T}, pay {E}{E}{E}: draw a card") {
        val d = driver()
        val wagon = d.putPermanentOnBattlefield(d.player1, "Bespoke Battlewagon")
        d.giveEnergy(3)
        val handBefore = d.getHandSize(d.player1)

        d.submit(ActivateAbility(d.player1, wagon, draw)).error shouldBe null
        d.bothPass()
        d.getHandSize(d.player1) shouldBe handBefore + 1
        d.energy() shouldBe 0
    }

    test("pay {E}{E}{E}{E}: becomes a 5/6 artifact creature until end of turn, without tapping") {
        val d = driver()
        val wagon = d.putPermanentOnBattlefield(d.player1, "Bespoke Battlewagon")
        d.state.projectedState.isCreature(wagon) shouldBe false

        d.giveEnergy(3)
        withClue("three energy can't pay {E}{E}{E}{E}") {
            d.submit(ActivateAbility(d.player1, wagon, animate)).error shouldNotBe null
        }

        d.giveEnergy(4)
        d.submit(ActivateAbility(d.player1, wagon, animate)).error shouldBe null
        d.bothPass()
        d.energy() shouldBe 0
        d.isTapped(wagon) shouldBe false
        d.state.projectedState.isCreature(wagon) shouldBe true
        d.state.projectedState.getPower(wagon) shouldBe 5
        d.state.projectedState.getToughness(wagon) shouldBe 6

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(wagon), d.player2).error shouldBe null
        d.passPriorityUntil(Step.END)
        d.getLifeTotal(d.player2) shouldBe 15
    }

    test("animated, its {T} abilities are subject to summoning sickness") {
        val d = driver()
        val wagon = d.putPermanentOnBattlefield(d.player1, "Bespoke Battlewagon")
        d.newlyArrived(wagon)
        d.giveEnergy(4)
        d.submit(ActivateAbility(d.player1, wagon, animate)).error shouldBe null
        d.bothPass()
        d.state.projectedState.isCreature(wagon) shouldBe true

        d.submit(ActivateAbility(d.player1, wagon, getEnergy)).error shouldNotBe null
    }

    test("crew 4 needs total power four") {
        val d = driver()
        val wagon = d.putPermanentOnBattlefield(d.player1, "Bespoke Battlewagon")
        val bear1 = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val bear2 = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")

        d.submitExpectFailure(CrewVehicle(d.player1, wagon, listOf(bear1)))
        d.submitSuccess(CrewVehicle(d.player1, wagon, listOf(bear1, bear2)))
        d.bothPass()
        d.state.projectedState.isCreature(wagon) shouldBe true
        d.state.projectedState.getPower(wagon) shouldBe 5
    }
})
