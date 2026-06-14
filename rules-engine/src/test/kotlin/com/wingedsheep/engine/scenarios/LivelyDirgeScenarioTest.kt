package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.LivelyDirge
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Lively Dirge — {1}{B} Sorcery, Spree
 *
 * + {1} — Search your library for a card, put it into your graveyard, then shuffle.
 * + {2} — Return up to two creature cards with total mana value 4 or less from your
 *         graveyard to the battlefield.
 */
class LivelyDirgeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(LivelyDirge)
        return driver
    }

    test("mode + {1}: search library for a card, put it into the graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        // A creature in the library to tutor up.
        driver.putCardOnTopOfLibrary(me, "Centaur Courser")

        val spell = driver.putCardInHand(me, "Lively Dirge")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 2) // {1}{B} base + {1} for the chosen mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(emptyList()),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass() // resolve -> pause for the search selection

        driver.isPaused shouldBe true
        val select = driver.pendingDecision
        select.shouldBeInstanceOf<SelectCardsDecision>()
        val courser = (select as SelectCardsDecision).options.first()

        driver.submitDecision(
            me,
            CardsSelectedResponse(decisionId = select.id, selectedCards = listOf(courser))
        )
        driver.isPaused shouldBe false

        driver.getGraveyard(me).contains(courser) shouldBe true
    }

    test("mode + {2}: return two creatures with total mana value 4 or less from graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        // Centaur Courser (MV 3) + Savannah Lions (MV 1) = total MV 4, exactly the cap.
        val courser = driver.putCardInGraveyard(me, "Centaur Courser") // {2}{G} = MV 3
        val lions = driver.putCardInGraveyard(me, "Savannah Lions")    // {W}    = MV 1

        val spell = driver.putCardInHand(me, "Lively Dirge")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 3) // {1}{B} base + {2} for the chosen mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(emptyList()),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass() // resolve -> pause for the up-to-two selection

        driver.isPaused shouldBe true
        val select = driver.pendingDecision
        select.shouldBeInstanceOf<SelectCardsDecision>()

        driver.submitDecision(
            me,
            CardsSelectedResponse(
                decisionId = (select as SelectCardsDecision).id,
                selectedCards = listOf(courser, lions)
            )
        )
        driver.isPaused shouldBe false

        // Both creatures return to the battlefield.
        driver.findPermanent(me, "Centaur Courser") shouldBe courser
        driver.findPermanent(me, "Savannah Lions") shouldBe lions
        driver.getGraveyard(me).contains(courser) shouldBe false
        driver.getGraveyard(me).contains(lions) shouldBe false
    }

    test("both modes: search a creature into the graveyard, then reanimate it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        driver.putCardOnTopOfLibrary(me, "Centaur Courser") // {2}{G} = MV 3 creature to tutor + reanimate

        val spell = driver.putCardInHand(me, "Lively Dirge")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 4) // {1}{B} base + {1} + {2}
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = listOf(0, 1),
                modeTargetsOrdered = listOf(emptyList(), emptyList()),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass() // resolve -> pause for the search selection

        driver.isPaused shouldBe true
        val searchSelect = driver.pendingDecision
        searchSelect.shouldBeInstanceOf<SelectCardsDecision>()
        val found = (searchSelect as SelectCardsDecision).options.first()
        driver.submitDecision(
            me,
            CardsSelectedResponse(decisionId = searchSelect.id, selectedCards = listOf(found))
        )

        // Mode 1 resolved → the creature is now in the graveyard, and mode 2 prompts the reanimation.
        driver.isPaused shouldBe true
        val reanimateSelect = driver.pendingDecision
        reanimateSelect.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitDecision(
            me,
            CardsSelectedResponse(
                decisionId = (reanimateSelect as SelectCardsDecision).id,
                selectedCards = listOf(found)
            )
        )
        driver.isPaused shouldBe false

        // The tutored creature is back on the battlefield.
        driver.findPermanent(me, "Centaur Courser") shouldBe found
    }

    test("Spree requires at least one mode: casting with no modes fails") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val spell = driver.putCardInHand(me, "Lively Dirge")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 1)
        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                chosenModes = emptyList(),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe false
    }
})
