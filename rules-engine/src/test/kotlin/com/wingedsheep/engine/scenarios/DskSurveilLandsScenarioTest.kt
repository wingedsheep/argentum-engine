package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.BleedingWoods
import com.wingedsheep.mtg.sets.definitions.dsk.cards.EtchedCornfield
import com.wingedsheep.mtg.sets.definitions.dsk.cards.MurkySewer
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the three Duskmourn: House of Horror common dual lands:
 * Bleeding Woods (R/G), Etched Cornfield (G/W), Murky Sewer (U/B).
 *
 * Each: "This land enters tapped unless a player has 13 or less life. {T}: Add {C1} or {C2}."
 * The enters-tapped condition is the existential [APlayerLifeAtMost(13)] — true when ANY
 * player is at 13 life or below, distinct from a controller-only threshold.
 */
class DskSurveilLandsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BleedingWoods + EtchedCornfield + MurkySewer)
        return driver
    }

    test("Bleeding Woods enters tapped while both players are at full life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val woods = driver.putCardInHand(p1, "Bleeding Woods")
        driver.playLand(p1, woods).isSuccess shouldBe true

        driver.state.getEntity(woods)?.has<TappedComponent>() shouldBe true
    }

    test("Bleeding Woods enters untapped when a player has 13 or less life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.setLifeTotal(driver.getOpponent(p1), 13)
        val woods = driver.putCardInHand(p1, "Bleeding Woods")
        driver.playLand(p1, woods).isSuccess shouldBe true

        driver.state.getEntity(woods)?.has<TappedComponent>() shouldBe false
    }

    test("Bleeding Woods taps for red and green") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val red = driver.putPermanentOnBattlefield(p1, "Bleeding Woods")
        driver.submit(ActivateAbility(p1, red, BleedingWoods.activatedAbilities[0].id)).isSuccess shouldBe true
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.red shouldBe 1

        val green = driver.putPermanentOnBattlefield(p1, "Bleeding Woods")
        driver.submit(ActivateAbility(p1, green, BleedingWoods.activatedAbilities[1].id)).isSuccess shouldBe true
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.green shouldBe 1
    }

    test("Etched Cornfield enters tapped at full life and taps for green and white") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val field = driver.putCardInHand(p1, "Etched Cornfield")
        driver.playLand(p1, field).isSuccess shouldBe true
        driver.state.getEntity(field)?.has<TappedComponent>() shouldBe true

        val green = driver.putPermanentOnBattlefield(p1, "Etched Cornfield")
        driver.submit(ActivateAbility(p1, green, EtchedCornfield.activatedAbilities[0].id)).isSuccess shouldBe true
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.green shouldBe 1

        val white = driver.putPermanentOnBattlefield(p1, "Etched Cornfield")
        driver.submit(ActivateAbility(p1, white, EtchedCornfield.activatedAbilities[1].id)).isSuccess shouldBe true
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.white shouldBe 1
    }

    test("Murky Sewer enters untapped when controller is low on life and taps for blue and black") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.setLifeTotal(p1, 10)
        val sewer = driver.putCardInHand(p1, "Murky Sewer")
        driver.playLand(p1, sewer).isSuccess shouldBe true
        driver.state.getEntity(sewer)?.has<TappedComponent>() shouldBe false

        val blue = driver.putPermanentOnBattlefield(p1, "Murky Sewer")
        driver.submit(ActivateAbility(p1, blue, MurkySewer.activatedAbilities[0].id)).isSuccess shouldBe true
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.blue shouldBe 1

        val black = driver.putPermanentOnBattlefield(p1, "Murky Sewer")
        driver.submit(ActivateAbility(p1, black, MurkySewer.activatedAbilities[1].id)).isSuccess shouldBe true
        driver.state.getEntity(p1)?.get<ManaPoolComponent>()?.black shouldBe 1
    }
})
