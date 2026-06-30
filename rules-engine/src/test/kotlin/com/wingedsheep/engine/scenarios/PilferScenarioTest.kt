package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dmu.cards.Pilfer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Pilfer {1}{B} Sorcery (DMU canonical; reprinted in FDN).
 *
 * Target opponent reveals their hand. You choose a nonland card from it.
 * That player discards that card.
 */
class PilferScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Pilfer))
        return driver
    }

    test("you choose a nonland card from the opponent's hand and they discard it") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent has a nonland card (chooseable) and a land (not chooseable).
        val nonland = driver.putCardInHand(opponent, "Grizzly Bears")
        driver.putCardInHand(opponent, "Plains")
        val gyBefore = driver.getGraveyard(opponent).size

        val pilfer = driver.putCardInHand(you, "Pilfer")
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, pilfer, targets = listOf(opponent)).isSuccess shouldBe true
        driver.bothPass() // resolve into the spell's effect

        // Drain decisions: choose the nonland card to discard.
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.state.pendingDecision is SelectCardsDecision) && guard++ < 30) {
            if (driver.state.pendingDecision is SelectCardsDecision) {
                driver.submitCardSelection(you, listOf(nonland))
            } else {
                driver.bothPass()
            }
        }

        // The chosen nonland card was discarded to the opponent's graveyard.
        driver.getGraveyard(opponent).contains(nonland) shouldBe true
        (driver.getGraveyard(opponent).size - gyBefore) shouldBe 1
    }

    test("only nonland cards are offered as choices") {
        val driver = createDriver()
        // Plains-only deck: every opening-hand card is a land, so the only nonland in the
        // opponent's hand is the one we add — the choice must offer it and exclude the lands.
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val nonland = driver.putCardInHand(opponent, "Grizzly Bears")
        val land = driver.putCardInHand(opponent, "Plains")

        val pilfer = driver.putCardInHand(you, "Pilfer")
        driver.giveMana(you, Color.BLACK, 1)
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, pilfer, targets = listOf(opponent)).isSuccess shouldBe true
        driver.bothPass()

        val decision = driver.state.pendingDecision as? SelectCardsDecision
            ?: error("Expected a SelectCardsDecision to choose the card to discard")
        decision.options.contains(nonland) shouldBe true
        decision.options.contains(land) shouldBe false
    }
})
