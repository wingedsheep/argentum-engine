package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards.LluwenImperfectNaturalist
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Regression tests for Lluwen, Imperfect Naturalist's ETB:
 * "When Lluwen enters, mill four cards, then you may put a creature or land
 *  card from among the milled cards on top of your library."
 */
class LluwenImperfectNaturalistTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LluwenImperfectNaturalist))
        return driver
    }

    test("casting Lluwen mills four cards and pauses for the put-back decision") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 30, "Swamp" to 20, "Grizzly Bears" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val lluwen = driver.putCardInHand(activePlayer, "Lluwen, Imperfect Naturalist")
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveMana(activePlayer, Color.GREEN, 1)

        val graveyardBefore = driver.state.getZone(ZoneKey(activePlayer, Zone.GRAVEYARD)).size

        driver.castSpell(activePlayer, lluwen).isSuccess shouldBe true
        driver.bothPass() // resolve Lluwen spell -> ETB trigger goes on stack
        driver.bothPass() // resolve ETB trigger -> mill 4, pause for put-back decision

        val graveyardAfter = driver.state.getZone(ZoneKey(activePlayer, Zone.GRAVEYARD))
        (graveyardAfter.size - graveyardBefore) shouldBe 4
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
    }

    test("casting a second Lluwen while one is already in play still triggers ETB (legend rule)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 30, "Swamp" to 20, "Grizzly Bears" to 10),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Lluwen, Imperfect Naturalist")
        val secondLluwen = driver.putCardInHand(activePlayer, "Lluwen, Imperfect Naturalist")
        driver.giveMana(activePlayer, Color.BLACK, 1)
        driver.giveMana(activePlayer, Color.GREEN, 1)

        val graveyardBefore = driver.state.getZone(ZoneKey(activePlayer, Zone.GRAVEYARD)).size

        driver.castSpell(activePlayer, secondLluwen).isSuccess shouldBe true
        driver.bothPass() // second Lluwen resolves; legend rule SBA pauses for choice

        // First stop: legend rule. Keep the just-cast Lluwen (older one goes to graveyard).
        driver.isPaused shouldBe true
        val legendDecision = driver.pendingDecision as SelectCardsDecision
        legendDecision.prompt shouldBe "Choose which Lluwen, Imperfect Naturalist to keep (legend rule)"
        driver.submitCardSelection(activePlayer, listOf(secondLluwen))

        // ETB trigger — deferred under the legend-rule continuation — now fires and pauses
        // for the put-back decision. No further priority passes needed.
        val graveyardAfter = driver.state.getZone(ZoneKey(activePlayer, Zone.GRAVEYARD))
        val lluwensOnBattlefield = driver.state.getZone(ZoneKey(activePlayer, Zone.BATTLEFIELD))
            .count { driver.state.getEntity(it)?.get<CardComponent>()?.name == "Lluwen, Imperfect Naturalist" }

        // 1 Lluwen sent to graveyard by legend rule + 4 milled = 5.
        (graveyardAfter.size - graveyardBefore) shouldBe 5
        lluwensOnBattlefield shouldBe 1
        driver.isPaused shouldBe true
        val putBackDecision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        putBackDecision.prompt shouldBe "You may put a creature or land card on top of your library"
    }
})
