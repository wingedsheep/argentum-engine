package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eoc.cards.ExplorationBroodship
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Exploration Broodship — 8+ | "Once during each of your turns, you may cast a permanent spell from
 * your graveyard by sacrificing a land in addition to paying its other costs."
 *
 * Pins: below 8 charge counters the graveyard permission is off; at 8 it casts a creature card from
 * the graveyard for its mana cost plus a sacrificed land; it is used up for the rest of the turn.
 */
class ExplorationBroodshipScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ExplorationBroodship))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.charge(id: EntityId, count: Int) {
        replaceState(state.updateEntity(id) { c ->
            c.with((c.get<CountersComponent>() ?: CountersComponent()).withAdded(CounterType.CHARGE, count))
        })
    }

    fun GameTestDriver.graveyardCasts(player: EntityId, cardId: EntityId) = legalActions(player)
        .filter { it.sourceZone == "GRAVEYARD" && (it.action as? CastSpell)?.cardId == cardId }

    fun GameTestDriver.giveBearMana(player: EntityId) {
        giveMana(player, Color.GREEN, 1)
        giveColorlessMana(player, 1)
    }

    test("below 8 charge counters no permanent card can be cast from the graveyard") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val ship = driver.putPermanentOnBattlefield(player, "Exploration Broodship")
        driver.charge(ship, 7)
        driver.putPermanentOnBattlefield(player, "Forest")
        val bear = driver.putCardInGraveyard(player, "Grizzly Bears")
        driver.giveBearMana(player)

        driver.graveyardCasts(player, bear).shouldBeEmpty()
    }

    test("at 8 charge counters a creature card is cast from the graveyard by sacrificing a land, once per turn") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val ship = driver.putPermanentOnBattlefield(player, "Exploration Broodship")
        driver.charge(ship, 8)
        val forest = driver.putPermanentOnBattlefield(player, "Forest")
        driver.putPermanentOnBattlefield(player, "Forest")
        val bear = driver.putCardInGraveyard(player, "Grizzly Bears")
        val secondBear = driver.putCardInGraveyard(player, "Grizzly Bears")
        driver.giveBearMana(player)

        val cast = driver.graveyardCasts(player, bear).single().action as CastSpell
        driver.submit(
            cast.copy(additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(forest)))
        ).outcome shouldBe Outcome.Done
        driver.state.getBattlefield().contains(forest) shouldBe false
        driver.bothPass()
        driver.state.getBattlefield().contains(bear) shouldBe true

        driver.giveBearMana(player)
        driver.graveyardCasts(player, secondBear).shouldBeEmpty()
    }

    test("the cast is refused without a land to sacrifice") {
        val driver = newDriver()
        val player = driver.activePlayer!!
        val ship = driver.putPermanentOnBattlefield(player, "Exploration Broodship")
        driver.charge(ship, 8)
        val bear = driver.putCardInGraveyard(player, "Grizzly Bears")
        driver.giveBearMana(player)

        driver.graveyardCasts(player, bear).shouldBeEmpty()
        driver.submitExpectFailure(CastSpell(player, bear))
        driver.getGraveyard(player).contains(bear) shouldBe true
    }
})
