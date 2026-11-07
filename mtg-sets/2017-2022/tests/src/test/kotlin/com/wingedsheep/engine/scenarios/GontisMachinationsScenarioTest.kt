package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.aer.cards.GontisMachinations
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Gonti's Machinations (AER) — only the first life loss each turn gives {E}; paying {E}{E} and
 * sacrificing it drains each opponent for 3, gaining the life actually lost.
 */
class GontisMachinationsScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + GontisMachinations)
        it.initMirrorMatch(Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })
    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 10) bothPass()
    }
    fun GameTestDriver.boltYourself() {
        val bolt = putCardInHand(player1, "Lightning Bolt")
        giveMana(player1, Color.RED, 1)
        castSpell(player1, bolt, listOf(player1)).error shouldBe null
        resolveStack()
    }
    val drain = GontisMachinations.activatedAbilities.single().id

    test("only the first life loss of the turn gives energy") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Gonti's Machinations")

        d.boltYourself()
        withClue("first loss this turn: {E}") { d.energy() shouldBe 1 }
        d.getLifeTotal(d.player1) shouldBe 17

        d.boltYourself()
        withClue("second loss the same turn doesn't trigger") { d.energy() shouldBe 1 }
        d.getLifeTotal(d.player1) shouldBe 14
    }

    test("pay {E}{E} and sacrifice: each opponent loses 3 and you gain the life lost") {
        val d = driver()
        val machinations = d.putPermanentOnBattlefield(d.player1, "Gonti's Machinations")
        d.giveEnergy(2)

        d.submit(ActivateAbility(d.player1, machinations, drain)).error shouldBe null
        withClue("energy and the enchantment are paid on activation") {
            d.energy() shouldBe 0
            d.findPermanent(d.player1, "Gonti's Machinations") shouldBe null
        }
        d.resolveStack()

        d.getLifeTotal(d.player2) shouldBe 17
        d.getLifeTotal(d.player1) shouldBe 23
    }

    test("the drain can't be activated with only one energy") {
        val d = driver()
        val machinations = d.putPermanentOnBattlefield(d.player1, "Gonti's Machinations")
        d.giveEnergy(1)

        d.submit(ActivateAbility(d.player1, machinations, drain)).error shouldNotBe null
        d.energy() shouldBe 1
        d.findPermanent(d.player1, "Gonti's Machinations") shouldBe machinations
    }
})
