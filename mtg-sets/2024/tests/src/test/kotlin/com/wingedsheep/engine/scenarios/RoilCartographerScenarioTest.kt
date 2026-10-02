package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.RoilCartographer
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Roil Cartographer (MH3) — "Landfall — Whenever a land you control enters, you get {E}.
 * / {T}, Pay six {E}: Draw three cards."
 */
class RoilCartographerScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + RoilCartographer)
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })
    val ability = RoilCartographer.activatedAbilities.single().id

    test("landfall: a land you control entering gets you one energy") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Roil Cartographer")
        val land = d.putCardInHand(d.player1, "Island")
        d.playLand(d.player1, land).error shouldBe null
        d.bothPass()
        d.energy() shouldBe 1
    }

    test("tap and pay six energy to draw three cards") {
        val d = driver()
        val cartographer = d.putCreatureOnBattlefield(d.player1, "Roil Cartographer")
        d.removeSummoningSickness(cartographer)
        d.giveEnergy(7)
        val handBefore = d.getHandSize(d.player1)
        d.submit(ActivateAbility(d.player1, cartographer, ability)).error shouldBe null
        d.energy() shouldBe 1
        d.isTapped(cartographer) shouldBe true
        d.bothPass()
        d.getHandSize(d.player1) shouldBe handBefore + 3
    }

    test("cannot activate with only five energy") {
        val d = driver()
        val cartographer = d.putCreatureOnBattlefield(d.player1, "Roil Cartographer")
        d.removeSummoningSickness(cartographer)
        d.giveEnergy(5)
        d.submit(ActivateAbility(d.player1, cartographer, ability)).error shouldNotBe null
        d.energy() shouldBe 5
        d.isTapped(cartographer) shouldBe false
    }

    test("summoning sickness prevents the tap ability") {
        val d = driver()
        val cartographer = d.putCreatureOnBattlefield(d.player1, "Roil Cartographer")
        d.giveEnergy(6)
        d.submit(ActivateAbility(d.player1, cartographer, ability)).error shouldNotBe null
        d.energy() shouldBe 6
    }
})
