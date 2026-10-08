package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.AbandonedCampground
import com.wingedsheep.mtg.sets.definitions.dsk.cards.StrangledCemetery
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome

/**
 * Tests for two Duskmourn: House of Horror common dual lands:
 * Abandoned Campground (W/U) and Strangled Cemetery (B/G).
 *
 * Each: "This land enters tapped unless a player has 13 or less life. {T}: Add {C1} or {C2}."
 * The enters-tapped condition is the existential `Conditions.APlayerLifeAtMost(13)` — true when ANY
 * player is at 13 life or below, distinct from a controller-only threshold.
 */
class DskHorrorLandsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + AbandonedCampground + StrangledCemetery)
        return driver
    }

    test("Abandoned Campground enters tapped while both players are at full life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val land = driver.putCardInHand(p1, "Abandoned Campground")
        driver.playLand(p1, land).outcome shouldBe Outcome.Done

        driver.state.getEntity(land)?.has<TappedComponent>() shouldBe true
    }

    test("Abandoned Campground enters untapped when an opponent has 13 or less life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.setLifeTotal(driver.getOpponent(p1), 13)
        val land = driver.putCardInHand(p1, "Abandoned Campground")
        driver.playLand(p1, land).outcome shouldBe Outcome.Done

        driver.state.getEntity(land)?.has<TappedComponent>() shouldBe false
    }

    test("Abandoned Campground taps for white and blue") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val white = driver.putPermanentOnBattlefield(p1, "Abandoned Campground")
        driver.submit(ActivateAbility(p1, white, AbandonedCampground.activatedAbilities[0].id)).outcome shouldBe Outcome.Done
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.white shouldBe 1

        val blue = driver.putPermanentOnBattlefield(p1, "Abandoned Campground")
        driver.submit(ActivateAbility(p1, blue, AbandonedCampground.activatedAbilities[1].id)).outcome shouldBe Outcome.Done
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.blue shouldBe 1
    }

    test("Strangled Cemetery enters tapped at full life and taps for black and green") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val land = driver.putCardInHand(p1, "Strangled Cemetery")
        driver.playLand(p1, land).outcome shouldBe Outcome.Done
        driver.state.getEntity(land)?.has<TappedComponent>() shouldBe true

        val black = driver.putPermanentOnBattlefield(p1, "Strangled Cemetery")
        driver.submit(ActivateAbility(p1, black, StrangledCemetery.activatedAbilities[0].id)).outcome shouldBe Outcome.Done
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.black shouldBe 1

        val green = driver.putPermanentOnBattlefield(p1, "Strangled Cemetery")
        driver.submit(ActivateAbility(p1, green, StrangledCemetery.activatedAbilities[1].id)).outcome shouldBe Outcome.Done
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.green shouldBe 1
    }

    test("Strangled Cemetery enters untapped when controller is low on life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.setLifeTotal(p1, 10)
        val land = driver.putCardInHand(p1, "Strangled Cemetery")
        driver.playLand(p1, land).outcome shouldBe Outcome.Done
        driver.state.getEntity(land)?.has<TappedComponent>() shouldBe false
    }
})
