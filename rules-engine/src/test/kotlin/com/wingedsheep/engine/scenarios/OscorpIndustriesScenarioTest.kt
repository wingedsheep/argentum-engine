package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Oscorp Industries (SPM) — Mayhem land (CR 702.187c no-cost form). Pins the new Mayhem
 * land-play-from-graveyard path (`PlayLandEnumerator` + `PlayLandHandler`) and the
 * "enters from a graveyard" ETB (the `EnteredFromGraveyardComponent` the handler now stamps on
 * graveyard land-plays).
 */
class OscorpIndustriesScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    fun markDiscarded(driver: GameTestDriver, playerId: EntityId, cardId: EntityId) {
        val prior = driver.state.getEntity(playerId)
            ?.get<CardsDiscardedThisTurnComponent>() ?: CardsDiscardedThisTurnComponent()
        driver.addComponent(playerId, prior.copy(cardIds = prior.cardIds + cardId, count = prior.count + 1))
    }

    fun playLandActions(driver: GameTestDriver, playerId: EntityId) =
        LegalActionEnumerator.create(driver.cardRegistry).enumerate(driver.state, playerId)
            .mapNotNull { it.action as? PlayLand }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    test("a discarded-this-turn Oscorp can be played from the graveyard via Mayhem; it enters tapped and you lose 2 life") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val oscorp = driver.putCardInGraveyard(player, "Oscorp Industries")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // In the graveyard but NOT discarded this turn: no Mayhem land-play offered.
        playLandActions(driver, player).none { it.cardId == oscorp }.shouldBeTrue()

        // Discarded this turn: the Mayhem land-play appears.
        markDiscarded(driver, player, oscorp)
        playLandActions(driver, player).map { it.cardId } shouldContain oscorp

        // Play it from the graveyard.
        val result = driver.submit(PlayLand(player, oscorp))
        result.error shouldBe null
        resolveStack(driver) // resolve the enters-from-graveyard trigger

        // It's on the battlefield, tapped, and you lost 2 life (entered from a graveyard).
        driver.state.getBattlefield().contains(oscorp) shouldBe true
        (driver.state.getEntity(oscorp)?.has<TappedComponent>() == true) shouldBe true
        driver.getLifeTotal(player) shouldBe 18
    }

    test("a land played from hand does not trigger the enters-from-graveyard life loss") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val oscorp = driver.putCardInHand(player, "Oscorp Industries")
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.submit(PlayLand(player, oscorp)).error shouldBe null
        resolveStack(driver)
        driver.state.getBattlefield().contains(oscorp) shouldBe true
        // Entered from hand — the ConditionalEffect's graveyard check is false, so no life loss.
        driver.getLifeTotal(player) shouldBe 20
    }
})
