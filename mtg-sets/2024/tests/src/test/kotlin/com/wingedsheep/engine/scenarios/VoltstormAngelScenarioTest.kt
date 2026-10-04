package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.VoltstormAngel
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Voltstorm Angel (MH3 #46) — {3}{W}{W} 4/4 Creature — Angel.
 *
 * "Flying
 *  When this creature enters, you get {E}{E}{E}.
 *  At the beginning of combat on your turn, you may pay {E}{E}. When you do, choose one —
 *  • This creature gains vigilance and lifelink until end of turn.
 *  • Other creatures you control get +1/+1 until end of turn."
 */
class VoltstormAngelScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(VoltstormAngel))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
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

    fun drainStack(d: GameTestDriver) {
        var guard = 0
        while (d.pendingDecision == null && d.state.stack.isNotEmpty() && guard++ < 10) d.bothPass()
    }

    /** Go to beginning of combat, pay {E}{E}, pick [mode], and resolve everything. */
    fun payAndChoose(d: GameTestDriver, player: EntityId, mode: Int) {
        d.passPriorityUntil(Step.BEGIN_COMBAT)
        drainStack(d)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(player, true).error shouldBe null
        val choice = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choice.options.size shouldBe 2
        d.submitDecision(player, OptionChosenResponse(choice.id, mode)).error shouldBe null
        drainStack(d)
    }

    test("casting it gives three energy") {
        val d = newDriver()
        val p1 = d.player1
        val card = d.putCardInHand(p1, "Voltstorm Angel")
        d.giveMana(p1, Color.WHITE, 5)
        d.castSpell(p1, card).error shouldBe null
        d.bothPass() // resolve the Angel
        drainStack(d) // resolve the ETB trigger
        d.findPermanent(p1, "Voltstorm Angel").shouldNotBeNull()
        energyOf(d, p1) shouldBe 3
    }

    test("mode 1 pays two energy and gives the Angel vigilance and lifelink") {
        val d = newDriver()
        val p1 = d.player1
        val angel = d.putCreatureOnBattlefield(p1, "Voltstorm Angel")
        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        seedEnergy(d, p1, 3)

        payAndChoose(d, p1, 0)

        energyOf(d, p1) shouldBe 1
        val projected = d.state.projectedState
        projected.hasKeyword(angel, Keyword.VIGILANCE) shouldBe true
        projected.hasKeyword(angel, Keyword.LIFELINK) shouldBe true
        projected.hasKeyword(bear, Keyword.LIFELINK) shouldBe false
        projected.getPower(bear) shouldBe 2
    }

    test("mode 2 gives other creatures you control +1/+1 but not the Angel or the opponent's") {
        val d = newDriver()
        val p1 = d.player1
        val p2 = d.getOpponent(p1)
        val angel = d.putCreatureOnBattlefield(p1, "Voltstorm Angel")
        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val oppBear = d.putCreatureOnBattlefield(p2, "Grizzly Bears")
        seedEnergy(d, p1, 2)

        payAndChoose(d, p1, 1)

        energyOf(d, p1) shouldBe 0
        val projected = d.state.projectedState
        projected.getPower(bear) shouldBe 3
        projected.getToughness(bear) shouldBe 3
        projected.getPower(angel) shouldBe 4
        projected.getPower(oppBear) shouldBe 2
        projected.hasKeyword(angel, Keyword.LIFELINK) shouldBe false
    }

    test("declining keeps the energy and does nothing") {
        val d = newDriver()
        val p1 = d.player1
        val angel = d.putCreatureOnBattlefield(p1, "Voltstorm Angel")
        val bear = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        seedEnergy(d, p1, 2)

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        drainStack(d)
        d.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        d.submitYesNo(p1, false).error shouldBe null
        drainStack(d)

        d.pendingDecision shouldBe null
        energyOf(d, p1) shouldBe 2
        d.state.projectedState.hasKeyword(angel, Keyword.LIFELINK) shouldBe false
        d.state.projectedState.getPower(bear) shouldBe 2
    }

    test("with fewer than two energy the payment is never offered") {
        val d = newDriver()
        val p1 = d.player1
        d.putCreatureOnBattlefield(p1, "Voltstorm Angel")
        seedEnergy(d, p1, 1)

        d.passPriorityUntil(Step.BEGIN_COMBAT)
        var guard = 0
        while (d.state.stack.isNotEmpty() && guard++ < 10) {
            (d.pendingDecision is YesNoDecision) shouldBe false
            d.bothPass()
        }
        d.pendingDecision shouldBe null
        energyOf(d, p1) shouldBe 1
    }
})
