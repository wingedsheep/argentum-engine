package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.ChthonianNightmare
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ChthonianNightmareScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + ChthonianNightmare)
        it.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }
    fun GameTestDriver.energy(): Int =
        state.getEntity(player1)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0
    fun GameTestDriver.giveEnergy(n: Int) = replaceState(state.updateEntity(player1) {
        it.with(CountersComponent(mapOf(CounterType.ENERGY to n)))
    })
    val ability = ChthonianNightmare.activatedAbilities.single().id

    test("entering supplies three energy counters") {
        val d = driver()
        val card = d.putCardInHand(d.player1, "Chthonian Nightmare")
        d.giveMana(d.player1, Color.BLACK, 2)
        d.castSpell(d.player1, card).error shouldBe null
        d.bothPass()
        d.bothPass()
        d.energy() shouldBe 3
    }
    test("energy, sacrifice, and return are costs; reanimation survives the source leaving") {
        val d = driver()
        val nightmare = d.putPermanentOnBattlefield(d.player1, "Chthonian Nightmare")
        val fodder = d.putCreatureOnBattlefield(d.player1, "Llanowar Elves")
        val bear = d.putCardInGraveyard(d.player1, "Grizzly Bears")
        d.giveEnergy(3)
        d.submit(ActivateAbility(d.player1, nightmare, ability,
            targets = listOf(ChosenTarget.Card(bear, d.player1, Zone.GRAVEYARD)), xValue = 2,
            costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)))).error shouldBe null
        d.energy() shouldBe 1
        d.findCardInHand(d.player1, "Chthonian Nightmare") shouldNotBe null
        d.getGraveyardCardNames(d.player1).contains("Llanowar Elves") shouldBe true
        d.getGraveyardCardNames(d.player1).contains("Grizzly Bears") shouldBe true
        d.bothPass()
        d.findPermanent(d.player1, "Grizzly Bears") shouldNotBe null
    }
    test("target mana value must equal X and an unaffordable activation changes nothing") {
        for ((energy, x) in listOf(1 to 2, 3 to 1)) {
            val d = driver()
            val nightmare = d.putPermanentOnBattlefield(d.player1, "Chthonian Nightmare")
            val fodder = d.putCreatureOnBattlefield(d.player1, "Llanowar Elves")
            val bear = d.putCardInGraveyard(d.player1, "Grizzly Bears")
            d.giveEnergy(energy)
            val before = d.state
            d.submit(ActivateAbility(d.player1, nightmare, ability,
                targets = listOf(ChosenTarget.Card(bear, d.player1, Zone.GRAVEYARD)), xValue = x,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)))).error shouldNotBe null
            d.state shouldBe before
        }
    }
    test("cannot target the creature being sacrificed or activate without a target") {
        for (targetFodder in listOf(false, true)) {
            val d = driver()
            val nightmare = d.putPermanentOnBattlefield(d.player1, "Chthonian Nightmare")
            val fodder = d.putCreatureOnBattlefield(d.player1, "Llanowar Elves")
            d.giveEnergy(3)
            val before = d.state
            d.submit(ActivateAbility(d.player1, nightmare, ability,
                targets = if (targetFodder) listOf(ChosenTarget.Card(fodder, d.player1, Zone.GRAVEYARD)) else emptyList(),
                xValue = 1, costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder)))).error shouldNotBe null
            d.state shouldBe before
        }
    }
})
