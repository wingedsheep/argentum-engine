package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.soi.cards.EpiphanyAtTheDrownyard
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Epiphany at the Drownyard: reveal the top X plus one cards, *you* separate them into two piles,
 * an *opponent* chooses the pile that goes to your hand; the other goes to your graveyard.
 *
 * Pins the parts the script can't show on its own: the reveal count follows the cast-time X (X+1),
 * the caster makes the split while the opponent makes the pile choice, and X = 0 reveals one card
 * with an empty pile the opponent may hand you (2016-04-08 ruling).
 */
class EpiphanyAtTheDrownyardScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(EpiphanyAtTheDrownyard)
        driver.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("X=2 reveals three cards; caster splits, opponent picks the pile for the caster's hand") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val c1 = driver.putCardOnTopOfLibrary(active, "Island")
        val c2 = driver.putCardOnTopOfLibrary(active, "Forest")
        val c3 = driver.putCardOnTopOfLibrary(active, "Mountain")
        val c4 = driver.putCardOnTopOfLibrary(active, "Plains")
        // Library from top: c4, c3, c2, c1 — X+1 = 3 reveals c4, c3, c2; c1 stays.

        val spell = driver.putCardInHand(active, "Epiphany at the Drownyard")
        driver.giveMana(active, Color.BLUE, 3)
        val handBefore = driver.getHandSize(active)

        driver.castXSpell(active, spell, xValue = 2).outcome shouldBe Outcome.Done
        driver.bothPass()

        // 1. The caster separates the three revealed cards.
        val split = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        split.playerId shouldBe active
        split.options.toSet() shouldBe setOf(c4, c3, c2)
        driver.submitDecision(active, CardsSelectedResponse(split.id, listOf(c4)))

        // 2. The opponent chooses which pile goes to the caster's hand: Pile 2 (c3, c2).
        val choose = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choose.playerId shouldBe opponent
        val pile2 = choose.optionCardIds!!.entries.first { it.value.toSet() == setOf(c3, c2) }.key
        driver.submitDecision(opponent, OptionChosenResponse(choose.id, pile2))

        driver.isPaused shouldBe false
        driver.state.getZone(ZoneKey(active, Zone.HAND)) shouldContainAll listOf(c3, c2)
        driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD)) shouldContainAll listOf(c4, spell)
        driver.getHandSize(active) shouldBe handBefore - 1 + 2
        driver.state.getZone(ZoneKey(active, Zone.LIBRARY)).first() shouldBe c1
    }

    test("X=0 reveals one card; the opponent may hand the caster the empty pile") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)

        val c1 = driver.putCardOnTopOfLibrary(active, "Island")
        val c2 = driver.putCardOnTopOfLibrary(active, "Forest")

        val spell = driver.putCardInHand(active, "Epiphany at the Drownyard")
        driver.giveMana(active, Color.BLUE, 1)
        val handBefore = driver.getHandSize(active)

        driver.castXSpell(active, spell, xValue = 0).outcome shouldBe Outcome.Done
        driver.bothPass()

        val split = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        split.playerId shouldBe active
        split.options shouldBe listOf(c2)
        driver.submitDecision(active, CardsSelectedResponse(split.id, listOf(c2)))

        val choose = driver.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        choose.playerId shouldBe opponent
        val emptyPile = choose.optionCardIds!!.entries.first { it.value.isEmpty() }.key
        driver.submitDecision(opponent, OptionChosenResponse(choose.id, emptyPile))

        driver.isPaused shouldBe false
        driver.getHandSize(active) shouldBe handBefore - 1
        driver.state.getZone(ZoneKey(active, Zone.GRAVEYARD)) shouldContainAll listOf(c2, spell)
        driver.state.getZone(ZoneKey(active, Zone.LIBRARY)).first() shouldBe c1
    }
})
