package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.GollumPatientPlotter
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gollum, Patient Plotter — "{B}, Sacrifice a creature: Return this card from your graveyard to
 * your hand. Activate only as a sorcery." Verifies the graveyard-functional activated ability
 * (Gap 11 — already engine-landed via activateFromZone = Zone.GRAVEYARD).
 */
class GollumPatientPlotterScenarioTest : FunSpec({

    val graveyardAbilityId = GollumPatientPlotter.activatedAbilities.first().id

    test("returns itself from graveyard to hand by sacrificing a creature at sorcery speed") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val gollum = driver.putCardInGraveyard(activePlayer, "Gollum, Patient Plotter")
        val fodder = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.giveMana(activePlayer, Color.BLACK, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gollum,
                abilityId = graveyardAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
            )
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // Gollum returns to hand; the sacrificed creature is gone; Gollum left the graveyard.
        driver.getHand(activePlayer).contains(gollum) shouldBe true
        driver.state.getGraveyard(activePlayer).contains(gollum) shouldBe false
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
    }

    test("cannot activate at instant speed (sorcery-speed restriction)") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 20), startingLife = 20)

        val activePlayer = driver.activePlayer!!
        // The active player's own upkeep is not a sorcery-speed window (not a main phase).
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)

        val gollum = driver.putCardInGraveyard(activePlayer, "Gollum, Patient Plotter")
        val fodder = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        driver.giveMana(activePlayer, Color.BLACK, 1)

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = gollum,
                abilityId = graveyardAbilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(fodder))
            )
        )
        result.isSuccess shouldBe false
    }
})
