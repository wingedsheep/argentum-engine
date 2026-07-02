package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.GarlandKnightOfCornelia
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.TimingRule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Garland, Knight of Cornelia // Chaos, the Endless (FIN):
 * "{3}{B}{B}{R}{R}: Return this card from your graveyard to the battlefield transformed.
 * Activate only as a sorcery."
 *
 * Exercises the new [com.wingedsheep.sdk.scripting.effects.ReturnSelfFromZoneTransformedEffect]:
 * a graveyard-activated ability whose resolution puts the card onto the battlefield with its
 * back face (Chaos) up. The card in the test goes to the graveyard *without ever being on the
 * battlefield* (as if discarded/milled), covering the path where no DoubleFacedComponent has
 * been stamped yet. Also closes the loop: Chaos dying puts the card (reverted to its front
 * face, Rule 712.8a) on the bottom of its owner's library.
 */
class GarlandKnightOfCorneliaScenarioTest : FunSpec({

    val projector = StateProjector()

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 40 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GarlandKnightOfCornelia))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun returnFromGraveyard(driver: GameTestDriver, active: com.wingedsheep.sdk.model.EntityId): com.wingedsheep.sdk.model.EntityId {
        val garland = driver.putCardInGraveyard(active, "Garland, Knight of Cornelia")
        driver.giveMana(active, Color.BLACK, 2)
        driver.giveMana(active, Color.RED, 2)
        driver.giveColorlessMana(active, 3)
        val abilityId = GarlandKnightOfCornelia.activatedAbilities.first().id
        driver.submit(ActivateAbility(playerId = active, sourceId = garland, abilityId = abilityId))
            .isSuccess shouldBe true
        driver.bothPass()
        resolveStack(driver)
        return garland
    }

    test("the graveyard ability is sorcery-speed and activates from the graveyard") {
        val ability = GarlandKnightOfCornelia.activatedAbilities.first()
        ability.timing shouldBe TimingRule.SorcerySpeed
        ability.activateFromZone shouldBe Zone.GRAVEYARD
    }

    test("activating from the graveyard returns the card to the battlefield as Chaos, the Endless") {
        val driver = newDriver()
        val active = driver.activePlayer!!

        val garland = returnFromGraveyard(driver, active)

        // Same entity, now on the battlefield with the back face up: Chaos, a 5/5 flying Demon.
        driver.findPermanent(active, "Chaos, the Endless") shouldBe garland
        driver.state.getZone(active, Zone.GRAVEYARD).isEmpty() shouldBe true
        val projected = projector.project(driver.state)
        projected.getPower(garland) shouldBe 5
        projected.getToughness(garland) shouldBe 5
        projected.hasKeyword(garland, Keyword.FLYING) shouldBe true
    }

    test("when Chaos dies it goes to the bottom of its owner's library as Garland (front face)") {
        val driver = newDriver()
        val active = driver.activePlayer!!

        val garland = returnFromGraveyard(driver, active)
        driver.findPermanent(active, "Chaos, the Endless") shouldBe garland

        // Kill the 5/5 with two of our own 3-damage bolts in the same turn; the death trigger
        // puts the card on the bottom of its owner's library instead of leaving it in the
        // graveyard.
        repeat(2) {
            val bolt = driver.putCardInHand(active, "Lightning Bolt")
            driver.giveMana(active, Color.RED, 1)
            driver.castSpell(active, bolt, targets = listOf(garland)).isSuccess shouldBe true
            driver.bothPass()
            resolveStack(driver)
        }

        driver.findPermanent(active, "Chaos, the Endless") shouldBe null
        driver.state.getZone(active, Zone.GRAVEYARD).contains(garland) shouldBe false
        val library = driver.state.getZone(active, Zone.LIBRARY)
        library.last() shouldBe garland
        // Off the battlefield the card reverts to its front face (Rule 712.8a).
        driver.state.getEntity(garland)!!.get<CardComponent>()!!.name shouldBe
            "Garland, Knight of Cornelia"
    }

})
