package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.KellanDaringTraveler
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kellan, Daring Traveler // Journey On (LCI).
 *
 * Kellan: "Whenever Kellan attacks, reveal the top card of your library. If it's a creature card
 * with mana value 3 or less, put it into your hand. Otherwise, you may put it into your graveyard."
 *
 * Journey On (Adventure — Sorcery): "Create X Map tokens, where X is one plus the number of
 * opponents who control an artifact."
 */
class KellanDaringTravelerScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens)
        driver.registerCard(KellanDaringTraveler)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun mapTokens(driver: GameTestDriver, player: EntityId): List<EntityId> =
        driver.getPermanents(player).filter { id ->
            driver.state.getEntity(id)?.get<CardComponent>()?.name == "Map"
        }

    // --- Kellan attack trigger ---------------------------------------------------------------

    test("attacking with a creature (MV <= 3) on top puts it into hand, no decision") {
        val driver = newDriver()
        val kellan = driver.putCreatureOnBattlefield(driver.player1, "Kellan, Daring Traveler")
        driver.removeSummoningSickness(kellan)
        driver.putCardOnTopOfLibrary(driver.player1, "Centaur Courser") // creature, MV 3 (boundary)

        val handBefore = driver.getHandSize(driver.player1)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(kellan), driver.player2)
        driver.bothPass()

        driver.isPaused shouldBe false // no "may" decision on the creature branch
        driver.getHandSize(driver.player1) shouldBe handBefore + 1
        driver.getGraveyardCardNames(driver.player1) shouldNotContain "Centaur Courser"
    }

    test("attacking with a creature of MV >= 4 on top offers the may-mill; accepting sends it to graveyard") {
        val driver = newDriver()
        val kellan = driver.putCreatureOnBattlefield(driver.player1, "Kellan, Daring Traveler")
        driver.removeSummoningSickness(kellan)
        driver.putCardOnTopOfLibrary(driver.player1, "Force of Nature") // creature, MV 5

        val handBefore = driver.getHandSize(driver.player1)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(kellan), driver.player2)
        driver.bothPass()

        driver.isPaused shouldBe true // "you may put it into your graveyard"
        driver.submitYesNo(driver.player1, true)

        driver.getHandSize(driver.player1) shouldBe handBefore // not a creature MV<=3, no draw
        driver.getGraveyardCardNames(driver.player1) shouldContain "Force of Nature"
    }

    test("attacking with a noncreature on top offers the may-mill; declining leaves it on top of library") {
        val driver = newDriver()
        val kellan = driver.putCreatureOnBattlefield(driver.player1, "Kellan, Daring Traveler")
        driver.removeSummoningSickness(kellan)
        driver.putCardOnTopOfLibrary(driver.player1, "Lightning Bolt") // noncreature

        val handBefore = driver.getHandSize(driver.player1)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(kellan), driver.player2)
        driver.bothPass()

        driver.isPaused shouldBe true
        driver.submitYesNo(driver.player1, false) // decline

        driver.getHandSize(driver.player1) shouldBe handBefore
        driver.getGraveyardCardNames(driver.player1) shouldNotContain "Lightning Bolt"
    }

    // --- Journey On (Adventure) --------------------------------------------------------------

    test("Journey On with no opponent artifact creates 1 Map, exiles the card, and lets Kellan be cast from exile") {
        val driver = newDriver()
        val kellan = driver.putCardInHand(driver.player1, "Kellan, Daring Traveler")
        driver.giveMana(driver.player1, Color.GREEN, 1)

        driver.submit(
            CastSpell(
                playerId = driver.player1,
                cardId = kellan,
                faceIndex = 0, // Journey On
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        mapTokens(driver, driver.player1).size shouldBe 1 // 1 + 0 opponents with an artifact

        // Card exiled by the Adventure (CR 715), with permission to cast the creature later.
        driver.getExile(driver.player1) shouldContain kellan
        driver.state.mayPlayPermissions.any { kellan in it.cardIds && it.controllerId == driver.player1 } shouldBe true
    }

    test("Journey On counts each opponent controlling an artifact: X = 1 + 1 = 2 Maps") {
        val driver = newDriver()
        driver.putCreatureOnBattlefield(driver.player2, "Frogmite") // artifact creature under opponent's control
        val kellan = driver.putCardInHand(driver.player1, "Kellan, Daring Traveler")
        driver.giveMana(driver.player1, Color.GREEN, 1)

        driver.submit(
            CastSpell(
                playerId = driver.player1,
                cardId = kellan,
                faceIndex = 0,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        mapTokens(driver, driver.player1).size shouldBe 2
    }
})
