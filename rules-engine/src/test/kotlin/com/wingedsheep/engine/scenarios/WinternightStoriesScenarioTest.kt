package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.WinternightStories
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Winternight Stories — {2}{U} Sorcery.
 *   "Draw three cards. Then discard two cards unless you discard a creature card."
 *
 * The "unless" is a player choice: discard one creature card, or discard two cards.
 */
class WinternightStoriesScenarioTest : FunSpec({

    fun createDriver(deck: Deck): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(WinternightStories)
        driver.initMirrorMatch(deck = deck, startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("choosing to discard a creature card discards only one card") {
        // Library is all Grizzly Bears (creature cards) so the three drawn cards are creatures.
        val driver = createDriver(Deck.of("Grizzly Bears" to 40))
        val player = driver.activePlayer!!

        val spell = driver.putCardInHand(player, "Winternight Stories")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveColorlessMana(player, 2)

        val handBefore = driver.state.getZone(ZoneKey(player, Zone.HAND)).size // includes the spell

        driver.submit(
            CastSpell(player, spell, paymentStrategy = PaymentStrategy.FromPool)
        ).isSuccess shouldBe true
        driver.bothPass()

        // After drawing three, a single discard decision: discard one creature card (the reduced
        // count), or two cards otherwise. minSelections is 1 because a creature satisfies it; the
        // creature cards are surfaced via conditionalMinimums.
        val discardDecision = driver.pendingDecision as? SelectCardsDecision
        discardDecision shouldNotBe null
        discardDecision!!.minSelections shouldBe 1
        discardDecision.maxSelections shouldBe 2
        val creatureCard = discardDecision.options.first { cardId ->
            driver.state.getEntity(cardId)?.get<CardComponent>()?.name == "Grizzly Bears"
        }
        driver.submitDecision(player, CardsSelectedResponse(discardDecision.id, listOf(creatureCard)))

        // Net: spell left hand (−1) + drew 3 (+3) + discarded 1 (−1) = +1 versus before-cast count.
        // handBefore counted the spell; the spell is now on the stack/graveyard so subtract it too.
        val handAfter = driver.state.getZone(ZoneKey(player, Zone.HAND)).size
        handAfter shouldBe (handBefore - 1) + 3 - 1
    }

    test("with no creature card in hand, you must discard two cards") {
        // All-land deck: neither the opening hand nor the three drawn cards are creatures.
        val driver = createDriver(Deck.of("Plains" to 40))
        val player = driver.activePlayer!!

        val spell = driver.putCardInHand(player, "Winternight Stories")
        driver.giveMana(player, Color.BLUE, 1)
        driver.giveColorlessMana(player, 2)

        driver.submit(
            CastSpell(player, spell, paymentStrategy = PaymentStrategy.FromPool)
        ).isSuccess shouldBe true
        driver.bothPass()

        // No creature in hand → the only feasible branch is "discard two cards" (auto-chosen).
        val discardDecision = driver.pendingDecision as? SelectCardsDecision
        discardDecision shouldNotBe null
        discardDecision!!.minSelections shouldBe 2
        discardDecision.maxSelections shouldBe 2
    }
})
